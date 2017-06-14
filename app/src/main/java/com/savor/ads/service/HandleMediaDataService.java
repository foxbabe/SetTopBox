package com.savor.ads.service;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.bean.BoxInitBean;
import com.savor.ads.bean.BoxInitResult;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.OnDemandBean;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.bean.PlayListCategoryItem;
import com.savor.ads.bean.PrizeInfo;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.bean.SetBoxTopResult;
import com.savor.ads.bean.SetTopBoxBean;
import com.savor.ads.bean.TvProgramResponse;
import com.savor.ads.bean.VersionInfo;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.log.LogReportUtil;
import com.savor.ads.log.LotteryLogUtil;
import com.savor.ads.okhttp.coreProgress.download.ProgressDownloader;
import com.savor.ads.oss.OSSValues;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.TechnicalLogReporter;
import com.savor.ads.utils.UpdateUtil;
import com.savor.ads.utils.tv.TvOperate;
import com.tvos.common.TvManager;
import com.tvos.common.exception.TvCommonException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 处理下载媒体文件逻辑的服务
 * Created by bichao on 2016/12/10.
 */

public class HandleMediaDataService extends Service implements ApiRequestListener {

    private Context context;
    private Session session;
    private String logo_md5 = null;
    private String loading_img_md5 = null;
    //接口返回的广告数据
    private SetTopBoxBean setTopBoxBean;
    //接口返回的点播数据
    private SetTopBoxBean mulitcasrtBoxBean;
    /**接口返回的盒子信息*/
    private BoxInitBean boxInitBean;

    private DBHelper dbHelper;
    private DownloadManager downloadManager = null;
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    /**
     * 每次第一次开机运行的时候都检测一遍本地文件文件，如果损毁就重新下载
     */
    private boolean isFirstRun = true;
    /**
     * 1.启动的时候写电视机播放音量和切换时间日志
     * 2.当音量和时间发生改变的时候写日志
     */
    private boolean isProduceLog = false;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        this.session = Session.get(this);
        dbHelper = DBHelper.get(context);
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("==========into onStartCommand method=========");
        LogFileUtil.write("HandleMediaDataService onStartCommand");
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {

                    // 循环检查SD卡、网络、小平台信息的情况直到可用
                    do {
                        LogFileUtil.write("HandleMediaDataService will check server info and network");
                        if (!TextUtils.isEmpty(AppUtils.getExternalSDCardPath()) &&
                                AppUtils.isNetworkAvailable(context) &&
                                session.getServerInfo() != null) {
                            break;
                        }

                        try {
                            Thread.sleep(1000 * 2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (true);

                    LogFileUtil.write("HandleMediaDataService will start UpdateUtil");
                    // 异步更新apk、rom
                    new UpdateUtil(context);

                    getPrizeInfo();

                    LogFileUtil.write("HandleMediaDataService will start getBoxInfo");
                    // 同步获取机顶盒基本信息，包括logo、loading图
                    getBoxInfo();

                    // 检测预约发布的播放时间是否已到达，启动时不检测因为已经在Application中检测过了
                    if (!isFirstRun && AppUtils.checkPlayTime(context)) {
                        notifyToPlay();
                    }

                    LogFileUtil.write("HandleMediaDataService will start getAdvertDataFromSmallPlatform");
                    // 同步获取轮播媒体数据
                    getAdvertDataFromSmallPlatform();

                    LogFileUtil.write("HandleMediaDataService will start getOnDemandDataFromSmallPlatform");
                    // 同步获取点播媒体数据
                    getOnDemandDataFromSmallPlatform();
//                    setAutoClose(true);

                    LogFileUtil.write("HandleMediaDataService will start getTVMatchDataFromSmallPlatform");
                    // 异步获取电视节目信息
                    getTVMatchDataFromSmallPlatform();

                    // 睡眠10分钟
                    try {
                        Thread.sleep(1000 * 60 * 10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }


    private void getPrizeInfo() {
        LogFileUtil.write("will start getPrizeInfo");
        LogUtils.d("will start getPrizeInfo");
        AppApi.getPrize(this, new ApiRequestListener() {
            @Override
            public void onSuccess(AppApi.Action method, Object obj) {
                if (obj instanceof PrizeInfo) {
                    LogUtils.d("Got new prize info, will update local prize config!");
                    LogFileUtil.write("Got new prize info, will update local prize config!");
                    PrizeInfo newPrize = (PrizeInfo) obj;
                    if (session.getPrizeInfo() == null || !session.getPrizeInfo().getDate_time().equals(newPrize.getDate_time())) {
                        session.setPrizeInfo(newPrize);
                        LotteryLogUtil.getInstance(context).writeLotteryUpdate();
                    }
                }
            }

            @Override
            public void onError(AppApi.Action method, Object obj) {
                LogUtils.d("Got non prize info, will clear local prize config!");
                LogFileUtil.write("Got non prize info, will clear local prize config!");
                session.setPrizeInfo(null);
            }

            @Override
            public void onNetworkFailed(AppApi.Action method) {
                LogUtils.d("Got prize info timeout!");
                LogFileUtil.write("Got prize info timeout!");
            }
        });
    }

    /**
     * 盒子下载之前删除旧的媒体文件（删除视频的操作移到AdsPlayer中进行）
     */
    private void deleteOldMedia() {

        LogUtils.d("删除多余视频");

        // PlayListVersion为空说明没有一个完整的播放列表（初装的时候），这时不做删除操作，以免删掉了手动拷入的视频
        if (session.getPlayListVersion() == null || session.getPlayListVersion().isEmpty()) {
            return;
        }

        //排除当前已经完整下载的文件和正在下载的文件，其他删除
        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media);
        File[] listFiles = new File(path).listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return;
        }
        try {
//            if (dbHelper.findPlayListByWhere(null, null)==null
//                    &&dbHelper.findNewPlayListByWhere(null, null)==null){
//                return;
//            }
            for (File file : listFiles) {
                if (file.isFile()) {
                    String selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?";
                    String[] selectionArgs = new String[]{file.getName()};

                    if (dbHelper.findPlayListByWhere(selection, selectionArgs) == null
                            && dbHelper.findNewPlayListByWhere(selection, selectionArgs) == null) {
                        file.delete();
                        LogUtils.d("删除文件===================" + file.getName());
                    }
                } else {
                    FileUtils.deleteFile(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void getBoxInfo() {
        try {
            String configJson = AppApi.getBoxInitInfo(this, this, session.getEthernetMac());
            JSONObject jsonObject = new JSONObject(configJson);
            if (jsonObject.getInt("code") != AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                LogUtils.d("接口返回的状态不对,code=" + jsonObject.getInt("code"));
                return;
            }

            Object result = gson.fromJson(configJson, new TypeToken<BoxInitResult>() {
            }.getType());
            if (result instanceof BoxInitResult) {
                BoxInitBean boxInitBean = ((BoxInitResult) result).getResult();
                /*******************设置盒子基本信息开始************************/
                initBoxInfo(boxInitBean);
                /*******************设置盒子基本信息结束************************/
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {

        }
    }

    /**
     * 获取小平台广告媒体文件
     */
    private void getAdvertDataFromSmallPlatform() {
        try {
            String configJson = AppApi.getAdvertDataFromSmallPlatform(this, this, session.getEthernetMac());

            Object result = gson.fromJson(configJson, new TypeToken<SetBoxTopResult>() {
            }.getType());
            if (result instanceof SetBoxTopResult) {
                SetBoxTopResult setBoxTopResult = (SetBoxTopResult) result;
                if (setBoxTopResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                    if (setBoxTopResult.getResult() != null) {
                        setTopBoxBean = setBoxTopResult.getResult();
                        handleSmallPlatformAdvertData();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getOnDemandDataFromSmallPlatform() {
        try {
            String ondemandJson = AppApi.getOnDemandDataFromSmallPlatform(this, this, session.getEthernetMac());
            Object result = gson.fromJson(ondemandJson, new TypeToken<SetBoxTopResult>() {
            }.getType());
            if (result instanceof SetBoxTopResult) {
                SetBoxTopResult setBoxTopResult = (SetBoxTopResult) result;
                if (setBoxTopResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS && setBoxTopResult.getResult() != null) {
                    mulitcasrtBoxBean = setBoxTopResult.getResult();
                    handleSmallPlatformOnDemandData();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getTVMatchDataFromSmallPlatform() {
        AppApi.getTVMatchDataFromSmallPlatform(this, this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSuccess(AppApi.Action method, Object obj) {
        switch (method) {
            case SP_GET_TV_MATCH_DATA_FROM_JSON:
                if (obj instanceof TvProgramResponse) {
                    TvProgramResponse response = (TvProgramResponse) obj;
                    TvOperate mtv = new TvOperate();
                    mtv.updateProgram(context, response);
                }
                break;
            case SP_GET_LOGO_DOWN:
                if (obj instanceof File) {
                    File f = (File) obj;
                    byte[] fRead = new byte[0];
                    String md5Value = null;
                    try {
                        fRead = org.apache.commons.io.FileUtils.readFileToByteArray(f);
                        md5Value = AppUtils.getMD5(fRead);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //比较本地文件版本是否与服务器文件一致，如果一致则启动安装
                    if (md5Value != null && md5Value.equals(logo_md5)) {
                        try {
                            File file = new File(Environment.getExternalStorageDirectory(), session.getSplashPath());
                            if (file.exists()) {
                                file.delete();
                            }
                            String newPath = "/Pictures/" + f.getName();
                            FileUtils.copyFile(f.getAbsolutePath(), Environment.getExternalStorageDirectory().getAbsolutePath() + newPath);
                            session.setSplashPath(newPath);
                            if (boxInitBean.getLogo_version_list() != null && !boxInitBean.getLogo_version_list().isEmpty()) {
                                session.setSplashVersion(boxInitBean.getLogo_version_list().get(0).getVersion());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case SP_GET_LOADING_IMG_DOWN:
                if (obj instanceof File) {
                    File f = (File) obj;
                    byte[] fRead = new byte[0];
                    String md5Value = null;
                    try {
                        fRead = org.apache.commons.io.FileUtils.readFileToByteArray(f);
                        md5Value = AppUtils.getMD5(fRead);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //比较本地文件版本是否与服务器文件一致，如果一致则启动安装
                    if (md5Value != null && md5Value.equals(loading_img_md5)) {
                        try {
                            File file = new File(Environment.getExternalStorageDirectory(), session.getLoadingPath());
                            if (file.exists()) {
                                file.delete();
                            }
                            String newPath = "/Pictures/" + f.getName();
                            FileUtils.copyFile(f.getAbsolutePath(), Environment.getExternalStorageDirectory().getAbsolutePath() + newPath);
                            session.setLoadingPath(newPath);
                            if (boxInitBean.getLoading_version_list() != null && !boxInitBean.getLoading_version_list().isEmpty()) {
                                session.setLoadingVersion(boxInitBean.getLoading_version_list().get(0).getVersion());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
        }
    }

    //机顶盒监听电视屏,控制是否当电视屏没有通电的时候关机,false不关机,true关机
    private void setAutoClose(boolean flag) {
        try {
            TvManager.setGpioDeviceStatus(128, flag);
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理小平台返回的广告数据
     */
    private void handleSmallPlatformAdvertData() {
        if (setTopBoxBean == null || setTopBoxBean.getPlaybill_list() == null || setTopBoxBean.getPlaybill_list().isEmpty()) {
            return;
        }

        //如果期数相同，则表示数据没有改变，不需要执行后续的下载动作（第一次循环即便期号相同，也做一次遍历作为文件校验）
        LogUtils.d("===============AdvertMediaPeriod===========" + session.getAdvertMediaPeriod());
        if (!isFirstRun && (isVersionListMatchNewer(session.getPlayListVersion(), setTopBoxBean) ||
                isVersionListMatchNewer(session.getNextPlayListVersion(), setTopBoxBean))) {
            return;
        }

        ArrayList<PlayListCategoryItem> playbill_list = setTopBoxBean.getPlaybill_list();
        boolean isAllCompleted = true;  //是否所有类型都下载完毕

        // 从列表中抽出版本信息
        ArrayList<VersionInfo> newVersions = new ArrayList<>();
        StringBuilder versionSequence = new StringBuilder();
        for (int i = 0; i < playbill_list.size(); i++) {
            PlayListCategoryItem item = playbill_list.get(i);
            newVersions.add(item.getVersion());
            if (i != 0) {
                versionSequence.append("_");
            }
            versionSequence.append(item.getVersion().getVersion());
        }
        String versionStr = versionSequence.toString();

        // 设置下载中期号
        session.setDownloadingPlayListVersion(newVersions);

        for (int i = 0; i < playbill_list.size(); i++) {
            PlayListCategoryItem item = playbill_list.get(i);
            VersionInfo versionInfo = item.getVersion();
            if (versionInfo == null || TextUtils.isEmpty(versionInfo.getType())) {
                continue;
            }

//            // 找出该类型的当前期号
//            String periodExist = AppUtils.findSpecifiedPeriodByType(session.getPlayListVersion(), versionInfo.getType());

            // 先从下载表中删除该期的记录
            String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "=? and " + DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?";
            String[] selectionArgs = new String[]{versionInfo.getVersion(), versionInfo.getType()};
            dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);

            List<MediaLibBean> mediaLibList = item.getMedia_lib();
            ArrayList<PlayListBean> mDownloadedList = new ArrayList<>();
            if (mediaLibList != null && mediaLibList.size() > 0) {

                ServerInfo serverInfo = session.getServerInfo();
                if (serverInfo == null)
                    break;

                String baseUrl = serverInfo.getDownloadUrl();
                if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }

                LogUtils.d("---------轮播视频开始下载---------");
                for (MediaLibBean mediaItem : mediaLibList) {
                    String url = baseUrl + mediaItem.getUrl();
                    String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + mediaItem.getName();
                    try {
                        // 下载、校验
                        boolean isChecked = false;
                        if (isDownloadCompleted(path, mediaItem.getMd5())) {
                            isChecked = true;
                        } else {
                            boolean isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                            if (isDownloaded && isDownloadCompleted(path, mediaItem.getMd5())) {
                                isChecked = true;
                            }
                        }

                        // 校验通过、插库
                        if (isChecked) {
                            PlayListBean bean = new PlayListBean();
                            bean.setPeriod(mediaItem.getPeriod());
                            bean.setDuration(mediaItem.getDuration());
                            bean.setMd5(mediaItem.getMd5());
                            bean.setVid(mediaItem.getVid());
                            bean.setMedia_name(mediaItem.getName());
                            bean.setMedia_type(mediaItem.getType());
                            bean.setOrder(mediaItem.getOrder());
                            bean.setSurfix(mediaItem.getSurfix());
                            bean.setMediaPath(path);
                            int id = -1;

                            // 插库成功，mDownloadedList中加入一条
                            if (dbHelper.insertOrUpdatePlayListLib(bean, id)) {
                                mDownloadedList.add(bean);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // 下载完成（mediaLibList==null或size为0也认为是下载完成，认为新的节目单中没有该类型的数据）
            if (mediaLibList == null ||  mDownloadedList.size() == mediaLibList.size()) {
                LogUtils.d("---------子分类轮播视频下载完成---------");

                // 记录日志
                LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                        Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "update", versionInfo.getType(), "",
                        "", Session.get(context).getVersionName(), versionInfo.getVersion(),
                        Session.get(context).getVodPeriod(), "");
            } else {
                isAllCompleted = false;
            }
        }

        if (isAllCompleted) {
            isFirstRun = false;

            // 标识是立即播放还是预约发布
            boolean fillCurrentBill = true;
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date pubDate = format.parse(setTopBoxBean.getPub_time());
                fillCurrentBill = pubDate.getTime() <= System.currentTimeMillis();
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (fillCurrentBill) {
                session.setPlayListVersion(newVersions);
                session.setAdvertMediaPeriod(versionStr);
            } else {
                session.setNextPlayListVersion(newVersions);
                session.setNextAdvertMediaPubTime(setTopBoxBean.getPub_time());
            }

            // 删除下载表中的非当前期、非预发布期的内容
            String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? and "
                    + DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "!=? ";
            String[] selectionArgs;
            if (session.getPlayListVersion() != null) {
                for (VersionInfo versionInfo : session.getPlayListVersion()) {
                    selectionArgs = new String[]{versionInfo.getVersion(), versionInfo.getType()};
                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);
                }
            }
            if (session.getNextPlayListVersion() != null) {
                for (VersionInfo versionInfo : session.getNextPlayListVersion()) {
                    selectionArgs = new String[]{versionInfo.getVersion(), versionInfo.getType()};
                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);
                }
            }

            // 将下载表中的内容拷贝到播放表
            dbHelper.copyPlaylist(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, DBHelper.MediaDBInfo.TableName.PLAYLIST);

            // 如果是填充当前期（立即播放）的话，通知ADSPlayer播放
            if (fillCurrentBill) {
                notifyToPlay();
            }
        }
    }

    private boolean isVersionListMatchNewer(ArrayList<VersionInfo> versionList, SetTopBoxBean bean) {
        boolean match = false;
        if (versionList != null && bean != null && bean.getPlaybill_list() != null) {
            if (bean.getPlaybill_list().size() == versionList.size()) {
                for (PlayListCategoryItem playListCategory : bean.getPlaybill_list()) {
                    VersionInfo versionInfo = playListCategory.getVersion();
                    if (versionInfo != null && !TextUtils.isEmpty(versionInfo.getType()) &&
                            !AppUtils.findSpecifiedPeriodByType(versionList, versionInfo.getType()).equals(versionInfo.getVersion())) {
                        return false;
                    }
                }
                match = true;
            }
        }
        return match;
    }

    private void notifyToPlay() {
        if(fillPlayList()) {
            LogUtils.d("发送广告下载完成广播");
            sendBroadcast(new Intent(ConstantValues.ADS_DOWNLOAD_COMPLETE_ACCTION));
        }
    }


    private boolean fillPlayList() {
        LogUtils.d("开始fillPlayList");
        ArrayList<PlayListBean> playList = dbHelper.getOrderedPlayList();

        if (playList != null && !playList.isEmpty()) {
            for (int i = 0; i < playList.size(); i++) {
                PlayListBean bean = playList.get(i);
                File mediaFile = new File(bean.getMediaPath());
                if (TextUtils.isEmpty(bean.getMd5()) ||
                        TextUtils.isEmpty(bean.getMediaPath()) ||
                        !mediaFile.exists() ||
                        !bean.getMd5().equals(AppUtils.getMD5Method(mediaFile))) {
                    LogUtils.e("媒体文件校验失败! vid:" + bean.getVid());
                    // 媒体文件校验失败时删除
                    playList.remove(i);

                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,
                            DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                            new String[]{bean.getPeriod(), bean.getVid(), bean.getMedia_type()});
                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.PLAYLIST,
                            DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                            new String[]{bean.getPeriod(), bean.getVid(), bean.getMedia_type()});

                    if (mediaFile.exists()) {
                        mediaFile.delete();
                    }

                    TechnicalLogReporter.md5Failed(this, bean.getVid());
                }

            }
        }

        if (playList != null && !playList.isEmpty()) {
            GlobalValues.PLAY_LIST = playList;
            return true;
        } else {
            LogFileUtil.writeApInfo("出现异常！下载完毕一整期但是数据库没查到对应期的数据！期号：" +
                    session.getAdsPeriod() + "_" + session.getAdvPeriod() + "_" + session.getProPeriod());
            return false;
        }
    }

    /**
     * 初始化盒子基本信息
     *
     * @param boiteBean
     */
    void initBoxInfo(BoxInitBean boiteBean) {
        if (boiteBean == null) {
            return;
        }
        boxInitBean = boiteBean;

        // 应后端统计要求，只要某个音量改变，就产生4条音量的记录
        if (!isProduceLog || boiteBean.getAds_volume() != session.getVolume() ||
                boiteBean.getProject_volume() != session.getProjectVolume() ||
                boiteBean.getDemand_volume() != session.getVodVolume() ||
                boiteBean.getTv_volume() != session.getTvVolume()) {
            String volumeUUID = String.valueOf(System.currentTimeMillis());
            //生产电视播放音量日志
            LogReportUtil.get(context).sendAdsLog(volumeUUID,
                    session.getBoiteId(),
                    session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()),
                    "player_volume",
                    "system",
                    "",
                    "",
                    session.getVersionName(),
                    session.getAdsPeriod(),
                    session.getVodPeriod(),
                    String.valueOf(boiteBean.getAds_volume()));
            LogReportUtil.get(context).sendAdsLog(volumeUUID,
                    session.getBoiteId(),
                    session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()),
                    "project_volume",
                    "system",
                    "",
                    "",
                    session.getVersionName(),
                    session.getAdsPeriod(),
                    session.getVodPeriod(),
                    String.valueOf(boiteBean.getProject_volume()));
            LogReportUtil.get(context).sendAdsLog(volumeUUID,
                    session.getBoiteId(),
                    session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()),
                    "vod_volume",
                    "system",
                    "",
                    "",
                    session.getVersionName(),
                    session.getAdsPeriod(),
                    session.getVodPeriod(),
                    String.valueOf(boiteBean.getDemand_volume()));
            LogReportUtil.get(context).sendAdsLog(volumeUUID,
                    session.getBoiteId(),
                    session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()),
                    "tv_volume",
                    "system",
                    "",
                    "",
                    session.getVersionName(),
                    session.getAdsPeriod(),
                    session.getVodPeriod(),
                    String.valueOf(boiteBean.getTv_volume()));

            if (boiteBean.getAds_volume() > 0) {
                session.setVolume(boiteBean.getAds_volume());
            } else {
                session.setVolume(ConstantValues.DEFAULT_ADS_VOLUME);
            }
            if (boiteBean.getProject_volume() > 0) {
                session.setProjectVolume(boiteBean.getProject_volume());
            } else {
                session.setProjectVolume(ConstantValues.DEFAULT_PROJECT_VOLUME);
            }
            if (boiteBean.getDemand_volume() > 0) {
                session.setVodVolume(boiteBean.getDemand_volume());
            } else {
                session.setVodVolume(ConstantValues.DEFAULT_VOD_VOLUME);
            }
            if (boiteBean.getTv_volume() > 0) {
                session.setTvVolume(boiteBean.getTv_volume());
            } else {
                session.setTvVolume(ConstantValues.DEFAULT_TV_VOLUME);
            }
        }

        if (!isProduceLog || boiteBean.getSwitch_time() != session.getSwitchTime()) {
            //生产电视切换时间日志
            LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                    session.getBoiteId(),
                    session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()),
                    "swich_time",
                    "system",
                    "",
                    "",
                    session.getVersionName(),
                    session.getAdsPeriod(),
                    session.getVodPeriod(),
                    String.valueOf(boiteBean.getSwitch_time()));

            if (boiteBean.getSwitch_time() > 0) {
                session.setSwitchTime(boiteBean.getSwitch_time());
            } else {
                session.setSwitchTime(ConstantValues.DEFAULT_SWITCH_TIME);
            }
        }

        isProduceLog = true;
//        if (!TextUtils.isEmpty(session.getBoiteId()) &&
//                !session.getBoiteId().equals(boiteBean.getHotel_id())){
//            deleteDataByChangeBoite();
//        }
        session.setBoxId(boiteBean.getBox_id());
        session.setBoiteId(boiteBean.getHotel_id());
        session.setBoiteName(boiteBean.getHotel_name());

        session.setRoomId(boiteBean.getRoom_id());
        session.setRoomName(boiteBean.getRoom_name());
        session.setRoomType(boiteBean.getRoom_type());
        session.setBoxName(boiteBean.getBox_name());
//        /**桶名称*/
//        if (!TextUtils.isEmpty(boiteBean.getOssBucketName())) {
//            session.setOss_bucket(boiteBean.getOssBucketName());
//        }
        /**桶地址*/
        if (!TextUtils.isEmpty(boiteBean.getAreaId())) {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            String datestr = format.format(new Date());

            session.setOss_file_path(OSSValues.uploadFilePath + boiteBean.getAreaId() + File.separator + datestr + File.separator);
        }

        // 组合小平台下发的各种版本信息
        ArrayList<VersionInfo> spVersionInfo = new ArrayList<>();
        if (boiteBean.getPlaybill_version_list() != null && !boiteBean.getPlaybill_version_list().isEmpty()) {
            spVersionInfo.addAll(boiteBean.getPlaybill_version_list());
        }
        if (boiteBean.getDemand_version_list() != null && !boiteBean.getDemand_version_list().isEmpty()) {
            spVersionInfo.addAll(boiteBean.getDemand_version_list());
        }
//        if (boiteBean.getLogo_version_list() != null && !boiteBean.getLogo_version_list().isEmpty()) {
//            spVersionInfo.addAll(boiteBean.getLogo_version_list());
//        }
//        if (boiteBean.getLoading_version_list() != null && !boiteBean.getLoading_version_list().isEmpty()) {
//            spVersionInfo.addAll(boiteBean.getLoading_version_list());
//        }
        if (boiteBean.getApk_version_list() != null && !boiteBean.getApk_version_list().isEmpty()) {
            spVersionInfo.addAll(boiteBean.getApk_version_list());
        }
        if (boiteBean.getSmall_web_version_list() != null && !boiteBean.getSmall_web_version_list().isEmpty()) {
            spVersionInfo.addAll(boiteBean.getSmall_web_version_list());
        }
        session.setSPVersionInfo(spVersionInfo);

        /**下载启动图*/
        if (!TextUtils.isEmpty(boiteBean.getLogo_url()) && !TextUtils.isEmpty(boiteBean.getLogo_md5()) &&
                boxInitBean.getLogo_version_list() != null && !boxInitBean.getLogo_version_list().isEmpty()) {
            File logoFile = new File(Environment.getExternalStorageDirectory(), session.getSplashPath());
            String md5 = null;
            try {
                if (logoFile.exists()) {
                    md5 = AppUtils.getMD5(org.apache.commons.io.FileUtils.readFileToByteArray(logoFile));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!boiteBean.getLogo_md5().equals(md5)) {
                ServerInfo serverInfo = session.getServerInfo();
                if (serverInfo != null) {
                    String baseUrl = serverInfo.getDownloadUrl();
                    String url = baseUrl + boiteBean.getLogo_url();
                    if (!TextUtils.isEmpty(boiteBean.getLogo_url())) {
                        String[] split = boiteBean.getLogo_url().split("/");
                        String logo_name = split[split.length - 1];
                        logo_md5 = boiteBean.getLogo_md5();
                        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.cache) + logo_name;
                        File tarFile = new File(path);
                        if (tarFile.exists()) {
                            tarFile.delete();
                        }
                        AppApi.downloadLOGO(url, context, this, path);
                    }
                }
            } else {
                // 做容错，当md5比对一致时设置一次期号
                session.setSplashVersion(boxInitBean.getLogo_version_list().get(0).getVersion());
            }
        }
        /**下载视频投屏加载图*/
        if (!TextUtils.isEmpty(boiteBean.getLoading_img_url()) && !TextUtils.isEmpty(boiteBean.getLoading_img_md5()) &&
                boxInitBean.getLoading_version_list() != null && !boxInitBean.getLoading_version_list().isEmpty()) {
            File loadingFile = new File(Environment.getExternalStorageDirectory(), session.getLoadingPath());
            String md5 = null;
            try {
                if (loadingFile.exists()) {
                    md5 = AppUtils.getMD5(org.apache.commons.io.FileUtils.readFileToByteArray(loadingFile));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!boiteBean.getLoading_img_md5().equals(md5)) {
                ServerInfo serverInfo = session.getServerInfo();
                if (serverInfo != null) {
                    String baseUrl = serverInfo.getDownloadUrl();
                    String url = baseUrl + boiteBean.getLoading_img_url();
                    if (!TextUtils.isEmpty(boiteBean.getLoading_img_url())) {
                        String[] split = boiteBean.getLoading_img_url().split("/");
                        String imageName = split[split.length - 1];
                        loading_img_md5 = boiteBean.getLoading_img_md5();
                        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.cache) + imageName;
                        File tarFile = new File(path);
                        if (tarFile.exists()) {
                            tarFile.delete();
                        }
                        AppApi.downloadLoadingImg(url, context, this, path);
                    }
                }
            } else {
                // 做容错，当md5比对一致时设置一次期号
                session.setLoadingVersion(boxInitBean.getLoading_version_list().get(0).getVersion());
            }
        }
    }

//    /**
//     * 如果发现酒楼ID发生改变，那么证明盒子换酒楼了，需要把视频和数据清空重新下载
//     */
//    private void deleteDataByChangeBoite() {
//        //排除当前已经完整下载的文件夹和正在下载的文件夹，其他删除
//        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media);
//        File[] listFiles = new File(path).listFiles();
//        if (listFiles == null || listFiles.length == 0) {
//            return;
//        }
//        try {
//            List<PlayListBean> list = null;
//            for (File file : listFiles) {
//                String selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME
//                        + "=? and "
//                        + DBHelper.MediaDBInfo.FieldName.PERIOD
//                        + "=? ";
//                String[] selectionArgs = null;
//                if (!TextUtils.isEmpty(session.getAdvertMediaPeriod())) {
//                    selectionArgs = new String[]{file.getName(), session.getAdvertMediaPeriod()};
//                    list = dbHelper.findPlayListByWhere(selection, selectionArgs);
//                    if (list != null && list.size() > 0) {
//                        FileUtils.deleteFile(file);
//                        LogUtils.d("删除文件===================" + file.getName());
//                    }
//                    continue;
//                } else if (!TextUtils.isEmpty(session.getAdvertDownloadingPeriod())) {
//                    selectionArgs = new String[]{file.getName(), session.getAdvertDownloadingPeriod()};
//                    list = dbHelper.findNewPlayListByWhere(selection, selectionArgs);
//                    if (list != null && list.size() > 0) {
//                        FileUtils.deleteFile(file);
//                        LogUtils.d("删除文件===================" + file.getName());
//                    }
//                    continue;
//                } else if (!TextUtils.isEmpty(session.getNextAdvertMediaPeriod())) {
//                    selectionArgs = new String[]{file.getName(), session.getNextAdvertMediaPeriod()};
//                    list = dbHelper.findPlayListByWhere(selection, selectionArgs);
//                    if (list != null && list.size() > 0) {
//                        FileUtils.deleteFile(file);
//                        LogUtils.d("删除文件===================" + file.getName());
//                    }
//                    continue;
//                }
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        session.setAdvertMediaPeriod(null);
//        session.setAdvertDownloadingPeriod(null);
//        session.setMulticastMediaPeriod(null);
//        session.setMulticastDownloadingPeriod(null);
//        dbHelper.deleteAllData(DBHelper.MediaDBInfo.TableName.PLAYLIST);
//    }

    /**
     * 创建广告下载任务
     *
     * @param mediaLib
     * @param period
     */
//    private void createDownloadTask(MediaLibBean mediaLib, String period, String path) {
//
//        //将下载任务添加到下载表
//        String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "=? and "
//                + DBHelper.MediaDBInfo.FieldName.VID + "=?";
//        String[] selectionArgs = new String[]{mediaLib.getPeriod(), mediaLib.getVid()};
//        List<MediaLibBean> list = dbHelper.findMediaLib(selection, selectionArgs);
//        if (list != null && list.size() > 0) {
//            //如果下载任务已存在，既不建立下载任务，只插入下载表数据
//            mediaLib.setTaskId(list.get(0).getTaskId());
//        } else {
//            //建立下载任务
//            File f = new File(path);
//            long taskId = getTaskId("url", f);
//            mediaLib.setTaskId(String.valueOf(taskId));
//        }
//        dbHelper.insertMediaLib(mediaLib);
//
//    }

    /**
     * 获取下载的taskId
     *
     * @param url
     * @param f
     * @return
     */
//    private long getTaskId(String url, File f) {
//        //创建下载任务,downloadUrl就是下载链接
//        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("url"));
//        //在通知栏中显示，默认就是显示的
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
//        request.setVisibleInDownloadsUi(false);
//        request.setDestinationUri(Uri.fromFile(f));
//        //将下载任务加入下载队列，否则不会进行下载
//        long taskId = downloadManager.enqueue(request);
//        return taskId;
//    }

    /**
     * 监听广告下载
     *
     * @param period
     */
    boolean advertThreadRunning = false;

//    private void monitorDownloadThread(final String period) {
//        if (!advertThreadRunning) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    advertThreadRunning = true;
//                    while (true) {
//                        String selection = "";
//                        String[] selectionArgs = new String[]{};
//                        List<MediaLibBean> list = dbHelper.findMediaLib(selection, selectionArgs);
//                        if (list == null || list.size() == 0) {
//                            session.setAdvertMediaPeriod(period);
//                            break;
//                        }
//                        if (list != null && list.size() > 0) {
//                            for (MediaLibBean lib : list) {
//                                boolean isCompleted = queryDownloadStatus(lib.getTaskId());
//                                try {
//                                    if (isCompleted) {
//                                        String mVid = lib.getVid();
//                                        String surfix = lib.getSurfix();
//                                        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media)
//                                                + mVid + "."
//                                                + surfix;
//                                        if (isDownloadCompleted(path, lib.getMd5())) {
//                                            selection = "";
//                                            selectionArgs = new String[]{lib.getTaskId()};
//                                            List<MediaLibBean> sameTaskIdList = dbHelper.findMediaLib(selection, selectionArgs);
//                                            boolean flag = dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MEDIALIB, selection, selectionArgs);
//                                            if (flag) {
//                                                for (MediaLibBean bean : sameTaskIdList) {
//                                                    PlayListBean play = new PlayListBean();
//                                                    play.setPeriod(bean.getPeriod());
//                                                    play.setDuration(bean.getDuration());
//                                                    play.setMd5(bean.getMd5());
//                                                    play.setVid(bean.getVid());
//                                                    play.setMedia_name(bean.getName());
//                                                    play.setOrder(bean.getOrder());
//                                                    play.setSurfix(bean.getSurfix());
//                                                    play.setMediaPath(path);
////                                                    dbHelper.insertPlayListLib(play);
//                                                }
//
//                                            }
//                                        }
//                                    }
//                                } catch (Exception e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                        try {
//                            Thread.sleep(60 * 1000 * 2);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    advertThreadRunning = false;
//                }
//            }).start();
//        }
//
//
//    }

    /**
     * 查询下载状态
     *
     * @param taskId
     * @return
     */
//    private boolean queryDownloadStatus(String taskId) {
//        if (TextUtils.isEmpty(taskId)) {
//            return false;
//        }
//        DownloadManager.Query query = new DownloadManager.Query();
//        query.setFilterById(Long.valueOf(taskId));
//        Cursor cursor = downloadManager.query(query);
//        if (cursor != null && cursor.moveToFirst()) {
//            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//            if (status == DownloadManager.STATUS_SUCCESSFUL) {
//                return true;
//            }
//
//        }
//        return false;
//    }

    /**
     * 文件是否下载完成判定
     *
     * @param path
     * @param md5
     * @return
     * @throws IOException
     */
    private boolean isDownloadCompleted(String path, String md5) throws Exception {
        if (AppUtils.isFileExist(path)) {
            String realMd5 = AppUtils.getMD5Method(new File(path));
            if (!TextUtils.isEmpty(md5) && md5.equals(realMd5)) {
                return true;
            }
            return false;
        } else {
            return false;
        }
    }


    /**
     * 处理小平台返回的点播视频
     */
    private void handleSmallPlatformOnDemandData() {
        if (mulitcasrtBoxBean == null || mulitcasrtBoxBean.getPlaybill_list() == null || mulitcasrtBoxBean.getPlaybill_list().isEmpty()) {
            return;
        }
        ServerInfo serverInfo = session.getServerInfo();
        if (serverInfo == null) {
            return;
        }


        String baseUrl = serverInfo.getDownloadUrl();
        if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        boolean isAllCompleted = true;      // 标识是否所有类型的视频都下载完成
        LogUtils.d("---------点播视频开始下载---------");
        ArrayList<PlayListCategoryItem> playbill_list = mulitcasrtBoxBean.getPlaybill_list();
        ArrayList<String> fileNames = new ArrayList<>();    // 下载成功的文件名集合（后面删除老视频会用到）

        // 抽出版本信息
        ArrayList<VersionInfo> newVersions = new ArrayList<>();
        StringBuilder versionSequence = new StringBuilder();
        for (int i = 0; i < playbill_list.size(); i++) {
            PlayListCategoryItem item = playbill_list.get(i);
            newVersions.add(item.getVersion());
            if (i != 0) {
                versionSequence.append("_");
            }
            versionSequence.append(item.getVersion().getVersion());
        }
        String versionStr = versionSequence.toString();
        session.setDownloadingVodVersion(newVersions);

        for (int i = 0; i < playbill_list.size(); i++) {
            PlayListCategoryItem item = playbill_list.get(i);
            VersionInfo versionInfo = item.getVersion();
            if (versionInfo == null || TextUtils.isEmpty(versionInfo.getType())) {
                continue;
            }

            List<MediaLibBean> mediaLibList = item.getMedia_lib();
            int completedCount = 0;     // 下载成功个数
            if (mediaLibList != null && mediaLibList.size() > 0) {

                LogUtils.d("---------轮播视频开始下载---------");
                for (MediaLibBean mediaLib : mediaLibList) {
                    String url = baseUrl + mediaLib.getUrl();
                    String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast) + mediaLib.getName();
                    fileNames.add(mediaLib.getName());
                    try {
                        boolean isChecked = false;
                        if (isDownloadCompleted(path, mediaLib.getMd5())) {
                            isChecked = true;
                        } else {
                            File file = new File(path);
                            if (file.exists() && file.isFile()) {
                                file.delete();
                            }
                            boolean isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                            if (isDownloaded && isDownloadCompleted(path, mediaLib.getMd5())) {
                                isChecked = true;
                            }
                        }
                        if (isChecked) {
                            completedCount++;

                            // 入库
                            String selection = DBHelper.MediaDBInfo.FieldName.TITLE + "=? ";
                            String[] selectionArgs = new String[]{mediaLib.getName()};
                            List<OnDemandBean> list = dbHelper.findMutlicastMediaLibByWhere(selection, selectionArgs);
                            if (list != null && list.size() > 1) {
                                dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB, selection, selectionArgs);
                                dbHelper.insertOrUpdateMulticastLib(mediaLib, false);
                            } else if (list != null && list.size() == 1) {
                                dbHelper.insertOrUpdateMulticastLib(mediaLib, true);
                            } else {
                                dbHelper.insertOrUpdateMulticastLib(mediaLib, false);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (mediaLibList == null || completedCount == mediaLibList.size()) {

                // 记录日志
                LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                        Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "update", versionInfo.getType(), "",
                        "", Session.get(context).getVersionName(), Session.get(context).getAdsPeriod(),
                        Session.get(context).getVodPeriod(), "");
            } else {
                isAllCompleted = false;
            }
        }

        if (isAllCompleted) {
            LogUtils.d("---------点播视频下载完成---------");

            session.setVodVersion(newVersions);
            session.setMulticastMediaPeriod(versionStr);

            deleteMediaFileNotInConfig(fileNames, AppUtils.StorageFile.multicast);
        }

    }


//    private void onDemandDownloadMethod(final MediaLibBean bean){
//        String p = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast);
//        String path = p+bean.getName();
//        ServerInfo serverInfo = session.getServerInfo();
//        String baseUrl = "";
//        if (serverInfo!=null&&!TextUtils.isEmpty(serverInfo.getDownloadUrl())){
//            baseUrl = serverInfo.getDownloadUrl();
//            if (baseUrl.endsWith("/")){
//                baseUrl = baseUrl.substring(0,baseUrl.length()-1);
//            }
//        }else{
//            return;
//        }
//        String url = baseUrl+ bean.getUrl();
//        try {
//            boolean downloaded = false;
//            if(isDownloadCompleted(path,bean.getMd5())){
//                downloaded = true;
//            }else{
//               File file = new File(path);
//                if (file.exists()&&file.isFile()){
//                    file.delete();
//                }
//                boolean isDownloaded = new ProgressDownloader(url,new File(path)).download(0);
//                if (isDownloaded&&isDownloadCompleted(path,bean.getMd5())){
//                    downloaded = true;
//                }
//            }
//            if (downloaded){
//                mDemandList.add(bean.getName());
//                String selection = DBHelper.MediaDBInfo.FieldName.TITLE + "=? ";
//                String[] selectionArgs = new String[]{bean.getName()};
//                List<OnDemandBean> list= dbHelper.findMutlicastMediaLibByWhere(selection,selectionArgs);
//                if (list!=null&&list.size()>1){
//                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB,selection,selectionArgs);
//                    dbHelper.insertOrUpdateMulticastLib(bean,false);
//                }else if (list!=null&&list.size()==1){
//                    dbHelper.insertOrUpdateMulticastLib(bean,true);
//                }else {
//                    dbHelper.insertOrUpdateMulticastLib(bean,false);
//                }
//
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }


    /**
     * 删除没有在小平台配置文件内的点播文件和点播数据库记录
     *
     * @param arrayList
     * @param storage
     */
    private void deleteMediaFileNotInConfig(List<String> arrayList, AppUtils.StorageFile storage) {
        File[] listFiles = new File(AppUtils.getFilePath(context, storage)).listFiles();
        for (File file : listFiles) {
            if (file.isFile()) {
                String oldName = file.getName();
                if (!arrayList.contains(oldName)) {
                    if (file.delete()) {
                        String selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=? ";
                        String[] selectionArgs = new String[]{oldName};
                        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB, selection, selectionArgs);
                    }
                }
            } else {
                FileUtils.deleteFile(file);
            }
        }
    }

    /**
     * 监听点播视频下载线程
     *
     * @param period
     */
    boolean multicastThreadRunning = false;

//    private void monitorMutlicastDownloadThread(final String period) {
//        if (multicastThreadRunning) {
//            return;
//        }
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                multicastThreadRunning = true;
//                int count = 0;
//                while (true) {
//                    List<OnDemandBean> list = null;//dbHelper.findMutlicastMediaLib();
//                    if (list == null) {
//                        break;
//                    }
//                    if (list != null && list.size() - 1 == count) {
//                        session.setMulticastMediaPeriod(period);
//                        break;
//                    }
//                    for (OnDemandBean bean : list) {
//                        boolean isCompleted = TextUtils.isEmpty(bean.getTaskId()) || queryDownloadStatus(bean.getTaskId());
//                        try {
//                            if (isCompleted) {
//                                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast) + bean.getTitle();
//                                if (isDownloadCompleted(path, bean.getMd5())) {
//
//                                    count++;
//                                }
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    try {
//                        Thread.sleep(60 * 1000 * 2);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                multicastThreadRunning = false;
//            }
//        }).start();
//
//    }

    @Override
    public void onError(AppApi.Action method, Object obj) {

    }

    @Override
    public void onNetworkFailed(AppApi.Action method) {

    }
}
