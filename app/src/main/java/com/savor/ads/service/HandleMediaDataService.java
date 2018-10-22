package com.savor.ads.service;

import android.app.Activity;
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
import com.savor.ads.BuildConfig;
import com.savor.ads.activity.TvPlayerActivity;
import com.savor.ads.activity.TvPlayerGiecActivity;
import com.savor.ads.bean.BoxInitBean;
import com.savor.ads.bean.BoxInitResult;
import com.savor.ads.bean.JsonBean;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.PrizeInfo;
import com.savor.ads.bean.ProgramBean;
import com.savor.ads.bean.ProgramBeanResult;
import com.savor.ads.bean.RstrSpecialty;
import com.savor.ads.bean.RstrSpecialtyOuterBean;
import com.savor.ads.bean.RstrSpecialtyResult;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.bean.SetBoxTopResult;
import com.savor.ads.bean.SetTopBoxBean;
import com.savor.ads.bean.Television;
import com.savor.ads.bean.TvProgramGiecResponse;
import com.savor.ads.bean.TvProgramResponse;
import com.savor.ads.bean.VersionInfo;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.log.LogReportUtil;
import com.savor.ads.log.LotteryLogUtil;
import com.savor.ads.okhttp.coreProgress.download.ProgressDownloader;
import com.savor.ads.oss.OSSUtils;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.TechnicalLogReporter;
import com.savor.ads.utils.UpdateUtil;
import com.savor.ads.utils.tv.TvOperate;
import com.savor.tvlibrary.AtvChannel;
import com.savor.tvlibrary.ITVOperator;
import com.savor.tvlibrary.TVOperatorFactory;
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
    /**
     * 是否连接实体小平台
     */
    private boolean isConnectedEntity=true;
    /**
     * 平台返回的节目单数据
     */
    private SetTopBoxBean setTopBoxBean;
    /**
     * 广告集合
     */
    private ProgramBean adsProgramBean;
    /**
     * 宣传片集合
     */
    private ProgramBean advProgramBean;
    /**
     * poly广告集合
     */
    private ProgramBean polyAdsProgramBean;

    private SetTopBoxBean multicastBoxBean;
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

    private int poly_timeout_count = 0;
    private int pro_timeout_count = 0;
    private int ads_timeout_count = 0;
    private int adv_timeout_count = 0;
    private int vod_timeout_count = 0;
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

                // 等10秒再开始下载
                try {
                    Thread.sleep(1000 * 30);
//                    Thread.sleep(1000 * 60*3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (true) {

                    try {
                        // 循环检查网络、小平台信息的情况直到可用
                        do {
                            LogFileUtil.write("HandleMediaDataService will check server info and network");
                            if (AppUtils.isNetworkAvailable(context) &&
                                    session.getServerInfo() != null &&
                                    !TextUtils.isEmpty(AppUtils.getMainMediaPath())) {
                                break;
                            }

                            try {
                                Thread.sleep(1000 * 2);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } while (true);

                        LogFileUtil.write("HandleMediaDataService will start UpdateUtil");
                        /**异步更新apk、rom,进入下载逻辑首先执行升级方法**/
                        new UpdateUtil(context);

                        LogFileUtil.write("HandleMediaDataService will check space available");
                        // 检测剩余存储空间
                        if (AppUtils.getAvailableExtSize() < ConstantValues.EXTSD_LEAST_AVAILABLE_SPACE/2) {
                            // 存储空间不足
                            LogFileUtil.writeException(new Throwable("Low spaces in media partition"));

                            // 清理可清理的视频等文件
                            cleanMediaWhenSpaceLow();
                            // 提前播放的pro在上面这一步可能已经被删除，这里重新填充节目单并通知播放
                            notifyToPlay();
                            // 上报服务器 卡满异常
                            AppApi.reportSDCardState(context, HandleMediaDataService.this, 2);

                        } else {
                            // 空间充足，开始更新资源

//                            getPrizeInfo();

                            LogFileUtil.write("HandleMediaDataService will start getBoxInfo");
                            // 同步获取机顶盒基本信息，包括logo、loading图
                            getBoxInfo();

                            // 检测预约发布的播放时间是否已到达，启动时不检测因为已经在Application中检测过了
                            if (!isFirstRun && AppUtils.checkPlayTime(context)) {
                                notifyToPlay();
                            }

                            LogFileUtil.write("HandleMediaDataService will start getProgramDataFromSmallPlatform");
                            // 同步获取轮播节目媒体数据
                            getProgramDataFromSmallPlatform(false);
                            LogFileUtil.write("HandleMediaDataService will start getAdvDataFromSmallPlatform");
                            //同步获取宣传片媒体数据
                            getAdvDataFromSmallPlatform(false);
                            LogFileUtil.write("HandleMediaDataService will start getPolyAdsFromSmallPlatform");
                            // 同步获取聚屏物料媒体数据
                            getPolyAdsFromSmallPlatform(false);
                            LogFileUtil.write("HandleMediaDataService will start getAdsDataFromSmallPlatform");
                            //同步获取广告片媒体数据
                            getAdsDataFromSmallPlatform(false);
                            LogFileUtil.write("HandleMediaDataService will start getOnDemandDataFromSmallPlatform");
                            // 同步获取点播媒体数据
                            getOnDemandDataFromSmallPlatform(false);
                            // 获取特色菜媒体数据
                            getSpecialtyFromSmallPlatform();
                            // 获取实时竞价媒体数据
                            getRtbAdsFromSmallPlatform();
    //

                            LogFileUtil.write("HandleMediaDataService will start getTVMatchDataFromSmallPlatform");
                            // 异步获取电视节目信息
                            getTVMatchDataFromSmallPlatform();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogFileUtil.writeException(e);
                    }

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

    private void cleanMediaWhenSpaceLow() {
        // 删除下载表中的当前、非下载中的节目单的内容
        String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? AND " + DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? AND " +
                DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? AND " + DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? ";
        String[] selectionArgs;
        selectionArgs = new String[]{session.getProPeriod(), session.getProDownloadPeriod(), session.getAdvPeriod(), session.getAdvDownloadPeriod()};
        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);

        AppUtils.deleteOldMedia(this);
        AppUtils.deleteMulticastMedia(this);
        AppUtils.clearPptTmpFiles(HandleMediaDataService.this);
    }

    @Deprecated
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
            JsonBean jsonBean = AppApi.getBoxInitInfo(this, this, session.getEthernetMac());
            JSONObject jsonObject = new JSONObject(jsonBean.getConfigJson());
            if (jsonObject.getInt("code") != AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                LogUtils.d("接口返回的状态不对,code=" + jsonObject.getInt("code"));
                return;
            }

            Object result = gson.fromJson(jsonBean.getConfigJson(), new TypeToken<BoxInitResult>() {
            }.getType());
            if (result instanceof BoxInitResult) {
                BoxInitBean boxInitBean = ((BoxInitResult) result).getResult();
                /*******************设置盒子基本信息开始************************/
                initBoxInfo(boxInitBean,jsonBean.getSmallType());
                /*******************设置盒子基本信息结束************************/
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {

        }
    }

    /**
     * 获取小平台节目单媒体文件
     * OSSsource true是从OSS下载，false是从实体小平台下载
     */
    private void getProgramDataFromSmallPlatform(boolean OSSsource) {
        isProCompleted = false;
        try {
            JsonBean jsonBean = AppApi.getProgramDataFromSmallPlatform(this, this, session.getEthernetMac());
            // 保存拿到的数据到本地
            FileUtils.write(ConstantValues.PRO_DATA_PATH, jsonBean.getConfigJson());

            SetBoxTopResult setBoxTopResult = gson.fromJson(jsonBean.getConfigJson(), new TypeToken<SetBoxTopResult>() {
            }.getType());
            if (setBoxTopResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                if (setBoxTopResult.getResult() != null) {
                    setTopBoxBean = setBoxTopResult.getResult();
                    isConnectedEntity = true;
                    handleSmallPlatformProgramData(jsonBean.getSmallType(),OSSsource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!TextUtils.isEmpty(e.getMessage())&&(e.getMessage().contains("failed to connect to")||e.getMessage().contains("No route to host"))){
                isConnectedEntity = false;
                handleSmallPlatformProgramData("",true);
            }
        }
    }

    /**
     * 获取小平台宣传片媒体文件
     * OSSsource true从OSS下载，false从实体小平台下载
     */
    private void getAdvDataFromSmallPlatform(boolean OSSsource) {
        try {
            JsonBean jsonBean = AppApi.getAdvDataFromSmallPlatform(this, this, session.getEthernetMac());
            // 保存拿到的数据到本地
            FileUtils.write(ConstantValues.ADV_DATA_PATH, jsonBean.getConfigJson());

            Object result = gson.fromJson(jsonBean.getConfigJson(), new TypeToken<ProgramBeanResult>() {
            }.getType());
            ProgramBeanResult programBeanResult = (ProgramBeanResult) result;
            if (programBeanResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                if (programBeanResult.getResult() != null) {
                    isConnectedEntity = true;
                    advProgramBean = programBeanResult.getResult();
                    handleSmallPlatformAdvData(jsonBean.getSmallType(),OSSsource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!TextUtils.isEmpty(e.getMessage())&&(e.getMessage().contains("failed to connect to")||e.getMessage().contains("No route to host"))){
                isConnectedEntity = false;
                handleSmallPlatformAdvData("",true);
            }
        }
    }

    /**
     * 拉取小平台上广告媒体文件
     * @param OSSsource true是从OSS上下载，false是从实体小平台下载
     */
    private void getAdsDataFromSmallPlatform(boolean OSSsource) {
        try {
            JsonBean jsonBean = AppApi.getAdsDataFromSmallPlatform(this, this, session.getEthernetMac());
            // 保存拿到的数据到本地
            FileUtils.write(ConstantValues.ADS_DATA_PATH, jsonBean.getConfigJson());
            Object result = gson.fromJson(jsonBean.getConfigJson(), new TypeToken<ProgramBeanResult>() {
            }.getType());
            ProgramBeanResult programBeanResult = (ProgramBeanResult) result;
            if (programBeanResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                if (programBeanResult.getResult() != null) {
                    isConnectedEntity = true;
                    adsProgramBean = programBeanResult.getResult();
                    handleSmallPlatformAdsData(jsonBean.getSmallType(),OSSsource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!TextUtils.isEmpty(e.getMessage())&&(e.getMessage().contains("failed to connect to")||e.getMessage().contains("No route to host"))){
                isConnectedEntity = false;
                handleSmallPlatformAdsData("",true);
            }
        }
    }

    private void getOnDemandDataFromSmallPlatform(boolean OSSsource) {
        try {
            JsonBean jsonBean = AppApi.getOnDemandDataFromSmallPlatform(this, this, session.getEthernetMac());
            Object result = gson.fromJson(jsonBean.getConfigJson(), new TypeToken<SetBoxTopResult>() {
            }.getType());
            SetBoxTopResult setBoxTopResult = (SetBoxTopResult) result;
            if (setBoxTopResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS && setBoxTopResult.getResult() != null) {
                isConnectedEntity = true;
                multicastBoxBean = setBoxTopResult.getResult();
                handleSmallPlatformOnDemandData(jsonBean.getSmallType(),OSSsource);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!TextUtils.isEmpty(e.getMessage())&&(e.getMessage().contains("failed to connect to")||e.getMessage().contains("No route to host"))){
                isConnectedEntity = false;
                handleSmallPlatformOnDemandData("",true);
            }
        }
    }

    private void getSpecialtyFromSmallPlatform() {
        try {
            JsonBean jsonBean = AppApi.getSpecialtyFromSmallPlatform(this, this, session.getEthernetMac());
            Object result = gson.fromJson(jsonBean.getConfigJson(), new TypeToken<RstrSpecialtyResult>() {
            }.getType());
            RstrSpecialtyResult setBoxTopResult = (RstrSpecialtyResult) result;
            if (setBoxTopResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS && setBoxTopResult.getResult() != null) {
                handleSpecialtyResult(setBoxTopResult.getResult(),jsonBean.getSmallType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSpecialtyResult(RstrSpecialtyOuterBean bean,String smallType) {
        if (bean == null||bean.getMedia_lib()==null||bean.getMedia_lib().size()==0) {
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

        boolean isAllCompleted = true;      // 标识是否所有的媒体文件都下载完成
        ArrayList<String> fileNames = new ArrayList<>();    // 下载成功的文件名集合（后面删除老文件会用到）

        VersionInfo versionInfo = bean.getVersion();
        if (versionInfo == null || TextUtils.isEmpty(versionInfo.getVersion())) {
            return;
        }

        String logUUID = String.valueOf(System.currentTimeMillis());
        // 记录下载开始日志
        int count = bean.getMedia_lib() == null ? 0 : bean.getMedia_lib().size();
        LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                String.valueOf(System.currentTimeMillis()), "start", "specialty_down", versionInfo.getVersion(),
                "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(count));

        session.setDownloadingSpecialtyPeriod(versionInfo.getVersion());
        List<RstrSpecialty> mediaLibList = bean.getMedia_lib();
        int completedCount = 0;     // 下载成功个数
        LogUtils.d("---------特色菜开始下载---------");
        if (mediaLibList != null && mediaLibList.size() > 0) {

            for (RstrSpecialty mediaLib : mediaLibList) {
                String url = baseUrl + mediaLib.getUrl();
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.specialty) + mediaLib.getName();
                fileNames.add(mediaLib.getName());
                try {
                    boolean isChecked = false;
                    if (isImageDownloadCompleted(path, mediaLib.getMd5())) {
                        isChecked = true;
                    } else {
                        File file = new File(path);
                        if (file.exists() && file.isFile()) {
                            file.delete();
                        }
                        boolean isDownloaded = false;
                        if (ConstantValues.VIRTUAL.equals(smallType)){
                            OSSUtils ossUtils = new OSSUtils(context,
                                    BuildConfig.OSS_BUCKET_NAME,
                                    mediaLib.getOss_path(),
                                    new File(path));

                            isDownloaded = ossUtils.syncDownload();
                        } else {
                            isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                        }
                        if (isDownloaded && isImageDownloadCompleted(path, mediaLib.getMd5())) {
                            isChecked = true;
                        }
                    }
                    if (isChecked) {
                        // 入库
                        String selection = DBHelper.MediaDBInfo.FieldName.FOOD_ID + "=? ";
                        String[] selectionArgs = new String[]{mediaLib.getFood_id()};
                        List<RstrSpecialty> list = dbHelper.findSpecialtyByWhere(selection, selectionArgs);
                        mediaLib.setMedia_path(path);
                        boolean isInsertSuccess = false;
                        if (list != null && list.size() > 1) {
                            dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB, selection, selectionArgs);
                            isInsertSuccess = dbHelper.insertOrUpdateSpecialtyLib(mediaLib, false);
                        } else if (list != null && list.size() == 1) {
                            isInsertSuccess = dbHelper.insertOrUpdateSpecialtyLib(mediaLib, true);
                        } else {
                            isInsertSuccess = dbHelper.insertOrUpdateSpecialtyLib(mediaLib, false);
                        }

                        if (isInsertSuccess) {
                            completedCount++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (mediaLibList == null || completedCount == mediaLibList.size()) {

                // 记录日志
                // 记录下载完成日志
                LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "end", "specialty_down", versionInfo.getVersion(),
                        "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(completedCount));
                LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                        Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "update", versionInfo.getType(), "",
                        "", Session.get(context).getVersionName(), Session.get(context).getAdsPeriod(),
                        Session.get(context).getVodPeriod(), "");
            } else {
                isAllCompleted = false;
                // 记录下载中止日志
                LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "suspend", "specialty_down", versionInfo.getVersion(),
                        "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(completedCount));
            }
        }

        if (isAllCompleted) {
            LogUtils.d("---------特色菜下载完成---------");

            session.setSpecialtyPeriod(versionInfo.getVersion());

            deleteMediaFileNotInConfig(fileNames, AppUtils.StorageFile.specialty, DBHelper.MediaDBInfo.TableName.SPECIALTY);
        }
    }

    private void getRtbAdsFromSmallPlatform() {
        try {
            JsonBean jsonBean = AppApi.getRtbadsFromSmallPlatform(this, this, session.getEthernetMac());
            Object result = gson.fromJson(jsonBean.getConfigJson(), new TypeToken<ProgramBeanResult>() {
            }.getType());
            ProgramBeanResult programBeanResult = (ProgramBeanResult) result;
            if (programBeanResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS && programBeanResult.getResult() != null) {
                handRtbadsResult(programBeanResult.getResult());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理RTB广告结果
     *
     * @param programBean
     */
    private void handRtbadsResult(ProgramBean programBean) {
        if (programBean == null
                || programBean.getVersion() == null
                || TextUtils.isEmpty(programBean.getVersion().getVersion())
                || programBean.getMedia_lib()==null
                || programBean.getMedia_lib().size()==0) {
            return;
        }
        String adsPeriod = programBean.getVersion().getVersion();
        if (session.getRtbadsPeriod().equals(adsPeriod)) {
            return;
        }

        String logUUID = String.valueOf(System.currentTimeMillis());
        // 记录下载开始日志
        int count = programBean.getMedia_lib() == null ? 0 : programBean.getMedia_lib().size();
        LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                String.valueOf(System.currentTimeMillis()), "start", "rtbads_down", adsPeriod,
                "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(count));

        session.setRtbadsDownloadPeriod(adsPeriod);
        boolean isAdsCompleted = false;
        int adsDownloadedCount = 0;
        ArrayList<String> fileNames = new ArrayList<>();    // 下载成功的文件名集合（后面删除老视频会用到）
        if (programBean.getMedia_lib() != null && programBean.getMedia_lib().size() > 0) {
            ServerInfo serverInfo = session.getServerInfo();
            if (serverInfo == null) {
                return;
            }
            String baseUrl = serverInfo.getDownloadUrl();
            if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            LogUtils.d("---------RTB广告视频开始下载---------");
            for (MediaLibBean bean : programBean.getMedia_lib()) {
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.rtb_ads) + bean.getName();
                String url = baseUrl + bean.getUrl();
                fileNames.add(bean.getName());
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
                        bean.setMediaPath(path);
                        // 入库
                        String selection = DBHelper.MediaDBInfo.FieldName.VID + "=? ";
                        String[] selectionArgs = new String[]{bean.getVid()};
                        List<MediaLibBean> list = dbHelper.findRtbadsMediaLibByWhere(selection, selectionArgs);
                        boolean isInsertSuccess = false;
                        if (list != null && list.size() > 1) {
                            dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.RTB_ADS, selection, selectionArgs);
                            isInsertSuccess = dbHelper.insertOrUpdateRTBAdsList(bean, false);
                        } else if (list != null && list.size() == 1) {
                            isInsertSuccess = dbHelper.insertOrUpdateRTBAdsList(bean, true);
                        } else {
                            isInsertSuccess = dbHelper.insertOrUpdateRTBAdsList(bean, false);
                        }

                        if (isInsertSuccess) {
                            adsDownloadedCount++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (adsDownloadedCount == programBean.getMedia_lib().size()) {
                isAdsCompleted = true;
            } else {
                isAdsCompleted = false;
            }
        } else {
            isAdsCompleted = true;
        }

        if (isAdsCompleted) {
            LogUtils.d("---------RTB广告视频下载完成---------");
            session.setRtbAdsPeriod(adsPeriod);
            // 记录日志
            // 记录下载开始日志
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "end", "rtbads_down", adsPeriod,
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(adsDownloadedCount));
            LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                    Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "update", programBean.getVersion().getType(), "",
                    "", Session.get(context).getVersionName(), programBean.getVersion().getVersion(),
                    Session.get(context).getVodPeriod(), "");


            deleteMediaFileNotInConfig(fileNames, AppUtils.StorageFile.rtb_ads, DBHelper.MediaDBInfo.TableName.RTB_ADS);
        } else {
            // 记录下载中止日志
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "suspend", "rtbads_down", adsPeriod,
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(adsDownloadedCount));
        }
    }

    /**
     * 获取百度聚屏广告
     * OSSsource true是从OSS下载，false是从实体小平台下载
     */
    private void getPolyAdsFromSmallPlatform(boolean OSSsource){
        try {
            JsonBean jsonBean = AppApi.getPolyAdsFromSmallPlatform(this,this,session.getEthernetMac());
            Object result = gson.fromJson(jsonBean.getConfigJson(), new TypeToken<ProgramBeanResult>() {
            }.getType());
            ProgramBeanResult programBeanResult = (ProgramBeanResult) result;
            if (programBeanResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS && programBeanResult.getResult() != null) {
                isConnectedEntity = true;
                polyAdsProgramBean = programBeanResult.getResult();
                handlePolyAdsFromSmallPlatform(OSSsource);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!TextUtils.isEmpty(e.getMessage())&&(e.getMessage().contains("failed to connect to")||e.getMessage().contains("No route to host"))){
                isConnectedEntity = false;
                handlePolyAdsFromSmallPlatform(true);
            }
        }

    }

    /**
     * 处理小平台返回的百度聚屏广告内容
     * 为了后期整合代码，故下载的聚屏广告并不单独创建表，统一放到rtb_ads下面
     * @param
     */
    private void handlePolyAdsFromSmallPlatform(boolean OSSsource){
        if (polyAdsProgramBean == null
                || polyAdsProgramBean.getVersion() == null
                || TextUtils.isEmpty(polyAdsProgramBean.getVersion().getVersion())
                || polyAdsProgramBean.getMedia_lib()==null
                || polyAdsProgramBean.getMedia_lib().size()==0) {
            return;
        }
        String adsPeriod = polyAdsProgramBean.getVersion().getVersion();
        if (session.getPolyAdsPeriod().equals(adsPeriod)) {
            return;
        }

        String logUUID = String.valueOf(System.currentTimeMillis());
        // 记录下载开始日志
        int count = polyAdsProgramBean.getMedia_lib() == null ? 0 : polyAdsProgramBean.getMedia_lib().size();
        LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                String.valueOf(System.currentTimeMillis()), "start", "poly_down", adsPeriod,
                "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(count));
        session.setPolyAdsDownloadPeriod(adsPeriod);

        boolean isOSS = OSSsource;
        boolean isAdsCompleted = false;
        int adsDownloadedCount = 0;
        ArrayList<String> fileNames = new ArrayList<>();    // 下载成功的文件名集合（后面删除老视频会用到）
        if (polyAdsProgramBean.getMedia_lib() != null && polyAdsProgramBean.getMedia_lib().size() > 0) {
            ServerInfo serverInfo = session.getServerInfo();
            if (serverInfo == null) {
                return;
            }
            String baseUrl = serverInfo.getDownloadUrl();
            if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            LogUtils.d("---------poly广告视频开始下载---------");
            for (MediaLibBean bean : polyAdsProgramBean.getMedia_lib()) {
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.poly_ads) + bean.getName();
                String url = baseUrl + bean.getUrl();
                fileNames.add(bean.getName());
                boolean isChecked = false;
                try {
                    // 下载、校验
                    if (isDownloadCompleted(path, bean.getMd5())) {
                        isChecked = true;
                    } else {
                        boolean isDownloaded = false;
                        if (isOSS){
                            OSSUtils ossUtils = new OSSUtils(context,
                                    BuildConfig.OSS_BUCKET_NAME,
                                    bean.getOss_path(),
                                    new File(path));
                            isDownloaded = ossUtils.syncDownload();
                        }else {
                            isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                        }

                        if (isDownloaded && isDownloadCompleted(path, bean.getMd5())) {
                            isChecked = true;
                        }

                    }
                    if (isChecked) {
                        bean.setMediaPath(path);
                        // 入库
                        String selection = DBHelper.MediaDBInfo.FieldName.VID + "=? ";
                        String[] selectionArgs = new String[]{bean.getVid()};
                        List<MediaLibBean> list = dbHelper.findRtbadsMediaLibByWhere(selection, selectionArgs);
                        boolean isInsertSuccess = false;
                        if (list != null && list.size() > 1) {
                            dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.RTB_ADS, selection, selectionArgs);
                            isInsertSuccess = dbHelper.insertOrUpdateRTBAdsList(bean, false);
                        } else if (list != null && list.size() == 1) {
                            isInsertSuccess = dbHelper.insertOrUpdateRTBAdsList(bean, true);
                        } else {
                            isInsertSuccess = dbHelper.insertOrUpdateRTBAdsList(bean, false);
                        }

                        if (isInsertSuccess) {
                            adsDownloadedCount++;
                            if (!TextUtils.isEmpty(GlobalValues.NOT_FOUND_BAIDU_ADS_KEY) &&
                                    GlobalValues.NOT_FOUND_BAIDU_ADS_KEY.equals(bean.getTp_md5())) {
                                GlobalValues.NOT_FOUND_BAIDU_ADS_KEY = null;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (adsDownloadedCount == polyAdsProgramBean.getMedia_lib().size()) {
                isAdsCompleted = true;
            } else {
                isAdsCompleted = false;
            }
        } else {
            isAdsCompleted = true;
        }

        if (isAdsCompleted) {
            LogUtils.d("---------poly广告视频下载完成---------");
            session.setPolyAdsPeriod(adsPeriod);
            // 记录日志
            // 记录下载结束日志
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "end", "polyads_down", adsPeriod,
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(adsDownloadedCount));
            LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                    Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "update", polyAdsProgramBean.getVersion().getType(), "",
                    "", Session.get(context).getVersionName(), polyAdsProgramBean.getVersion().getVersion(),
                    Session.get(context).getVodPeriod(), "");


            deleteMediaFileNotInConfig(fileNames, AppUtils.StorageFile.poly_ads, DBHelper.MediaDBInfo.TableName.RTB_ADS);
        } else {
            // 记录下载中止日志
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "suspend", "polyads_down", adsPeriod,
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(adsDownloadedCount));
            if (!isOSS){
                if(poly_timeout_count<5){
                    poly_timeout_count ++;
                    handlePolyAdsFromSmallPlatform(isOSS);
//                    getPolyAdsFromSmallPlatform(isOSS);
                }else {
                    poly_timeout_count = 0;
//                    getPolyAdsFromSmallPlatform(!isOSS);
                    handlePolyAdsFromSmallPlatform(!isOSS);
                }
            }

        }
    }


    private void getTVMatchDataFromSmallPlatform() {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof TvPlayerGiecActivity ||activity instanceof TvPlayerActivity){
            return;
        }
        if (AppUtils.isMstar()) {
            AppApi.getTVMatchDataFromSmallPlatform(this, this);
        } else {
            AppApi.getGiecTVMatchDataFromSmallPlatform(this, this);
        }
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
            case SP_GET_TV_MATCH_DATA_FROM_GIEC_JSON:
                if (obj instanceof TvProgramGiecResponse) {
                    TvProgramGiecResponse response = (TvProgramGiecResponse) obj;
                    ITVOperator tvOperate = TVOperatorFactory.getTVOperator(getApplicationContext(), TVOperatorFactory.TVType.GIEC);
                    for (AtvChannel atvChannel :
                            response.getTvChannelList()) {
                        atvChannel.setDisplayName(atvChannel.getChannelName());
//                        atvChannel.setDisplayNumber(atvChannel.getChannelNum() + "");
                    }
                    tvOperate.setAtvChannels(response.getTvChannelList());
                    session.setTvDefaultChannelNumber(response.getLockingChannelNum());
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
            case CP_POST_SDCARD_STATE_JSON:
                LogUtils.d("上报SD卡状态成功");
                break;
        }
    }

    /**
     * 机顶盒监听电视屏,控制是否当电视屏没有通电的时候关机
     * @param flag false不关机,true关机
     */
    private void setAutoClose(boolean flag) {
        try {
            TvManager.setGpioDeviceStatus(128, flag);
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理小平台返回的节目单数据（包含内容数据和宣传片占位和广告占位）
     * OSSsource true是从OSS下载，false是从实体小平台下载
     */
    private void handleSmallPlatformProgramData(String smallType,boolean OSSsource) {
        if (setTopBoxBean == null
                || setTopBoxBean.getPlaybill_list() == null
                || setTopBoxBean.getPlaybill_list().isEmpty()) {
            return;
        }
        //该集合包含三部分数据，1:真实节目，2：宣传片占位符.3:广告占位符
        ArrayList<ProgramBean> playbill_list = setTopBoxBean.getPlaybill_list();
        //当前最新节目期号
        String proPeriod = "";
        boolean isOSS = OSSsource;
        for (ProgramBean item : playbill_list) {
            if (item.getMedia_lib()==null||item.getMedia_lib().size()==0){
                continue;
            }
            String logUUID = String.valueOf(System.currentTimeMillis());
            if (ConstantValues.PRO.equals(item.getVersion().getType())) {
                proPeriod = item.getVersion().getVersion();

                //如果期数相同，则表示数据没有改变，不需要执行后续的下载动作（第一次循环即便期号相同，也做一次遍历作为文件校验）
                LogUtils.d("===============proMediaPeriod===========" + session.getProPeriod());
                if (!isFirstRun &&
                        (session.getProPeriod().equals(proPeriod) || session.getProNextPeriod().equals(proPeriod))) {
                    isProCompleted = true;
                    mProCompletedPeriod = proPeriod;
                    continue;
                }
                String selection = DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=? and " +
                        DBHelper.MediaDBInfo.FieldName.PERIOD + "=? ";
                String[] selectionArgs = new String[]{ConstantValues.PRO, proPeriod};
                dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,selection, selectionArgs);
                // 设置下载中期号
                session.setProDownloadPeriod(proPeriod);

                // 记录下载开始日志
                int count = item.getMedia_lib() == null ? 0 : item.getMedia_lib().size();
                LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "start", "pro_down", proPeriod,
                        "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(count));
            }

            VersionInfo versionInfo = item.getVersion();
            if (versionInfo == null || TextUtils.isEmpty(versionInfo.getType())) {
                continue;
            }

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
                        boolean isNewDownload = false;
                        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + mediaItem.getName();
                        //判断当前数据是节目还是其他，如果是节目走下载逻辑,其他则直接入库
                        if (ConstantValues.PRO.equals(versionInfo.getType())) {
                            String url = baseUrl + mediaItem.getUrl();
                            LogUtils.v("****开始下载pro视频:"+mediaItem.getChinese_name()+"****");
                            // 下载、校验
                            if (isDownloadCompleted(path, mediaItem.getMd5())) {
                                isChecked = true;
                                LogUtils.v("****pro视频:"+mediaItem.getChinese_name()+"下载完成****");
                            } else {
                                boolean isDownloaded = false;
                                //虚拟小平台下载
                                if (ConstantValues.VIRTUAL.equals(smallType)||isOSS){
                                    OSSUtils ossUtils = new OSSUtils(context,
                                            BuildConfig.OSS_BUCKET_NAME,
                                            mediaItem.getOss_path(),
                                            new File(path));
                                    isDownloaded = ossUtils.syncDownload();
                                }else {
                                    isDownloaded = new ProgressDownloader(url, new File(path)).download(0);

                                }
                                if (isDownloaded && isDownloadCompleted(path, mediaItem.getMd5())) {
                                    isChecked = true;
                                    isNewDownload = true;
                                    LogUtils.v("****pro视频:"+mediaItem.getChinese_name()+"下载完成****");
                                }
                            }
                        } else {
                            isChecked = true;
                        }
                        // 校验通过、插库
                         if (isChecked) {
                            mediaItem.setMediaPath(path);
                            String selection = DBHelper.MediaDBInfo.FieldName.ADS_ORDER + "=? and " +
                                    DBHelper.MediaDBInfo.FieldName.PERIOD + "=? ";
                            String[] selectionArgs = new String[]{mediaItem.getOrder() + "", mediaItem.getPeriod()};
                            List<MediaLibBean> list = dbHelper.findNewPlayListByWhere(selection, selectionArgs);
                            if (list == null || list.isEmpty()) {
                                // 插库成功，downloadedCount加1
                                if (dbHelper.insertOrUpdateNewPlayListLib(mediaItem, -1)) {
                                    downloadedCount++;

                                    if (isNewDownload) {
                                        notifyToPlay();
                                    }
                                }
                            } else {
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
                    // 记录下载完成日志
                    LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                            String.valueOf(System.currentTimeMillis()), "end", "pro_down", proPeriod,
                            "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(downloadedCount));
                    LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                            Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                            String.valueOf(System.currentTimeMillis()), "update", versionInfo.getType(), "",
                            "", Session.get(context).getVersionName(), versionInfo.getVersion(),
                            Session.get(context).getVodPeriod(), "");
                } else {
                    isProCompleted = false;
                    if (!isFirstRun) {
                        // 记录下载中止日志
                        LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                                String.valueOf(System.currentTimeMillis()), "suspend", "pro_down", proPeriod,
                                "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(downloadedCount));
                    }
                    if (!isOSS){
                        if(pro_timeout_count<5){
                            pro_timeout_count ++;
//                            getProgramDataFromSmallPlatform(isOSS);
                            handleSmallPlatformProgramData(smallType,isOSS);
                        }else{
                            pro_timeout_count = 0;
//                            getProgramDataFromSmallPlatform(!isOSS);
                            handleSmallPlatformProgramData(smallType,!isOSS);
                        }
                    }

                }
            }
        }
    }

    /**
     * 处理小平台返回的宣传片数据(下载完宣传片数据之后需要更新到节目单中，组合成可播放的节目单)
     */
    private void handleSmallPlatformAdvData(String smallType,boolean OSSsource) {
        if (advProgramBean == null
                || advProgramBean.getVersion() == null
                || TextUtils.isEmpty(advProgramBean.getVersion().getVersion())
                || advProgramBean.getMedia_lib()==null
                || advProgramBean.getMedia_lib().size()==0) {
            return;
        }

        String advPeriod = advProgramBean.getVersion().getVersion();
        if (!isFirstRun &&
                (session.getAdvPeriod().equals(advPeriod) || session.getAdvNextPeriod().equals(advPeriod))) {
            return;
        }

        ServerInfo serverInfo = session.getServerInfo();
        if (serverInfo == null) {
            return;
        }
        boolean isOSS = OSSsource;
        String baseUrl = serverInfo.getDownloadUrl();
        if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String logUUID = String.valueOf(System.currentTimeMillis());
        // 记录下载开始日志
        int count = advProgramBean.getMedia_lib() == null ? 0 : advProgramBean.getMedia_lib().size();
        LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                String.valueOf(System.currentTimeMillis()), "start", "adv_down", advPeriod,
                "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(count));

        boolean isAdvCompleted = false;
        session.setAdvDownloadPeriod(advProgramBean.getVersion().getVersion());
        int advDownloadedCount = 0;
        if (advProgramBean.getMedia_lib() != null && advProgramBean.getMedia_lib().size() > 0) {
            for (MediaLibBean bean : advProgramBean.getMedia_lib()) {
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + bean.getName();
                String url = baseUrl + bean.getUrl();
                boolean isChecked = false;
                try {
                    LogUtils.v("****开始下载adv视频:"+bean.getChinese_name()+"****");
                    // 下载、校验
                    if (isDownloadCompleted(path, bean.getMd5())) {
                        isChecked = true;
                    } else {
                        boolean isDownloaded = false;
                        //虚拟小平台
                        if (ConstantValues.VIRTUAL.equals(smallType)||isOSS){
                            OSSUtils ossUtils = new OSSUtils(context,
                                    BuildConfig.OSS_BUCKET_NAME,
                                    bean.getOss_path(),
                                    new File(path));
                            isDownloaded = ossUtils.syncDownload();
                        }else{
                            isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                        }

                        if (isDownloaded && isDownloadCompleted(path, bean.getMd5())) {
                            isChecked = true;
                        }
                    }
                    LogUtils.v("****adv视频:"+bean.getChinese_name()+"下载完成****");
                    if (isChecked) {
                        // ADV是先在handleSmallPlatformProgramData()中插入，插入时的期号是节目单号，然后在这里更新实际数据
                        String selection = DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=? and " +
                                DBHelper.MediaDBInfo.FieldName.LOCATION_ID + "=? and " +
                                DBHelper.MediaDBInfo.FieldName.PERIOD + "=? ";

                        String[] selectionArgs = new String[]{bean.getType(), bean.getLocation_id(), advPeriod};
                        List<MediaLibBean> list = dbHelper.findNewPlayListByWhere(selection, selectionArgs);
                        String[] selectionArgs2 = new String[]{bean.getType(), bean.getLocation_id(), advProgramBean.getMenu_num()};

                        if (list!=null&&!list.isEmpty()){
                            if (list.size() > 1) {
                                dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);
                            } else if (list.size() == 1) {
                                dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs2);
                                advDownloadedCount++;
                            }
                        }else{
                            list = dbHelper.findNewPlayListByWhere(selection, selectionArgs2);
                            int id = -1;
                            if (list != null) {
                                if (list.size() > 1) {
                                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs2);
                                } else if (list.size() == 1) {
                                    id = list.get(0).getId();
                                }
                            }
                            if (id != -1) {
                                bean.setOrder(list.get(0).getOrder());
                                bean.setMediaPath(path);
                                // 插库成功，downloadedCount加1
                                if (dbHelper.insertOrUpdateNewPlayListLib(bean, id)) {
                                    advDownloadedCount++;
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (advDownloadedCount == advProgramBean.getMedia_lib().size()) {
                isAdvCompleted = true;
            } else {
                isAdvCompleted = false;
            }
        } else {
            isAdvCompleted = true;
        }

        // 记录日志
        if (isAdvCompleted) {
            // 记录下载完成日志
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "end", "adv_down", advPeriod,
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(advDownloadedCount));
        } else {
            // 记录下载中止日志
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "suspend", "adv_down", advPeriod,
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(advDownloadedCount));

        }

        if (isAdvCompleted && isProCompleted && !TextUtils.isEmpty(advProgramBean.getMenu_num()) &&
                advProgramBean.getMenu_num().equals(mProCompletedPeriod)) {
            isFirstRun = false;
            // 记录日志
            LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                    Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "update", advProgramBean.getVersion().getType(), "",
                    "", Session.get(context).getVersionName(), advProgramBean.getVersion().getVersion(),
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
            String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? AND " + DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? AND " +
                    DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? AND " + DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? ";
            String[] selectionArgs;
            selectionArgs = new String[]{session.getProPeriod(), session.getProNextPeriod(), session.getAdvPeriod(), session.getAdvNextPeriod()};
            dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);

            // 将下载表中的内容拷贝到播放表
            dbHelper.copyTableMethod(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, DBHelper.MediaDBInfo.TableName.PLAYLIST);

            // 如果是填充当前期（立即播放）的话，通知ADSPlayer播放
            if (fillCurrentBill) {
                notifyToPlay();
            }
        }

        if (!isAdvCompleted&&!isOSS){
            if (adv_timeout_count<5){
                adv_timeout_count ++;
//                getAdvDataFromSmallPlatform(isOSS);
                handleSmallPlatformAdvData(smallType,isOSS);
            }else {
                adv_timeout_count = 0;
//                getAdvDataFromSmallPlatform(!isOSS);
                handleSmallPlatformAdvData(smallType,!isOSS);
            }
        }


    }

    /**
     * 通过小平台获取广告数据
     */
    private void handleSmallPlatformAdsData(String smallType,boolean OSSsource) {
        if (adsProgramBean == null
                || adsProgramBean.getVersion() == null
                || TextUtils.isEmpty(adsProgramBean.getVersion().getVersion())
                || adsProgramBean.getMedia_lib()==null
                || adsProgramBean.getMedia_lib().size()==0) {
            return;
        }
        String adsPeriod = adsProgramBean.getVersion().getVersion();
        if (!isAdsFirstRun && session.getAdsPeriod().equals(adsPeriod)) {
            return;
        }

        // 清空ads下载表
        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWADSLIST, null, null);

        String logUUID = String.valueOf(System.currentTimeMillis());
        // 记录下载开始日志
        int count = adsProgramBean.getMedia_lib() == null ? 0 : adsProgramBean.getMedia_lib().size();
        LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                String.valueOf(System.currentTimeMillis()), "start", "ads_down", adsPeriod,
                "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(count));

        session.setAdsDownloadPeriod(adsPeriod);
        boolean isOSS = OSSsource;
        boolean isAdsCompleted = false;
        int adsDownloadedCount = 0;
        if (adsProgramBean.getMedia_lib() != null && adsProgramBean.getMedia_lib().size() > 0) {
            ServerInfo serverInfo = session.getServerInfo();
            if (serverInfo == null) {
                return;
            }
            String baseUrl = serverInfo.getDownloadUrl();
            if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            for (MediaLibBean bean : adsProgramBean.getMedia_lib()) {
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + bean.getName();
                String url = baseUrl + bean.getUrl();
                boolean isChecked = false;
                try {
                    // 下载、校验
                    LogUtils.v("****开始下载ads视频:"+bean.getChinese_name()+"****");
                    if (isDownloadCompleted(path, bean.getMd5())) {
                        isChecked = true;
                    } else {
                        boolean isDownloaded = false;
                        //虚拟小平台
                        if (ConstantValues.VIRTUAL.equals(smallType)||isOSS){
                            OSSUtils ossUtils = new OSSUtils(context,
                                    BuildConfig.OSS_BUCKET_NAME,
                                    bean.getOss_path(),
                                    new File(path));
                            isDownloaded = ossUtils.syncDownload();
                        }else{
                            isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                        }
                        if (isDownloaded && isDownloadCompleted(path, bean.getMd5())) {
                            isChecked = true;
                        }
                    }
                    LogUtils.v("****ads视频:"+bean.getChinese_name()+"下载完成****");
                    if (isChecked) {
                        bean.setMediaPath(path);
                        // 插库成功，mDownloadedList中加入一条
                        if (dbHelper.insertOrUpdateNewAdsList(bean, -1)) {
                            adsDownloadedCount++;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (adsDownloadedCount == adsProgramBean.getMedia_lib().size()) {
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
            // 从ADS下载表拷贝到正式表
            dbHelper.copyTableMethod(DBHelper.MediaDBInfo.TableName.NEWADSLIST, DBHelper.MediaDBInfo.TableName.ADSLIST);
            // 记录日志
            // 记录下载完成日志
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "end", "ads_down", adsPeriod,
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(adsDownloadedCount));
            LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                    Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "update", adsProgramBean.getVersion().getType(), "",
                    "", Session.get(context).getVersionName(), adsProgramBean.getVersion().getVersion(),
                    Session.get(context).getVodPeriod(), "");

            notifyToPlay();
        } else {
            // 记录下载中止日志
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "suspend", "ads_down", adsPeriod,
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(adsDownloadedCount));
            if (!isOSS){
                if (ads_timeout_count <5){
                    ads_timeout_count ++;
//                    getAdsDataFromSmallPlatform(isOSS);
                    handleSmallPlatformAdsData(smallType,isOSS);
                }else{
                    ads_timeout_count = 0;
//                    getAdsDataFromSmallPlatform(!isOSS);
                    handleSmallPlatformAdsData(smallType,!isOSS);
                }
            }

        }
    }

    private void notifyToPlay() {
        if (AppUtils.fillPlaylist(this, null, 1)) {
            LogUtils.d("发送通知更新播放列表广播");
            sendBroadcast(new Intent(ConstantValues.UPDATE_PLAYLIST_ACTION));
        }
    }

    /**
     * 初始化盒子基本信息
     *
     * @param boiteBean
     */
    void initBoxInfo(BoxInitBean boiteBean,String smallType) {
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
            }
            if (boiteBean.getProject_volume() > 0) {
                session.setProjectVolume(boiteBean.getProject_volume());
            }
            if (boiteBean.getDemand_volume() > 0) {
                session.setVodVolume(boiteBean.getDemand_volume());
            }
            if (boiteBean.getTv_volume() > 0) {
                session.setTvVolume(boiteBean.getTv_volume());
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
        if (!TextUtils.isEmpty(boiteBean.getArea_id())) {
            session.setOssAreaId(boiteBean.getArea_id());
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
        try{
            if (boiteBean.getTv_list()!=null&&boiteBean.getTv_list().size()>0){
                Television tv = boiteBean.getTv_list().get(0);
                session.setTvSize(Integer.valueOf(tv.getTv_size()));
            }
        }catch (Exception e){
            e.printStackTrace();
        }


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
                String[] split = boiteBean.getLogo_url().split("/");
                String logo_name = split[split.length - 1];
                logo_md5 = boiteBean.getLogo_md5();
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.cache) + logo_name;
                if (ConstantValues.VIRTUAL.equals(smallType)){
                    OSSUtils ossUtils = new OSSUtils(context,
                            BuildConfig.OSS_BUCKET_NAME,
                            boiteBean.getLogo_url(),
                            new File(path));
                            ossUtils.syncDownload();
                }else{
                    ServerInfo serverInfo = session.getServerInfo();
                    if (serverInfo != null) {
                        String baseUrl = serverInfo.getDownloadUrl();
                        String url = baseUrl + boiteBean.getLogo_url();
                        if (!TextUtils.isEmpty(boiteBean.getLogo_url())) {
                            File tarFile = new File(path);
                            if (tarFile.exists()) {
                                tarFile.delete();
                            }
                            AppApi.downloadLOGO(url, context, this, path);
                        }
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
                String[] split = boiteBean.getLoading_img_url().split("/");
                String imageName = split[split.length - 1];
                loading_img_md5 = boiteBean.getLoading_img_md5();
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.cache) + imageName;
                if (ConstantValues.VIRTUAL.equals(smallType)){
                    OSSUtils ossUtils = new OSSUtils(context,
                            BuildConfig.OSS_BUCKET_NAME,
                            boiteBean.getLoading_img_url(),
                            new File(path));
                    ossUtils.syncDownload();
                }else{
                    ServerInfo serverInfo = session.getServerInfo();
                    if (serverInfo != null) {
                        String baseUrl = serverInfo.getDownloadUrl();
                        String url = baseUrl + boiteBean.getLoading_img_url();
                        if (!TextUtils.isEmpty(boiteBean.getLoading_img_url())) {
                            File tarFile = new File(path);
                            if (tarFile.exists()) {
                                tarFile.delete();
                            }
                            AppApi.downloadImg(url, context, this, path);
                        }
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
            String realMd5 = AppUtils.getEasyMd5(new File(path));
            if (!TextUtils.isEmpty(md5) && md5.equals(realMd5)) {
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * 图片是否下载完成判定
     *
     * @param path
     * @param md5
     * @return
     * @throws IOException
     */
    private boolean isImageDownloadCompleted(String path, String md5) throws Exception {
        if (AppUtils.isFileExist(path)) {
            String realMd5 = AppUtils.getMD5(FileUtils.readByte(path));
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
    private void handleSmallPlatformOnDemandData(String smallType,boolean OSSsource) {
        if (multicastBoxBean == null
                || multicastBoxBean.getPlaybill_list() == null
                || multicastBoxBean.getPlaybill_list().isEmpty()
                || multicastBoxBean.getPlaybill_list()==null
                || multicastBoxBean.getPlaybill_list().size()==0) {
            return;
        }
        ServerInfo serverInfo = session.getServerInfo();
        if (serverInfo == null) {
            return;
        }
        boolean isOSS = OSSsource;
        String baseUrl = serverInfo.getDownloadUrl();
        if (!TextUtils.isEmpty(baseUrl) && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        boolean isAllCompleted = true;      // 标识是否所有类型的视频都下载完成
        ArrayList<ProgramBean> playbill_list = multicastBoxBean.getPlaybill_list();
        ArrayList<String> fileNames = new ArrayList<>();    // 下载成功的文件名集合（后面删除老视频会用到）

        // 抽出版本信息
        ArrayList<VersionInfo> newVersions = new ArrayList<>();
        StringBuilder versionSequence = new StringBuilder();
        for (int i = 0; i < playbill_list.size(); i++) {
            ProgramBean item = playbill_list.get(i);
            newVersions.add(item.getVersion());
            if (i != 0) {
                versionSequence.append("_");
            }
            versionSequence.append(item.getVersion().getVersion());
        }
        String versionStr = versionSequence.toString();
        session.setDownloadingVodVersion(newVersions);

        for (int i = 0; i < playbill_list.size(); i++) {
            ProgramBean item = playbill_list.get(i);
            VersionInfo versionInfo = item.getVersion();
            if (versionInfo == null || TextUtils.isEmpty(versionInfo.getType())) {
                continue;
            }

            String logUUID = String.valueOf(System.currentTimeMillis());
            // 记录下载开始日志
            int count = item.getMedia_lib() == null ? 0 : item.getMedia_lib().size();
            LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "start", "vod_down", versionInfo.getVersion(),
                    "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(count));

            List<MediaLibBean> mediaLibList = item.getMedia_lib();
            int completedCount = 0;     // 下载成功个数
            if (mediaLibList != null && mediaLibList.size() > 0) {

                LogUtils.d("---------点播视频开始下载---------");
                for (MediaLibBean mediaLib : mediaLibList) {
                    String url = baseUrl + mediaLib.getUrl();
                    String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast) + mediaLib.getName();
                    String path2 = AppUtils.getFilePath(context,AppUtils.StorageFile.media)+mediaLib.getName();
                    fileNames.add(mediaLib.getName());
                    try {
                        boolean isChecked = false;
                        if (isDownloadCompleted(path, mediaLib.getMd5())||isDownloadCompleted(path2,mediaLib.getName())) {
                            isChecked = true;
                        } else {
                            File file = new File(path);
                            if (file.exists() && file.isFile()) {
                                file.delete();
                            }
                            boolean isDownloaded = false;
                            //虚拟小平台
                            if (ConstantValues.VIRTUAL.equals(smallType)||isOSS){
                                OSSUtils ossUtils = new OSSUtils(context,
                                        BuildConfig.OSS_BUCKET_NAME,
                                        mediaLib.getOss_path(),
                                        new File(path));
                                isDownloaded = ossUtils.syncDownload();
                            }else{
                                isDownloaded = new ProgressDownloader(url, new File(path)).download(0);
                            }
                            if (isDownloaded && isDownloadCompleted(path, mediaLib.getMd5())) {
                                isChecked = true;
                            }
                        }
                        if (isChecked) {
                            // 入库
                            String selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=? ";
                            String[] selectionArgs = new String[]{mediaLib.getName()};
                            List<MediaLibBean> list = dbHelper.findMutlicastMediaLibByWhere(selection, selectionArgs);
                            boolean isInsertSuccess = false;
                            if (list != null && list.size() > 1) {
                                dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB, selection, selectionArgs);
                                isInsertSuccess = dbHelper.insertOrUpdateMulticastLib(mediaLib, false);
                            } else if (list != null && list.size() == 1) {
                                isInsertSuccess = dbHelper.insertOrUpdateMulticastLib(mediaLib, true);
                            } else {
                                isInsertSuccess = dbHelper.insertOrUpdateMulticastLib(mediaLib, false);
                            }

                            if (isInsertSuccess) {
                                completedCount++;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (mediaLibList == null || completedCount == mediaLibList.size()) {

                // 记录日志
                // 记录下载完成日志
                LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "end", "vod_down", versionInfo.getVersion(),
                        "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(completedCount));
                LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                        Session.get(context).getBoiteId(), Session.get(context).getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "update", versionInfo.getType(), "",
                        "", Session.get(context).getVersionName(), Session.get(context).getAdsPeriod(),
                        Session.get(context).getVodPeriod(), "");
            } else {
                isAllCompleted = false;
                // 记录下载中止日志
                LogReportUtil.get(this).sendAdsLog(logUUID, session.getBoiteId(), session.getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "suspend", "vod_down", versionInfo.getVersion(),
                        "", session.getVersionName(), session.getAdsPeriod(), session.getVodPeriod(), String.valueOf(completedCount));
            }
        }

        if (isAllCompleted) {
            LogUtils.d("---------点播视频下载完成---------");

            session.setVodVersion(newVersions);
            session.setMulticastMediaPeriod(versionStr);

            deleteMediaFileNotInConfig(fileNames, AppUtils.StorageFile.multicast, DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB);
        }else{
            if (!isOSS){
                if (vod_timeout_count<5){
                    vod_timeout_count ++;
                    getOnDemandDataFromSmallPlatform(isOSS);
                }else {
                    vod_timeout_count =0;
                    getOnDemandDataFromSmallPlatform(!isOSS);
                }
            }
        }
    }

    /**
     * 删除没有在小平台配置文件内的点播文件和点播数据库记录
     *
     * @param arrayList
     * @param storage
     */
    private void deleteMediaFileNotInConfig(List<String> arrayList, AppUtils.StorageFile storage, String tableName) {
        File[] listFiles = new File(AppUtils.getFilePath(context, storage)).listFiles();
        for (File file : listFiles) {
            if (file.isFile()) {
                String oldName = file.getName();
                if (!arrayList.contains(oldName)) {
                    if (file.delete()) {
                        String selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=? ";
                        String[] selectionArgs = new String[]{oldName};
//                        if (!TextUtils.isEmpty(type)){
//                            selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=? and "
//                                    + DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=? ";
//                            selectionArgs = new String[]{oldName,type};
//                        }else{
//                            selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=? ";
//                            selectionArgs = new String[]{oldName};
//                        }

                        dbHelper.deleteDataByWhere(tableName, selection, selectionArgs);
                    }
                }
            } else {
                FileUtils.deleteFile(file);
            }
        }
    }


    @Override
    public void onError(AppApi.Action method, Object obj) {
        switch (method) {
            case CP_POST_SDCARD_STATE_JSON:
                LogUtils.d("上报SD卡状态失败");
                break;
        }
    }

    @Override
    public void onNetworkFailed(AppApi.Action method) {
        switch (method) {
            case CP_POST_SDCARD_STATE_JSON:
                LogUtils.d("上报SD卡状态失败，网络异常");
                break;
        }
    }
}
