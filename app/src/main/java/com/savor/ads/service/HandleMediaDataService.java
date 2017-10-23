package com.savor.ads.service;

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
import com.savor.ads.bean.ProgramBean;
import com.savor.ads.bean.ProgramBeanResult;
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
 * 处理下载媒体文件逻辑的服务（下载内容分为两大块，第一是节目单，第二是点播）
 * 节目单分为三大块：既分三个接口返回数据
 * 1：节目接口，返回整个节目单，包含节目视频内容和宣传片以及广告的占位符
 * 2：宣传片接口，返回所有的宣传片，只有当节目和宣传片全部下载完成以后，才可以播放本期视频
 * 3: 广告接口，返回节目单中广告位上的广告
 * Created by bichao on 2016/12/10.
 */

public class HandleMediaDataService extends Service implements ApiRequestListener {

    private Context context;
    private Session session;
    private String logo_md5 = null;
    private String loading_img_md5 = null;
    //小平台返回的节目数据
    private SetTopBoxBean setTopBoxBean;
    //小平台返回的宣传片数据
    private ProgramBean programAdvBean;
    //小平台返回的广告片数据
    private ProgramBean programAdsBean;
    //接口返回的点播数据
    private SetTopBoxBean mulitcasrtBoxBean;
    /**
     * 接口返回的盒子信息
     */
    private BoxInitBean boxInitBean;

    private DBHelper dbHelper;
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    /**
     * 每次第一次开机运行的时候都检测一遍本地文件文件，如果损毁就重新下载
     */
    private boolean isFirstRun = true;
    /**
     * 广告下载
     */
    private boolean isAdsFirstRun = true;
    /**
     * 1.启动的时候写电视机播放音量和切换时间日志
     * 2.当音量和时间发生改变的时候写日志
     */
    private boolean isProduceLog = false;

    private boolean isProCompleted = false;  //节目是否下载完毕
    private String mProCompletedPeriod;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        this.session = Session.get(this);
        dbHelper = DBHelper.get(context);
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

                    LogFileUtil.write("HandleMediaDataService will start getProgramDataFromSmallPlatform");
                    // 同步获取轮播节目媒体数据
                    getProgramDataFromSmallPlatform();
                    //同步获取宣传片媒体数据
                    getAdvDataFromSmallPlatform();
                    //同步获取广告片媒体数据
                    getAdsDataFromSmallPlatform();
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
     * 获取小平台节目单媒体文件
     */
    private void getProgramDataFromSmallPlatform() {
        isProCompleted = false;
        try {
            String configJson = AppApi.getProgramDataFromSmallPlatform(this, this, session.getEthernetMac());

            Object result = gson.fromJson(configJson, new TypeToken<SetBoxTopResult>() {
            }.getType());
            if (result instanceof SetBoxTopResult) {
                SetBoxTopResult setBoxTopResult = (SetBoxTopResult) result;
                if (setBoxTopResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                    if (setBoxTopResult.getResult() != null) {
                        setTopBoxBean = setBoxTopResult.getResult();
                        handleSmallPlatformProgramData();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取小平台宣传片媒体文件
     */
    private void getAdvDataFromSmallPlatform() {
        try {
            String configJson = AppApi.getAdvDataFromSmallPlatform(this, this, session.getEthernetMac());

            Object result = gson.fromJson(configJson, new TypeToken<ProgramBeanResult>() {
            }.getType());
            if (result instanceof ProgramBeanResult) {
                ProgramBeanResult programBeanResult = (ProgramBeanResult) result;
                if (programBeanResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                    if (programBeanResult.getResult() != null) {
                        programAdvBean = programBeanResult.getResult();
                        handleSmallPlatformAdvData();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拉取小平台上广告媒体文件
     */
    private void getAdsDataFromSmallPlatform() {
        try {
            String configJson = AppApi.getAdsDataFromSmallPlatform(this, this, session.getEthernetMac());
            Object result = gson.fromJson(configJson, new TypeToken<ProgramBeanResult>() {
            }.getType());
            if (result instanceof ProgramBeanResult) {
                ProgramBeanResult programBeanResult = (ProgramBeanResult) result;
                if (programBeanResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                    if (programBeanResult.getResult() != null) {
                        programAdsBean = programBeanResult.getResult();
                        handleSmallPlatformAdsData();
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
     * 处理小平台返回的节目单数据（包含内容数据和宣传片占位和广告占位）
     */
    private void handleSmallPlatformProgramData() {
        if (setTopBoxBean == null || setTopBoxBean.getPlaybill_list() == null || setTopBoxBean.getPlaybill_list().isEmpty()) {
            return;
        }
        //该集合包含三部分数据，1:真实节目，2：宣传片占位符.3:广告占位符
        ArrayList<PlayListCategoryItem> playbill_list = setTopBoxBean.getPlaybill_list();
        //当前最新节目期号
        String proPeriod = null;

        for (PlayListCategoryItem item : playbill_list) {
            if (ConstantValues.PRO.equals(item.getVersion().getType())) {
                proPeriod = item.getVersion().getVersion();

                //如果期数相同，则表示数据没有改变，不需要执行后续的下载动作（第一次循环即便期号相同，也做一次遍历作为文件校验）
                LogUtils.d("===============proMediaPeriod===========" + session.getProPeriod());
                if (!isFirstRun &&
                        (session.getProPeriod().equals(proPeriod) || session.getProNextPeriod().equals(proPeriod))) {
                    continue;
                }

                // 设置下载中期号
                session.setProDownloadPeriod(proPeriod);
            }

            VersionInfo versionInfo = item.getVersion();
            if (versionInfo == null || TextUtils.isEmpty(versionInfo.getType())) {
                continue;
            }

            // 先从下载表中删除该期的记录
            String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "=? and " + DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?";
            String[] selectionArgs = new String[]{versionInfo.getVersion(), versionInfo.getType()};
            dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);

            List<MediaLibBean> mediaLibList = item.getMedia_lib();
            int downloadedCount = 0;
            if (mediaLibList != null && mediaLibList.size() > 0) {

                ServerInfo serverInfo = session.getServerInfo();
                if (serverInfo == null) {
                    break;
                }

                String baseUrl = serverInfo.getDownloadUrl();
                if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }

                LogUtils.d("---------轮播视频开始下载---------");
                for (MediaLibBean mediaItem : mediaLibList) {
                    try {
                        boolean isChecked = false;
                        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + mediaItem.getName();
                        //判断当前数据是节目还是其他，如果是节目走下载逻辑,其他则直接入库
                        if (ConstantValues.PRO.equals(versionInfo.getType())) {
                            String url = baseUrl + mediaItem.getUrl();

                            // 下载、校验
                            if (isDownloadCompleted(path, mediaItem.getMd5())) {
                                isChecked = true;
                            } else {
                                boolean isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                                if (isDownloaded && isDownloadCompleted(path, mediaItem.getMd5())) {
                                    isChecked = true;
                                }
                            }
                        } else {
                            isChecked = true;
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
                            bean.setLocation_id(mediaItem.getLocation_id());
                            bean.setMediaPath(path);
                            // 插库成功，downloadedCount加1
                            if (dbHelper.insertOrUpdateNewPlayListLib(bean, -1)) {
                                downloadedCount++;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (ConstantValues.PRO.equals(versionInfo.getType())) {
                // 下载完成（mediaLibList==null或size为0也认为是下载完成，认为新的节目单中没有该类型的数据）
                if (mediaLibList == null || downloadedCount == mediaLibList.size()) {
                    LogUtils.d("---------节目视频下载完成---------");
                    isProCompleted = true;
                    mProCompletedPeriod = proPeriod;
                    // 记录日志
                    LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                            Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                            String.valueOf(System.currentTimeMillis()), "update", versionInfo.getType(), "",
                            "", Session.get(context).getVersionName(), versionInfo.getVersion(),
                            Session.get(context).getVodPeriod(), "");
                } else {
                    isProCompleted = false;
                }
            }
        }
    }

    /**
     * 处理小平台返回的宣传片数据(下载完宣传片数据之后需要更新到节目单中，组合成可播放的节目单)
     */
    private void handleSmallPlatformAdvData() {
        if (programAdvBean == null || programAdvBean.getVersion() == null || TextUtils.isEmpty(programAdvBean.getVersion().getVersion())) {
            return;
        }

        String advPeriod = programAdvBean.getVersion().getVersion();
        if (!isFirstRun &&
                (session.getAdvPeriod().equals(advPeriod) || session.getAdvNextPeriod().equals(advPeriod))) {
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

        boolean isAdvCompleted = false;
        session.setAdvDownloadPeriod(programAdvBean.getVersion().getVersion());
        if (programAdvBean.getMedia_lib() != null && programAdvBean.getMedia_lib().size() > 0) {
            int advDownloadedCount = 0;
            for (MediaLibBean bean : programAdvBean.getMedia_lib()) {
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + bean.getName();
                String url = baseUrl + bean.getUrl();
                boolean isChecked = false;
                try {
                    // 下载、校验
                    if (isDownloadCompleted(path, bean.getMd5())) {
                        isChecked = true;
                    } else {
                        boolean isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                        if (isDownloaded && isDownloadCompleted(path, bean.getMd5())) {
                            isChecked = true;
                        }
                    }
                    if (isChecked) {
                        String selection = DBHelper.MediaDBInfo.FieldName.MEDIATYPE
                                + "=? and "
                                + DBHelper.MediaDBInfo.FieldName.LOCATION_ID
                                + "=? ";
                        String[] selectionArgs = new String[]{bean.getType(), bean.getLocation_id()};
                        List<PlayListBean> list = dbHelper.findNewPlayListByWhere(selection, selectionArgs);
                        int id = -1;
                        if (list != null) {
                            if (list.size() > 1) {
                                dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);
                            } else if (list.size() == 1) {
                                id = list.get(0).getId();
                            }
                        }
                        if (id != -1) {
                            PlayListBean playListBean = new PlayListBean();
                            playListBean.setPeriod(bean.getPeriod());
                            playListBean.setDuration(bean.getDuration());
                            playListBean.setMd5(bean.getMd5());
                            playListBean.setVid(bean.getVid());
                            playListBean.setMedia_name(bean.getName());
                            playListBean.setMedia_type(bean.getType());
                            playListBean.setOrder(list.get(0).getOrder());
                            playListBean.setSurfix(bean.getSurfix());
                            playListBean.setMediaPath(path);
                            playListBean.setLocation_id(bean.getLocation_id());
                            // 插库成功，downloadedCount加1
                            if (dbHelper.insertOrUpdateNewPlayListLib(playListBean, id)) {
                                advDownloadedCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (advDownloadedCount == programAdvBean.getMedia_lib().size()) {
                isAdvCompleted = true;
            } else {
                isAdvCompleted = false;
            }
        } else {
            isAdvCompleted = true;
        }

        if (isAdvCompleted && isProCompleted && !TextUtils.isEmpty(programAdvBean.getMenu_num()) &&
                programAdvBean.getMenu_num().equals(mProCompletedPeriod)) {
            isFirstRun = false;
            // 记录日志
            LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                    Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "update", programAdvBean.getVersion().getType(), "",
                    "", Session.get(context).getVersionName(), programAdvBean.getVersion().getVersion(),
                    Session.get(context).getVodPeriod(), "");

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
                session.setProPeriod(mProCompletedPeriod);
                session.setAdvPeriod(advPeriod);
            } else {
                session.setProNextPeriod(mProCompletedPeriod);
                session.setProNextMediaPubTime(setTopBoxBean.getPub_time());
                session.setAdvNextPeriod(advPeriod);
            }


            // 删除下载表中的当前、非预发布的节目单的内容
            String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? AND " + DBHelper.MediaDBInfo.FieldName.PERIOD + "!=?";
            String[] selectionArgs;
            selectionArgs = new String[]{session.getProPeriod(), session.getProNextPeriod()};
            dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);

            // 将下载表中的内容拷贝到播放表
            dbHelper.copyPlaylist(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, DBHelper.MediaDBInfo.TableName.PLAYLIST);

            // 如果是填充当前期（立即播放）的话，通知ADSPlayer播放
            if (fillCurrentBill) {
                notifyToPlay();
            }
        }
    }

    /**
     * 通过小平台获取广告数据
     */
    private void handleSmallPlatformAdsData() {
        if (programAdsBean == null || programAdsBean.getVersion() == null || TextUtils.isEmpty(programAdsBean.getVersion().getVersion())) {
            return;
        }
        String adsPeriod = programAdsBean.getVersion().getVersion();
        if (!isAdsFirstRun && session.getAdsPeriod().equals(adsPeriod)) {
            return;
        }

        // 先从下载表中删除该期的记录
        String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "=?";
        String[] selectionArgs = new String[]{adsPeriod};
        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.ADSLIST, selection, selectionArgs);

        session.setAdsDownloadPeriod(adsPeriod);
        boolean isAdsCompleted = false;
        if (programAdsBean.getMedia_lib() != null && programAdsBean.getMedia_lib().size() > 0) {
            ServerInfo serverInfo = session.getServerInfo();
            if (serverInfo == null) {
                return;
            }
            String baseUrl = serverInfo.getDownloadUrl();
            if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            int adsDownloadedCount = 0;
            for (MediaLibBean bean : programAdsBean.getMedia_lib()) {
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + bean.getName();
                String url = baseUrl + bean.getUrl();
                boolean isChecked = false;
                try {
                    // 下载、校验
                    if (isDownloadCompleted(path, bean.getMd5())) {
                        isChecked = true;
                    } else {
                        boolean isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                        if (isDownloaded && isDownloadCompleted(path, bean.getMd5())) {
                            isChecked = true;
                        }
                    }
                    if (isChecked) {
                        PlayListBean playListBean = new PlayListBean();
                        playListBean.setPeriod(bean.getPeriod());
                        playListBean.setDuration(bean.getDuration());
                        playListBean.setMd5(bean.getMd5());
                        playListBean.setVid(bean.getVid());
                        playListBean.setMedia_name(bean.getName());
                        playListBean.setMedia_type(bean.getType());
                        playListBean.setOrder(bean.getOrder());
                        playListBean.setSurfix(bean.getSurfix());
                        playListBean.setMediaPath(path);
                        playListBean.setStart_date(bean.getStart_date());
                        playListBean.setEnd_date(bean.getEnd_date());
                        playListBean.setLocation_id(bean.getLocation_id());
                        // 插库成功，mDownloadedList中加入一条
                        if (dbHelper.insertOrUpdateAdsList(playListBean, -1)) {
                            adsDownloadedCount++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (adsDownloadedCount == programAdsBean.getMedia_lib().size()) {
                isAdsCompleted = true;
            } else {
                isAdsCompleted = false;
            }
        } else {
            isAdsCompleted = true;
        }

        if (isAdsCompleted) {
            isAdsFirstRun = false;
            session.setAdsPeriod(adsPeriod);
            // 从ADS下载表中删除非该期的记录
            String selectionD = DBHelper.MediaDBInfo.FieldName.PERIOD + "!=?";
            String[] selectionArgsD = new String[]{adsPeriod};
            dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.ADSLIST, selectionD, selectionArgsD);

            // 记录日志
            LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                    Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "update", programAdsBean.getVersion().getType(), "",
                    "", Session.get(context).getVersionName(), programAdsBean.getVersion().getVersion(),
                    Session.get(context).getVodPeriod(), "");

            notifyToPlay();
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
        if (fillPlayList()) {
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

                // 特殊处理ads数据
                if (bean.getMedia_type().equals(ConstantValues.ADS)) {
                    String selection = DBHelper.MediaDBInfo.FieldName.LOCATION_ID
                            + "=? ";
                    String[] selectionArgs = new String[]{bean.getLocation_id()};
                    List<PlayListBean> list = dbHelper.findAdsByWhere(selection, selectionArgs);
                    if (list != null && !list.isEmpty()) {
                        for (PlayListBean item :
                                list) {
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date startDate = null;
                            Date endDate = null;
                            try {
                                startDate = format.parse(item.getStart_date());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            try {
                                endDate = format.parse(item.getEnd_date());
                                // 截止日期要加1天
                                endDate.setTime(endDate.getTime() + (1000 * 60 * 60 * 24));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            Date now = new Date();
                            if (startDate != null && endDate != null &&
                                    now.after(startDate) && now.before(endDate)) {
                                bean.setVid(item.getVid());
                                bean.setDuration(item.getDuration());
                                bean.setMd5(item.getMd5());
                                bean.setMedia_name(item.getMedia_name());
                                bean.setMediaPath(item.getMediaPath());
                                break;
                            }
                        }
                    }
                }

                File mediaFile = new File(bean.getMediaPath());
                boolean fileCheck = false;
                if (!TextUtils.isEmpty(bean.getMd5()) &&
                        !TextUtils.isEmpty(bean.getMediaPath()) &&
                        mediaFile.exists()) {
                    if (!bean.getMd5().equals(AppUtils.getMD5Method(mediaFile))) {
                        fileCheck = true;

                        TechnicalLogReporter.md5Failed(this, bean.getVid());
                    }
                } else {
                    fileCheck = true;
                }

                if (fileCheck) {
                    LogUtils.e("媒体文件校验失败! vid:" + bean.getVid());
                    // 校验失败时将文件路径置空，下面会删除掉为空的项
                    bean.setMediaPath(null);
                    if (mediaFile.exists()) {
                        mediaFile.delete();
                    }

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
                }
            }
        }

        if (playList != null && !playList.isEmpty()) {
            ArrayList<PlayListBean> list = new ArrayList<>();
            for (PlayListBean bean : playList) {
                if (!TextUtils.isEmpty(bean.getMediaPath())) {
                    list.add(bean);
                }
            }
            GlobalValues.PLAY_LIST = list;
            return true;
        } else {
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
            session.setOssAreaId(boiteBean.getAreaId());
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


    @Override
    public void onError(AppApi.Action method, Object obj) {

    }

    @Override
    public void onNetworkFailed(AppApi.Action method) {

    }
}
