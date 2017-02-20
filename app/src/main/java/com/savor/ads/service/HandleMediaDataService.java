package com.savor.ads.service;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.activity.AdsPlayerActivity;
import com.savor.ads.activity.MainActivity;
import com.savor.ads.bean.BoiteBean;
import com.savor.ads.bean.BoxInitBean;
import com.savor.ads.bean.BoxInitResult;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.OnDemandBean;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.bean.RoomBean;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.bean.SetBoxTopResult;
import com.savor.ads.bean.SetTopBoxBean;
import com.savor.ads.bean.TvProgramResponse;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.log.LogReportUtil;
import com.savor.ads.okhttp.coreProgress.download.ProgressDownloader;
import com.savor.ads.oss.OSSValues;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.FileDownProgress;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.ShellUtils;
import com.savor.ads.utils.TechnicalLogReporter;
import com.savor.ads.utils.UpdateUtil;
import com.savor.ads.utils.tv.TvOperate;
import com.tvos.common.TvManager;
import com.tvos.common.exception.TvCommonException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/**
 * 处理下载媒体文件逻辑的服务
 * Created by bichao on 2016/12/10.
 */

public class HandleMediaDataService extends Service implements ApiRequestListener{

    private Context context;
    private Session session;
    private String logo_md5=null;
    //接口返回的广告数据
    private SetTopBoxBean setTopBoxBean;
    //接口返回的点播数据
    private SetTopBoxBean mulitcasrtBoxBean;

    private DBHelper dbHelper ;
    List<PlayListBean> mAdsList = new ArrayList<>();
    List<String> mDemandList = new ArrayList<>();
    private boolean isRun = true;
    private DownloadManager downloadManager=null;
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    /**
     * 每次第一次开机运行的时候都检测一遍本地文件文件，如果损毁就重新下载
     */
    private boolean isFirstRun=true;
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

                while (isRun) {
                    do{
                        LogFileUtil.write("HandleMediaDataService will check server info and period");
                        LogUtils.d("===============AdvertMediaPeriod==========="+session.getAdvertMediaPeriod());
                        if (!TextUtils.isEmpty(AppUtils.getExternalSDCardPath())
                                &&AppUtils.isNetworkAvailable(context)){
                            ServerInfo serverInfo = session.getServerInfo();
                            LogUtils.d("===============serverInfo==========="+serverInfo);
                            if (serverInfo!=null) {
                                break;
                            }
                        }
                        try {
                            Thread.sleep(1000*2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }while (true);

                    LogFileUtil.write("HandleMediaDataService will start UpdateUtil");
                    new UpdateUtil(context);
                    LogFileUtil.write("HandleMediaDataService will start getBoxInfo");
                    getBoxInfo();
                    LogFileUtil.write("HandleMediaDataService will start getAdvertDataFromSmallPlatfrom");
                    getAdvertDataFromSmallPlatfrom();
                    LogFileUtil.write("HandleMediaDataService will start getOnDemandDataFromSmallPlatfrom");
                    getOnDemandDataFromSmallPlatfrom();
//                    setAutoClose(true);
                    LogFileUtil.write("HandleMediaDataService will start getTVMatchDataFromSmallPlatfrom");
                    getTVMatchDataFromSmallPlatfrom();
                    try {
                        Thread.sleep(1000*60*10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 盒子下载之前删除旧的媒体文件
     */
    private void deleteOldMedia() {

        LogUtils.d("删除多余视频");
        //排除当前已经完整下载并且在播放的视频和正在下载的视频，其他数据删除
        String completeMediaPeriod = session.getAdvertMediaPeriod();
        String downloadingMediaPeriod = session.getAdvertDownloadingPeriod();
        if (TextUtils.isEmpty(downloadingMediaPeriod)||TextUtils.isEmpty(completeMediaPeriod)){
            return;
        }
        //排除当前已经完整下载的文件夹和正在下载的文件夹，其他删除
        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media);
        File[] listFiles = new File(path).listFiles();
        if (listFiles==null||listFiles.length==0){
            return;
        }
        try {
            if (dbHelper.findPlayListByWhere(null, null)==null
                    &&dbHelper.findNewPlayListByWhere(null, null)==null){
                return;
            }
            for (File file : listFiles) {
                String selection=DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?";
                String[] selectionArgs = new String[]{file.getName()};

                //已经下载的完整的一期的数据
//                list = dbHelper.findPlayListByWhere(selection, selectionArgs);
//                if (list==null){
//                    list = dbHelper.findNewPlayListByWhere(selection, selectionArgs);
//                }
                if (dbHelper.findPlayListByWhere(selection, selectionArgs)==null
                        &&dbHelper.findNewPlayListByWhere(selection, selectionArgs)==null){
                    FileUtils.delDir(file);
                    LogUtils.d("删除文件==================="+file.getName());
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
            if (jsonObject.getInt("code") != AppApi.HTTP_RESPONSE_STATE_SUCCESS){
                LogUtils.d("接口返回的状态不对,code="+jsonObject.getInt("code"));
                return;
            }

            Object result = gson.fromJson(configJson, new TypeToken<BoxInitResult>() {
            }.getType());
            if (result instanceof BoxInitResult){
                BoxInitBean boxInitBean = ((BoxInitResult) result).getResult();
                /*******************设置盒子基本信息开始************************/
                initBoxInfo(boxInitBean);
                /*******************设置盒子基本信息结束************************/
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (JSONException e){

        }
    }

    /**
     * 获取小平台广告媒体文件
     */
    private void getAdvertDataFromSmallPlatfrom(){
        try {
            String configJson = AppApi.getAdvertDataFromSmallPlatform(this,this, session.getEthernetMac());
            JSONObject jsonObject = new JSONObject(configJson);
            if (jsonObject.getInt("code")!=AppApi.HTTP_RESPONSE_STATE_SUCCESS){
                LogUtils.d("接口返回的状态不对,code="+jsonObject.getInt("code"));
                return;
            }

            Object result = gson.fromJson(configJson, new TypeToken<SetBoxTopResult>() {
            }.getType());
            if (result instanceof SetBoxTopResult){
                SetBoxTopResult setBoxTopResult = (SetBoxTopResult) result;
                if (setBoxTopResult!=null&&setBoxTopResult.getResult()!=null){
                    setTopBoxBean = setBoxTopResult.getResult();
                    handleSmallPlatformAdvertData();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (JSONException e){

        }

    }
    private void getOnDemandDataFromSmallPlatfrom(){
        try {
            String ondemandJson = AppApi.getOnDemandDataFromSmallPlatform(this,this, session.getEthernetMac());
            try {
                JSONObject jsonObject = new JSONObject(ondemandJson);
                if (jsonObject.getInt("code")!=AppApi.HTTP_RESPONSE_STATE_SUCCESS){
//                    Looper.prepare();
//                    ShowMessage.showToast(context,jsonObject.get("msg").toString());
                    return;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            Object result = gson.fromJson(ondemandJson, new TypeToken<SetBoxTopResult>() {
            }.getType());
            if (result instanceof SetBoxTopResult){
                SetBoxTopResult setBoxTopResult = (SetBoxTopResult) result;
                if (setBoxTopResult!=null&&setBoxTopResult.getResult()!=null){
                    mulitcasrtBoxBean = setBoxTopResult.getResult();
                    handleSmallPlatformOnDemandData();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void getTVMatchDataFromSmallPlatfrom(){
        AppApi.getTVMatchDataFromSmallPlatform(this,this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSuccess(AppApi.Action method, Object obj) {
        switch (method){
            case SP_GET_TV_MATCH_DATA_FROM_JSON:
                if (obj instanceof TvProgramResponse){
                    TvProgramResponse response = (TvProgramResponse)obj;
                    TvOperate mtv = new TvOperate();
                    mtv.updateProgram(context, response);
                }
                break;
            case SP_GET_LOGO_DOWN:
                if (obj instanceof FileDownProgress){
                    FileDownProgress fs = (FileDownProgress) obj;
                    long now = fs.getNow();
                    long total = fs.getTotal();

                }else if (obj instanceof File) {
                    File f = (File) obj;
                    byte[] fRead = new byte[0];
                    String md5Value=null;
                    try {
                        fRead = org.apache.commons.io.FileUtils.readFileToByteArray(f);
                        md5Value = AppUtils.getMD5(fRead);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //比较本地文件版本是否与服务器文件一致，如果一致则启动安装
                    if (md5Value!=null&&md5Value.equals(logo_md5)){
                        ShellUtils.updateLogoPic(f.getAbsolutePath());
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
    private void handleSmallPlatformAdvertData(){
        if (setTopBoxBean==null){
            return ;
        }
        String config = AppUtils.getFilePath(context, AppUtils.StorageFile.config);
        if (AppUtils.isDirNull(config)){
            //如果本地config.txt文件存在，就删除
            new File(config).delete();
        }
        //如果期刊数相同，则表示数据没有改变不需要在此执行
        LogUtils.d("===============AdvertMediaPeriod==========="+session.getAdvertMediaPeriod());
        if ((!TextUtils.isEmpty(session.getAdvertMediaPeriod()) &&(session.getAdvertMediaPeriod().equals(setTopBoxBean.getPeriod())) ||
                    (!TextUtils.isEmpty(session.getNextAdvertMediaPeriod()) && session.getNextAdvertMediaPeriod().equals(setTopBoxBean.getPeriod())))){
            if (!isFirstRun){
                if (AppUtils.checkPlayTime(context)){
                    notifyToPlay();
                }
                return;
            }else{
                deleteOldMedia();
            }
        }

        /*******************更新媒体库并下载开始********************************/
        List<MediaLibBean> mediaLibList = setTopBoxBean.getMedia_lib();
        String selection=DBHelper.MediaDBInfo.FieldName.PERIOD + "=?";
        String[] selectionArgs = new String[]{setTopBoxBean.getPeriod()};

//        dbHelper.deleteAllData(DBHelper.MediaDBInfo.TableName.MEDIALIB);
//        dbHelper.deleteAllData(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST);
        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,selection,selectionArgs);
        if (mediaLibList!=null&&mediaLibList.size()>0){
            session.setAdvertDownloadingPeriod(setTopBoxBean.getPeriod());
            mAdsList.clear();
            ServerInfo serverInfo = session.getServerInfo();
            String baseUrl = serverInfo.getDownloadUrl();
            if (!TextUtils.isEmpty(baseUrl)&&baseUrl.endsWith("/")){
                baseUrl = baseUrl.substring(0,baseUrl.length()-1);
            }
            LogUtils.d("---------广告视频开始下载---------");
//            List<String> list = new ArrayList<>();
            for(MediaLibBean mediaLib:mediaLibList){
//                list.add(mediaLib.getName());
                String url  =baseUrl+mediaLib.getUrl();
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + mediaLib.getName();
//                dbHelper.insertMediaLib(mediaLib);
                try {
                    boolean downloaded = false;
                    if(isDownloadCompleted(path,mediaLib.getMd5())){
                        downloaded = true;
                    }else{
                        boolean isDownloaded = new ProgressDownloader(url,new File(path)).download(0);
                        if (isDownloaded&&isDownloadCompleted(path,mediaLib.getMd5())){
                            downloaded = true;
                        }
                    }
                    if (downloaded){
                        PlayListBean bean = new PlayListBean();
                        bean.setPeriod(mediaLib.getPeriod());
                        bean.setDuration(mediaLib.getDuration());
                        bean.setMd5(mediaLib.getMd5());
                        bean.setVid(mediaLib.getVid());
                        bean.setMedia_name(mediaLib.getName());
                        bean.setMedia_type(mediaLib.getType());
                        bean.setOrder(mediaLib.getOrder());
                        bean.setSurfix(mediaLib.getSurfix());
                        bean.setMediaPath(path);
                        int id = -1;
//                        selection=DBHelper.MediaDBInfo.FieldName.PERIOD + "=? and "
//                                + DBHelper.MediaDBInfo.FieldName.VID+"=? and "
//                                + DBHelper.MediaDBInfo.FieldName.ADS_ORDER +"=? ";
//                        selectionArgs = new String[]{mediaLib.getPeriod(),mediaLib.getVid(),String.valueOf(mediaLib.getOrder())};
//                        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MEDIALIB,selection,selectionArgs);
//                        if (playList!=null){
//                            if (playList.size()==1){
//                                id = playList.get(0).getId();
//                            }else{
//                                dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,selection,selectionArgs);
//                            }
//                        }
                        if (dbHelper.insertOrUpdatePlayListLib(bean,id)){
                            mAdsList.add(bean);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (mAdsList.size()==mediaLibList.size()){
                isFirstRun = false;
                //如果为空则是第一次下载完成，将当前广告期号保存session
                if (TextUtils.isEmpty(session.getAdvertMediaPeriod())){
                    session.setAdvertMediaPeriod(setTopBoxBean.getPeriod());
                }else if (!session.getAdvertMediaPeriod().equals(setTopBoxBean.getPeriod())){
                    //如果不为空并且与本地期号不相同，证明是新的一期下载完成，将期号保存在下期期号session
                    session.setNextAdvertMediaPeriod(setTopBoxBean.getPeriod());
                    session.setNextAdvertMediaPubTime(setTopBoxBean.getPub_time());
                }
                LogUtils.d("---------广告视频当前期号---------"+setTopBoxBean.getPeriod());
                LogUtils.d("---------广告视频下载完成---------");
                // 上报日志广告更新完毕
                TechnicalLogReporter.adUpdate(this, setTopBoxBean.getPeriod());
                if (!TextUtils.isEmpty(session.getAdvertMediaPeriod())
                        &&!TextUtils.isEmpty(session.getNextAdvertMediaPeriod())){
                    selection=DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? and "
                            + DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? ";
                    selectionArgs = new String[]{session.getAdvertMediaPeriod(),session.getNextAdvertMediaPeriod()};
                }else if(!TextUtils.isEmpty(session.getAdvertMediaPeriod())){
                    selection=DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? ";
                    selectionArgs = new String[]{session.getAdvertMediaPeriod()};
                }
                dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,selection,selectionArgs);
                dbHelper.copyPlaylist(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,DBHelper.MediaDBInfo.TableName.PLAYLIST);
                if (TextUtils.isEmpty(session.getNextAdvertMediaPubTime())){
                    notifyToPlay();
                }
            }

        }

        /*******************更新媒体库并下载结束********************************/

    }

    private void notifyToPlay() {
        fillPlayList();
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof MainActivity) {
            LogFileUtil.write("广告下载完成, will goto AdsPlayerActivity");
            Intent intent = new Intent(activity, AdsPlayerActivity.class);
            activity.startActivity(intent);
        } else {
            LogUtils.d("发送广告下载完成广播");
            sendBroadcast(new Intent(ConstantValues.ADS_DOWNLOAD_COMPLETE_ACCTION));
        }
    }


    private void fillPlayList() {
        LogUtils.d("开始fillPlayList");
        if (!TextUtils.isEmpty(Session.get(this).getAdvertMediaPeriod())) {
            ArrayList<PlayListBean> playList = dbHelper.getOrderedPlayList(Session.get(this).getAdvertMediaPeriod());

            if (playList != null) {
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
                                DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " + DBHelper.MediaDBInfo.FieldName.VID + "=?",
                                new String[]{bean.getPeriod(), bean.getVid()});

                        if (mediaFile.exists()) {
                            mediaFile.delete();
                        }

                        TechnicalLogReporter.md5Failed(this, bean.getVid());
                    }

                }
            }

            dbHelper.close();
            ConstantValues.PLAY_LIST = playList;
        }
    }

    /**
     * 初始化盒子基本信息
     * @param boiteBean
     */
    void initBoxInfo(BoxInitBean boiteBean) {
        if (boiteBean == null){
            return;
        }
        if (!isProduceLog||boiteBean.getSwitch_time()!=session.getSwitchTime()){
            //生产电视播放音量日志
            LogReportUtil.get(context).sendAdsLog(String.valueOf(System.currentTimeMillis()),
                    session.getBoiteId(),
                    session.getRoomId(),
                    String.valueOf(System.currentTimeMillis()),
                    "player_volume",
                    "system",
                    "",
                    "",
                    session.getVersionName(),
                    session.getAdvertMediaPeriod(),
                    session.getMulticastMediaPeriod(),
                    String.valueOf(boiteBean.getSwitch_time()));
        }

        if (boiteBean.getSwitch_time() > 0) {
            session.setSwitchTime(boiteBean.getSwitch_time());
        }
        if (!isProduceLog||boiteBean.getVolume()!=session.getVolume()){
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
                    session.getAdvertMediaPeriod(),
                    session.getMulticastMediaPeriod(),
                    String.valueOf(boiteBean.getVolume()));
        }

        if (boiteBean.getVolume() > 0) {
            session.setVolume(boiteBean.getVolume());
        }
        isProduceLog = true;
//        if (!TextUtils.isEmpty(session.getBoiteId()) &&
//                !session.getBoiteId().equals(boiteBean.getHotel_id())){
//            deleteDataByChangeBoite();
//        }
        session.setBoxId(session.getMacAddress());
        session.setBoiteId(boiteBean.getHotel_id());
        session.setBoiteName(boiteBean.getHotel_name());

        session.setRoomId(boiteBean.getRoom_id());
        session.setRoomName(boiteBean.getRoom_name());
        session.setBoxName(boiteBean.getBox_name());
        /**桶名称*/
        if (!TextUtils.isEmpty(boiteBean.getOssBucketName())){
            session.setOss_bucket(boiteBean.getOssBucketName());
        }
        /**桶地址*/
        if (!TextUtils.isEmpty(boiteBean.getAreaId())){
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            String datestr = format.format(new Date());

            session.setOss_file_path(OSSValues.uploadFilePath+boiteBean.getAreaId()+File.separator+datestr+File.separator);
        }
        /**下载启动图*/
        if (!TextUtils.isEmpty(boiteBean.getLogo_url())){
            ServerInfo serverInfo = session.getServerInfo();
            if (serverInfo != null) {
                String baseUrl = serverInfo.getDownloadUrl();
                String url =baseUrl+boiteBean.getLogo_url();
                if (!TextUtils.isEmpty(boiteBean.getLogo_url())){
                    String[] split = boiteBean.getLogo_url().split("/");
                    String logo_name = split[split.length-1];
                    logo_md5 = boiteBean.getLogo_md5();
                    File tarFile =new File(AppUtils.getFilePath(context, AppUtils.StorageFile.cache));
                    if(tarFile.exists()){
                        com.savor.ads.utils.FileUtils.delDir(tarFile);
                    }
                    String path = AppUtils.getFilePath(context, AppUtils.StorageFile.cache)+ logo_name;
                    AppApi.downloadLOGO(url,context,this,path);
                }
            }

        }
    }

    /**
     * 如果发现酒楼ID发生改变，那么证明盒子换酒楼了，需要把视频和数据清空重新下载
     */
    private void deleteDataByChangeBoite(){
        //排除当前已经完整下载的文件夹和正在下载的文件夹，其他删除
        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media);
        File[] listFiles = new File(path).listFiles();
        if (listFiles==null||listFiles.length==0){
            return;
        }
        try {
            List<PlayListBean> list= null;
            for (File file:listFiles){
                String selection=DBHelper.MediaDBInfo.FieldName.MEDIANAME
                        + "=? and "
                        + DBHelper.MediaDBInfo.FieldName.PERIOD
                        + "=? ";
                String[] selectionArgs = null;
                if (!TextUtils.isEmpty(session.getAdvertMediaPeriod())){
                    selectionArgs = new String[]{file.getName(),session.getAdvertMediaPeriod()};
                    list = dbHelper.findPlayListByWhere(selection, selectionArgs);
                    if (list!=null&&list.size()>0){
                        FileUtils.delDir(file);
                        LogUtils.d("删除文件==================="+file.getName());
                    }
                    continue;
                }else if(!TextUtils.isEmpty(session.getAdvertDownloadingPeriod())){
                    selectionArgs = new String[]{file.getName(),session.getAdvertDownloadingPeriod()};
                    list = dbHelper.findNewPlayListByWhere(selection, selectionArgs);
                    if (list!=null&&list.size()>0){
                        FileUtils.delDir(file);
                        LogUtils.d("删除文件==================="+file.getName());
                    }
                    continue;
                }else if (!TextUtils.isEmpty(session.getNextAdvertMediaPeriod())){
                    selectionArgs = new String[]{file.getName(),session.getNextAdvertMediaPeriod()};
                    list = dbHelper.findPlayListByWhere(selection, selectionArgs);
                    if (list!=null&&list.size()>0){
                        FileUtils.delDir(file);
                        LogUtils.d("删除文件==================="+file.getName());
                    }
                    continue;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        session.setAdvertMediaPeriod(null);
        session.setAdvertDownloadingPeriod(null);
        session.setMulticastMediaPeriod(null);
        session.setMulticastDownloadingPeriod(null);
        dbHelper.deleteAllData(DBHelper.MediaDBInfo.TableName.PLAYLIST);
    }

    /**
     * 创建广告下载任务
     * @param mediaLib
     * @param period
     */
    private void createDownloadTask(MediaLibBean mediaLib,String period,String path){

        //将下载任务添加到下载表
        String selection=DBHelper.MediaDBInfo.FieldName.PERIOD + "=? and "
                + DBHelper.MediaDBInfo.FieldName.VID+"=?";
        String[] selectionArgs = new String[]{mediaLib.getPeriod(),mediaLib.getVid()};
        List<MediaLibBean> list = dbHelper.findMediaLib(selection,selectionArgs);
        if (list!=null&&list.size()>0){
            //如果下载任务已存在，既不建立下载任务，只插入下载表数据
            mediaLib.setTaskId(list.get(0).getTaskId());
        }else {
            //建立下载任务
            File f = new File(path);
            long taskId = getTaskId("url",f);
            mediaLib.setTaskId(String.valueOf(taskId));
        }
        dbHelper.insertMediaLib(mediaLib);

    }

    /**
     * 获取下载的taskId
     * @param url
     * @param f
     * @return
     */
    private long getTaskId(String url,File f){
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("url"));
        //在通知栏中显示，默认就是显示的
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setVisibleInDownloadsUi(false);
        request.setDestinationUri(Uri.fromFile(f));
        //将下载任务加入下载队列，否则不会进行下载
        long taskId = downloadManager.enqueue(request);
        return taskId;
    }

    /**
     * 监听广告下载
     * @param period
     */
    boolean advertThreadRunning = false;
    private void monitorDownloadThread(final String period){
        if (!advertThreadRunning){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    advertThreadRunning = true;
                    while (true){
                        String selection="";
                        String[] selectionArgs = new String[]{};
                        List<MediaLibBean> list = dbHelper.findMediaLib(selection,selectionArgs);
                        if (list==null||list.size()==0){
                            session.setAdvertMediaPeriod(period);
                            break;
                        }
                        if (list!=null&&list.size()>0){
                            for (MediaLibBean lib:list){
                                boolean isCompleted = queryDownloadStatus(lib.getTaskId());
                                try {
                                    if (isCompleted){
                                        String mVid = lib.getVid();
                                        String surfix = lib.getSurfix();
                                        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media)
                                                + mVid + "."
                                                + surfix;
                                        if (isDownloadCompleted(path,lib.getMd5())){
                                            selection = "";
                                            selectionArgs = new String[]{lib.getTaskId()};
                                            List<MediaLibBean> sameTaskIdList = dbHelper.findMediaLib(selection,selectionArgs);
                                            boolean flag = dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MEDIALIB,selection,selectionArgs);
                                            if (flag){
                                                for (MediaLibBean bean:sameTaskIdList){
                                                    PlayListBean play = new PlayListBean();
                                                    play.setPeriod(bean.getPeriod());
                                                    play.setDuration(bean.getDuration());
                                                    play.setMd5(bean.getMd5());
                                                    play.setVid(bean.getVid());
                                                    play.setMedia_name(bean.getName());
                                                    play.setOrder(bean.getOrder());
                                                    play.setSurfix(bean.getSurfix());
                                                    play.setMediaPath(path);
//                                                    dbHelper.insertPlayListLib(play);
                                                }

                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        try {
                            Thread.sleep(60*1000*2);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    advertThreadRunning = false;
                }
            }).start();
        }


    }

    /**
     * 查询下载状态
     * @param taskId
     * @return
     */
    private boolean queryDownloadStatus(String taskId){
        if (TextUtils.isEmpty(taskId)){
            return false;
        }
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(Long.valueOf(taskId));
        Cursor cursor = downloadManager.query(query);
        if(cursor!=null&&cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status==DownloadManager.STATUS_SUCCESSFUL){
                return true;
            }

        }
        return false;
    }

    /**
     * 文件是否下载完成判定
     * @param path
     * @param md5
     * @return
     * @throws IOException
     */
    private boolean isDownloadCompleted(String path, String md5) throws Exception {
        if (AppUtils.isFileExist(path)) {
            String realMd5 = AppUtils.getMD5Method(new File(path));
            if (!TextUtils.isEmpty(md5)&&md5.equals(realMd5)) {
                return true;
            }
            return false;
        } else {
            return false;
        }
    }
    //本期视频是否下载完成
    @Deprecated
    protected boolean isCurrentPeriodDownloadCompleted(String period){
        boolean flag = false;
        String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "=? ";
        String[] selectionArgs = new String[]{session.getAdvertMediaPeriod()};
        List<PlayListBean> list = dbHelper.findPlayListByWhere(selection,selectionArgs);
        if (list!=null&&list.size()==setTopBoxBean.getMedia_lib().size()){
            //本期所有视频全部下载完成
            flag = true;
            session.setAdvertMediaPeriod(period);
        }
        return flag;
    }

    /**
     * 处理小平台返回的点播视频
     */
    private void handleSmallPlatformOnDemandData(){
        if (mulitcasrtBoxBean==null||mulitcasrtBoxBean.getMedia_lib().size()==0){
//            setAutoClose(true);
            return;
        }

        String period = mulitcasrtBoxBean.getPeriod();
        List<String> list = new ArrayList<>();

        dbHelper.deleteAllData(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB);
        session.setMulticastDownloadingPeriod(mulitcasrtBoxBean.getPeriod());
        LogUtils.d("---------点播视频开始下载---------");
        mDemandList.clear();
        for (MediaLibBean bean:mulitcasrtBoxBean.getMedia_lib()){
            String videoName = bean.getName();
            list.add(videoName);
            onDemandDownloadMethod(bean);

        }
        if (list.size()==mDemandList.size()){
            // 上报点播视频下载完毕日志
            TechnicalLogReporter.vodUpdate(this, period);

            session.setMulticastMediaPeriod(mulitcasrtBoxBean.getPeriod());
            LogUtils.d("--------当前点播视频期号---------"+mulitcasrtBoxBean.getPeriod());
            LogUtils.d("---------点播视频下载完成---------");
        }
        deleteMediaFileNotInConfig(list, AppUtils.StorageFile.multicast);
//        setAutoClose(true);
    }



    private void onDemandDownloadMethod(final MediaLibBean bean){


        String p = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast);
        String path = p+bean.getName();
        ServerInfo serverInfo = session.getServerInfo();
        String baseUrl = "";
        if (serverInfo!=null&&!TextUtils.isEmpty(serverInfo.getDownloadUrl())){
            baseUrl = serverInfo.getDownloadUrl();
            if (baseUrl.endsWith("/")){
                baseUrl = baseUrl.substring(0,baseUrl.length()-1);
            }
        }else{
            return;
        }
        String url = baseUrl+ bean.getUrl();
        try {
            boolean downloaded = false;
            if(isDownloadCompleted(path,bean.getMd5())){
                downloaded = true;
            }else{
               File file = new File(path);
                if (file.exists()&&file.isFile()){
                    file.delete();
                }
                boolean isDownloaded = new ProgressDownloader(url,new File(path)).download(0);
                if (isDownloaded&&isDownloadCompleted(path,bean.getMd5())){
                    downloaded = true;
                }
            }
            if (downloaded){
                mDemandList.add(bean.getName());
                dbHelper.insertmulticastLib(bean);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 创建点播广告下载任务
     * @param bean
     * @param path
     */
    private void createMulticastDownloadTask(OnDemandBean bean,String path){
        File f = new File(path);
        long taskId = getTaskId("url",f);
        bean.setTaskId(String.valueOf(taskId));
//        dbHelper.insertmulticastLib(bean);
    }

    /**
     * 删除没有在小平台配置文件内的视频文件
     * @param arrayList
     * @param storage
     */
    private void deleteMediaFileNotInConfig(List<String> arrayList, AppUtils.StorageFile storage){
        File[] listFiles = new File(AppUtils.getFilePath(context, storage)).listFiles();
        for (File file : listFiles) {
            if (file.isFile()) {
                    String oldName = file.getName();
                if (!arrayList.contains(oldName)) {
                    if (file.delete()) {
                        String selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=? ";
                        String[] selectionArgs = new String[]{oldName};
                        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, selectionArgs);
                    }
                }
            }
        }
    }

    /**
     * 监听点播视频下载线程
     * @param period
     */
    boolean multicastThreadRunning = false;
    private void monitorMutlicastDownloadThread(final String period){
        if (multicastThreadRunning){
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                multicastThreadRunning =  true;
                int count = 0;
                while (true){
                    List<OnDemandBean> list = dbHelper.findMutlicastMediaLib();
                    if (list==null){
                        break;
                    }
                    if (list!=null&&list.size()-1==count){
                        session.setMulticastMediaPeriod(period);
                        break;
                    }
                    for (OnDemandBean bean:list){
                        boolean isCompleted = TextUtils.isEmpty(bean.getTaskId())||queryDownloadStatus(bean.getTaskId());
                        try {
                            if (isCompleted){
                                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast)+bean.getTitle();
                                if (isDownloadCompleted(path,bean.getMd5())){

                                    count ++;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(60*1000*2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                multicastThreadRunning =  false;
            }
        }).start();

    }
    @Override
    public void onError(AppApi.Action method, Object obj) {

    }

    @Override
    public void onNetworkFailed(AppApi.Action method) {

    }



    @Override
    public void onDestroy() {
        this.isRun = false;
        super.onDestroy();
    }
}
