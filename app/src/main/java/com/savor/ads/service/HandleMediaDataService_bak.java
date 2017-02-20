package com.savor.ads.service;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.bean.AdsModel;
import com.savor.ads.bean.BoiteBean;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.OnDemandBean;
import com.savor.ads.bean.OnDemandListBean;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.bean.RoomBean;
import com.savor.ads.bean.SetTopBoxBean;
import com.savor.ads.bean.TvProgramResponse;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.tv.TvOperate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 处理下载媒体文件逻辑的服务
 * Created by bichao on 2016/12/10.
 */

public class HandleMediaDataService_bak extends Service implements ApiRequestListener{

    private Context context;
    private Session session;
    //酒楼实体
    private SetTopBoxBean setTopBoxBean;
    private OnDemandListBean onDemandList;
    //广告媒体文件存放路径
    private String saveMediaFilePath = null;
    private DBHelper dbHelper ;
    List<AdsModel> mAdsList = new ArrayList<AdsModel>();
    List<AdsModel> mCompletedAds = new ArrayList<AdsModel>();
    List<AdsModel> mUnAds = new ArrayList<AdsModel>();
    private boolean isRun = true;
    private DownloadManager downloadManager=null;
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isRun) {
                    getAdvertDataFromSmallPlatfrom();
                    getOnDemandDataFromSmallPlatfrom();
                    try {
                        Thread.sleep(60*1000*10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        getTVMatchDataFromSmallPlatfrom();
        return super.onStartCommand(intent, flags, startId);
    }


    private void getAdvertDataFromSmallPlatfrom(){
        try {
            String configJson = AppApi.getAdvertDataFromSmallPlatform(this,this,"");
            Object result = gson.fromJson(configJson, new TypeToken<SetTopBoxBean>() {
            }.getType());
            if (result instanceof BoiteBean){
                setTopBoxBean = (SetTopBoxBean) result;
                handleSmallPlatformAdvertData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void getOnDemandDataFromSmallPlatfrom(){
        try {
            String ondemandJson = AppApi.getOnDemandDataFromSmallPlatform(this,this,"");
            Object result = gson.fromJson(ondemandJson, new TypeToken<OnDemandListBean>() {
            }.getType());
            if (result instanceof OnDemandListBean){
                onDemandList = (OnDemandListBean)result;
                handleSmallPlatformOnDemandData();
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
            case SP_GET_ADVERT_DATA_FROM_JSON:
                if (obj instanceof BoiteBean){

                    setTopBoxBean = (SetTopBoxBean) obj;
                    handleSmallPlatformAdvertData();
                }
                break;
            case SP_GET_ON_DEMAND_DATA_FROM_JSON:
                if (obj instanceof OnDemandListBean){
                    onDemandList = (OnDemandListBean)obj;
                    handleSmallPlatformOnDemandData();
                }
                break;
            case SP_GET_TV_MATCH_DATA_FROM_JSON:
                if (obj instanceof TvProgramResponse){
                    TvProgramResponse response = (TvProgramResponse)obj;
                    TvOperate mtv = new TvOperate();
                    mtv.updateProgram(context, response);
                }
                break;
        }
    }

    /**
     * 处理小平台返回的广告数据
     */
    private void handleSmallPlatformAdvertData(){
        if (setTopBoxBean==null){
            return ;
        }
        //TODO:此处有一个update动作,UpdateUtil.initUpdate("", mCon, mHandler);
        String config = AppUtils.getFilePath(context, AppUtils.StorageFile.config);
        if (AppUtils.isDirNull(config)){
            //如果本地config.txt文件存在，就删除
            new File(config).delete();
        }
        final BoiteBean boiteBean = setTopBoxBean.getBoite();
        //如果期刊数相同，则表示数据没有改变不需要在此执行
        if (session.getAdvertDownloadingPeriod().equals(setTopBoxBean.getPeriod())){
            if (!session.getAdvertMediaPeriod().equals(setTopBoxBean.getPeriod())){
                monitorDownloadThread(setTopBoxBean.getPeriod());
            }
            return;
        }
        /*******************设置盒子基本信息开始************************/
        initBoxInfo(boiteBean);
        /*******************设置盒子基本信息结束************************/
        /*******************更新媒体库并下载开始********************************/
        List<MediaLibBean> mediaLibList = setTopBoxBean.getMedia_lib();
        deleteDownloadTask(1);
        String selection = DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? ";
        String[] selectionArgs = new String[]{session.getAdvertDownloadingPeriod()};
        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,selection,selectionArgs);
        if (mediaLibList!=null&&mediaLibList.size()>0){
            for(MediaLibBean mediaLib:mediaLibList){
                String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media)
                        + mediaLib.getVid() + "."
                        + mediaLib.getSurfix();
                try {
                    if(isDownloadCompleted(path,mediaLib.getMd5())){
                        PlayListBean bean = new PlayListBean();
                        bean.setPeriod(mediaLib.getPeriod());
                        bean.setDuration(mediaLib.getDuration());
                        bean.setMd5(mediaLib.getMd5());
                        bean.setVid(mediaLib.getVid());
                        bean.setMedia_name(mediaLib.getName());
                        bean.setOrder(mediaLib.getOrder());
                        bean.setSurfix(mediaLib.getSurfix());
                        bean.setMediaPath(path);
//                        dbHelper.insertPlayListLib(bean);
                    }else{
                        createDownloadTask(mediaLib,setTopBoxBean.getPeriod(),path);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            session.setAdvertDownloadingPeriod(setTopBoxBean.getPeriod());
            monitorDownloadThread(setTopBoxBean.getPeriod());
        }

        /*******************更新媒体库并下载结束********************************/

    }

    /**
     * 初始化盒子基本信息
     * @param boiteBean
     */
    void initBoxInfo(BoiteBean boiteBean){
        session.setSwitchTime(setTopBoxBean.getSwitch_time());
        if (boiteBean==null){
            return;
        }
        if(this.session.getAdvertMediaPeriod().equals(setTopBoxBean.getPeriod())){
            //TODO:此处做上报处理
        }
        session.setBoiteId(boiteBean.getHotel_id());
        session.setBoiteName(boiteBean.getHotel_name());
        RoomBean roomBean = setTopBoxBean.getRoom();
        if (roomBean==null){
            return;
        }
        session.setRoomId(roomBean.getRoom_id());
        session.setRoomName(roomBean.getRoom_name());
    }

    /**
     * 如果新一期的任务来了，就把老的一起的下载任务去掉
     */
    private void deleteDownloadTask(int type){
        if (type==1){
            String selection="";
            String[] selectionArgs = new String[]{};
            List<MediaLibBean> list = dbHelper.findMediaLib(selection,selectionArgs);
            if (list!=null&&list.size()>0) {
                for (MediaLibBean bean : list) {
                    if (downloadManager != null) {
                        downloadManager.remove(Long.valueOf(bean.getTaskId()));
                    }
                }
            }
            dbHelper.deleteAllData(DBHelper.MediaDBInfo.TableName.MEDIALIB);

        }else{
            List<OnDemandBean> list = dbHelper.findMutlicastMediaLib();
            if (list!=null&&list.size()>0) {
                for (OnDemandBean bean : list) {
                    if (downloadManager != null&&!TextUtils.isEmpty(bean.getTaskId())) {
                        downloadManager.remove(Long.valueOf(bean.getTaskId()));
                    }
                }
            }
        }

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
                                } catch (IOException e) {
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
    private boolean isDownloadCompleted(String path, String md5) throws IOException {
        if (AppUtils.isFileExist(path)) {
            String realMd5 = AppUtils.getMD5Method(new File(path));
            if (md5.equals(realMd5)) {
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
        if (onDemandList==null||onDemandList.getMedia_lib().size()==0){
            return;
        }
        if (session.getMulticastDownloadingPeriod().equals(onDemandList.getPeriod())){
            if(!session.getMulticastMediaPeriod().equals(onDemandList.getPeriod())){
                monitorMutlicastDownloadThread(onDemandList.getPeriod());
            }
            return;
        }
        String areaid = onDemandList.getAreaid();
        String period = onDemandList.getPeriod();
        String type = onDemandList.getType();
        List<String> list = new ArrayList<String>();
        deleteDownloadTask(2);
        dbHelper.deleteAllData(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB);
        for (OnDemandBean bean:onDemandList.getMedia_lib()){
            String videoName = bean.getTitle();
            list.add(videoName);
            onDemandDownloadMethod(bean,areaid,period,type);

        }
        monitorMutlicastDownloadThread(period);
        session.setMulticastDownloadingPeriod(period);
        deleteMulticastMediaNotInConfig(list);
    }



    private void onDemandDownloadMethod(final OnDemandBean bean, final String areaid,
                                              final String period, final String type){


        String p = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast);
        String path = p+bean.getTitle();

        try {
            if(AppUtils.isFileExist(path)){
                if(isDownloadCompleted(path,bean.getMd5())){
//                    dbHelper.insertmulticastLib(bean);
                }else{
                   File file = new File(path);
                    if (file.exists()&&file.isFile()){
                        file.delete();
                    }
                }
            }else{
                createMulticastDownloadTask(bean,path);
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
    //删除没有在小平台配置文件内的点播视频文件
    private void deleteMulticastMediaNotInConfig(List<String> arrayList){
        File[] listFiles = new File(AppUtils.getFilePath(context, AppUtils.StorageFile.multicast)).listFiles();
        for (File file : listFiles) {
            if (file.isFile()) {
                    String oldName = file.getName();
                if (!arrayList.contains(oldName))
                    file.delete();
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
                        } catch (IOException e) {
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
