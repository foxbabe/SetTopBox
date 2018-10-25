package com.savor.ads.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.android.internal.policy.IFaceLockCallback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.BuildConfig;
import com.savor.ads.SavorApplication;
import com.savor.ads.activity.AdsPlayerActivity;
import com.savor.ads.activity.MainActivity;
import com.savor.ads.activity.MonkeyGameActivity;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.MiniProgramProjection;
import com.savor.ads.bean.PrizeInfo;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.dialog.AtlasDialog;
import com.savor.ads.dialog.AtlasDialog.UpdateDownloadedProgress;
import com.savor.ads.dialog.CircularProgressDialog;
import com.savor.ads.okhttp.coreProgress.download.ProgressDownloader;
import com.savor.ads.oss.OSSUtils;
import com.savor.ads.projection.ProjectionManager;
import com.savor.ads.projection.action.VodAction;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.MiniProgramQrCodeWindowManager;
import com.savor.ads.utils.ShowMessage;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.savor.small.netty.MiniProNettyClient;
import cn.savor.small.netty.MiniProNettyClient.MiniNettyMsgCallback;
import cn.savor.small.netty.NettyClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.memcache.binary.BinaryMemcacheRequestEncoder;

/**
 * 启动小程序Netty服务
 * Created by zhanghq on 2018/07/09.
 */
public class MiniProgramNettyService extends IntentService implements MiniNettyMsgCallback{
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    private Context context;
    private Session session;
    AtlasDialog atlasDialog =null;
    CircularProgressDialog circularProgressDialog = null;
    private int INTERVAL_TIME=1000*8;
    private int currentIndex;
    private String words;
    private String avatarUrl;
    private String nickName;
    Handler handler=new Handler(Looper.getMainLooper());
    public MiniProgramNettyService() {
        super("");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        session = Session.get(context);
        atlasDialog = new AtlasDialog(context);
        circularProgressDialog = new CircularProgressDialog(context);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogUtils.d("MiniProgramNettyService onHandleIntent");
        LogFileUtil.write("MiniProgramNettyService onHandleIntent");
        try {
            LogUtils.d("启动小程序NettyService");
            LogFileUtil.write("启动小程序NettyService");

            fetchMessage();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("MiniProgramNettyService onDestroy");
        LogFileUtil.write("MiniProgramNettyService onDestroy");
    }

    private void fetchMessage() throws InterruptedException {
        try{
            LogUtils.d("MiniProgramNettyService fetchMessage");

            MiniProNettyClient.init(ConstantValues.MINI_PROGRAM_NETTY_PORT,ConstantValues.MINI_PROGRAM_NETTY_URL,this,getApplicationContext());
            MiniProNettyClient.get().connect(MiniProNettyClient.get().configureBootstrap(new Bootstrap()));

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveMiniServerMsg(String msg, String content) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        /***
         * action字段
         * 1:呼玛  2：投屏：3 退出投屏 4:投屏多张图片（包括单张）5:点播机顶盒内存在的视频 9:手机小程序呼出大码
         * 101：发起游戏 102:开始游戏 103:加入游戏 104:退出游戏 105:原班人马，在玩一次
         */

        if (ConstantValues.NETTY_MINI_PROGRAM_COMMAND.equals(msg)){
            if (!TextUtils.isEmpty(content)){
                try {
                    JSONObject jsonObject = new JSONObject(content);
                    int action = jsonObject.getInt("action");
                    if (action!=101&&action!=102&&action!=103&&action!=105
                            &&ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity){
                        MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
                        activity.exitGame();
                    }
                    //如果是小程序投屏的话，就将app投屏状态情况
                    GlobalValues.CURRENT_PROJECT_BITMAP = null;
                    MiniProgramProjection miniProgramProjection = gson.fromJson(content, new TypeToken<MiniProgramProjection>() {}.getType());
                    if (action==1){
                        int code = jsonObject.getInt("code");
                        if (!(ActivitiesManager.getInstance().getCurrentActivity() instanceof MainActivity)) {
                            if (getApplication() instanceof SavorApplication) {
                                ((SavorApplication) getApplication()).showQrCodeWindow(code+"");
                            }
                        }
                    }else if(action==2){
                      if (miniProgramProjection==null){
                          return;
                      }
                      boolean isDownloaded=false;
                      String url = miniProgramProjection.getUrl();
                      String fileName = miniProgramProjection.getFilename();
                      String avatarUrl = miniProgramProjection.getAvatarUrl();
                      String nickName = miniProgramProjection.getNickName();

                      int resourceType = 0;
                      if (jsonObject.has("resource_type")){
                          resourceType = jsonObject.getInt("resource_type");
                      }else {
                          resourceType = 1;
                      }
                      if (TextUtils.isEmpty(url)){
                          return;
                      }
                      String path = AppUtils.getFilePath(context, AppUtils.StorageFile.lottery) +fileName;
                      File file = new File(path);
                      if (file.exists()){
                          isDownloaded = true;
                      }else{
                          OSSUtils ossUtils = new OSSUtils(context,
                                  BuildConfig.OSS_BUCKET_NAME,
                                  url,
                                  file);
                          final Handler handler=new Handler(Looper.getMainLooper());
                          HashMap<String,Object> params = new HashMap<>();
                          params.put("action","1");
                          params.put("openid",miniProgramProjection.getOpenid());
                          params.put("order_time",System.currentTimeMillis());
                          if (2==resourceType){
                              if (atlasDialog!=null&&atlasDialog.isShowing()){
                                  atlasDialog.projectTipAnimateOut();
                              }
                              params.put("video_id",miniProgramProjection.getVideo_id());
                              postProjectionVideosLog(params);
                              ossUtils.setDownloadProgressListener(new DownloadProgressListener() {
                                  @Override
                                  public void getDownloadProgress(final long currentSize, final long totalSize) {

                                      handler.post(new Runnable() {
                                          @Override
                                          public void run() {
                                              if (circularProgressDialog!=null&&!circularProgressDialog.isShowing()){
                                                  circularProgressDialog.show();

                                              }
                                              circularProgressDialog.initContent();
                                              circularProgressDialog.projectTipAnimateIn();
                                              BigDecimal b  =   new  BigDecimal(currentSize*1.0/totalSize);
                                              Log.d("circularProgress","原始除法得到的值"+currentSize*1.0/totalSize);
                                              float   f1   =  b.setScale(2,  BigDecimal.ROUND_HALF_UP).floatValue();
                                              Log.d("circularProgress","保留两位小数得到的值"+f1);
                                              if (f1>=0.01f){
                                                  String value = String.valueOf(f1*100);
                                                  int progress = Integer.valueOf(value.split("\\.")[0]);
                                                  Log.d("circularProgress","乘以100并且转成整数得到的值"+progress);
                                                  circularProgressDialog.initnum(progress);
                                              }

                                          }
                                      });
                                  }
                              });
                          }else{
                              params.put("img_id",miniProgramProjection.getImg_id());
                              postProjectionImagesLog(params);
                          }
                          isDownloaded = ossUtils.syncDownload();
                          if (resourceType==2){
                              handler.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      if (circularProgressDialog!=null){
                                          circularProgressDialog.projectTipAnimateOut();
                                          circularProgressDialog.dismiss();
                                      }
                                  }
                              });
                          }
                      }
                     if (isDownloaded){
                         if (handler!=null){
                             handler.removeCallbacks(mProjectShowImageRunnable);
                         }
                         MobclickAgent.onEvent(context,"screenProjctionDownloadSuccess"+file.getName());
                         HashMap<String,Object> params = new HashMap<>();
                        if (1==resourceType){
                            params.put("action","2");
                            params.put("openid",miniProgramProjection.getOpenid());
                            params.put("img_id",miniProgramProjection.getImg_id());
                            params.put("order_time",System.currentTimeMillis());
                            postProjectionImagesLog(params);
                         if (!TextUtils.isEmpty(GlobalValues.PROJECTION_WORDS)){
                             ProjectOperationListener.getInstance(context).showImage(1,path,true,GlobalValues.PROJECTION_WORDS,avatarUrl,nickName);
                         }else{
                             ProjectOperationListener.getInstance(context).showImage(1,path,true,avatarUrl,nickName);
                         }
                        }else if (2==resourceType){
                            params.put("action","2");
                            params.put("openid",miniProgramProjection.getOpenid());
                            params.put("video_id",miniProgramProjection.getVideo_id());
                            params.put("order_time",System.currentTimeMillis());
                            postProjectionVideosLog(params);
                            ProjectOperationListener.getInstance(context).showVideo(path,0,true,avatarUrl,nickName);
                        }
                     }else{
                         MobclickAgent.onEvent(context,"screenProjctionDownloadError"+file.getName());
                     }
                    }else if (action==3){
                        if (handler!=null){
                            handler.removeCallbacks(mProjectShowImageRunnable);
                        }
                        if (atlasDialog!=null&&atlasDialog.isShowing()){
                            atlasDialog.projectTipAnimateOut();
                        }
                        ProjectOperationListener.getInstance(context).stop(GlobalValues.CURRENT_PROJECT_ID);
                    }else if (action==4){

                        final Handler handler=new Handler(Looper.getMainLooper());
                        if (miniProgramProjection==null|| TextUtils.isEmpty(miniProgramProjection.getUrl())){
                            return;
                        }
                        avatarUrl = miniProgramProjection.getAvatarUrl();
                        nickName = miniProgramProjection.getNickName();
                        words = miniProgramProjection.getForscreen_char();
                        final String openid = miniProgramProjection.getOpenid();
                        final int img_nums = miniProgramProjection.getImg_nums();
                        final String forscreen_id = miniProgramProjection.getForscreen_id();
                        final String url = miniProgramProjection.getUrl();
                        if (miniProgramProjection.getImg_nums()>1){

                            handler.post(new Runnable(){
                                public void run(){

                                    if (!atlasDialog.isShowing()){
                                        atlasDialog.show();
                                    }
                                    if(!TextUtils.isEmpty(GlobalValues.CURRENT_OPEN_ID)
                                            &&!openid.equals(GlobalValues.CURRENT_OPEN_ID)){
                                        GlobalValues.PROJECT_IMAGES.clear();
                                        GlobalValues.CURRENT_OPEN_ID = openid;
                                        GlobalValues.CURRRNT_PROJECT_ID = forscreen_id;
                                        GlobalValues.PROJECTION_WORDS = words;
                                        if (handler!=null){
                                            handler.removeCallbacks(mProjectShowImageRunnable);
                                        }
                                    }else if (!TextUtils.isEmpty(GlobalValues.CURRENT_OPEN_ID)
                                            &&openid.equals(GlobalValues.CURRENT_OPEN_ID)){
                                        if (!GlobalValues.CURRRNT_PROJECT_ID.equals(forscreen_id)){
                                            GlobalValues.PROJECT_IMAGES.clear();
                                            GlobalValues.CURRRNT_PROJECT_ID = forscreen_id;
                                            GlobalValues.PROJECTION_WORDS = words;
                                            if (handler!=null){
                                                handler.removeCallbacks(mProjectShowImageRunnable);
                                            }
                                        }
                                    }else{
                                        GlobalValues.PROJECT_IMAGES.clear();
                                        GlobalValues.CURRENT_OPEN_ID = openid;
                                        GlobalValues.CURRRNT_PROJECT_ID = forscreen_id;
                                        GlobalValues.PROJECTION_WORDS = words;
                                        if (handler!=null){
                                            handler.removeCallbacks(mProjectShowImageRunnable);
                                        }
                                    }
                                    if (GlobalValues.PROJECT_IMAGES.size()>=img_nums){
                                        atlasDialog.initContent(img_nums,GlobalValues.PROJECT_IMAGES.size());
                                    }else{
                                        atlasDialog.initContent(img_nums,GlobalValues.PROJECT_IMAGES.size()+1);
                                    }

                                    atlasDialog.projectTipAnimateIn();
                                    atlasDialog.initnum(GlobalValues.PROJECT_IMAGES.size()+1);

                                }
                            });
                        }
                        HashMap<String,Object> params = new HashMap<>();
                        params.put("action","1");
                        params.put("openid",miniProgramProjection.getOpenid());
                        params.put("img_id",miniProgramProjection.getImg_id());
                        params.put("order_time",System.currentTimeMillis());
                        postProjectionImagesLog(params);
                        boolean isDownloaded=false;
                        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.lottery) +miniProgramProjection.getFilename();
                        File file = new File(path);
                        if (file.exists()){
                            isDownloaded = true;
                        }else{
                            OSSUtils ossUtils = new OSSUtils(context,
                                    BuildConfig.OSS_BUCKET_NAME,
                                    url,
                                    file);
                            isDownloaded = ossUtils.syncDownload();
                        }
                        if (isDownloaded){
                            params.clear();
                            params.put("action","2");
                            params.put("openid",miniProgramProjection.getOpenid());
                            params.put("img_id",miniProgramProjection.getImg_id());
                            params.put("order_time",System.currentTimeMillis());
                            postProjectionImagesLog(params);
                        }
                        if (isDownloaded&&img_nums>1){
                            if (!TextUtils.isEmpty(openid)){
                                GlobalValues.PROJECT_IMAGES.add(path);
                            }
                            if (GlobalValues.PROJECT_IMAGES!=null&&GlobalValues.PROJECT_IMAGES.size()==1){
                                currentIndex = 0;
                                projectShowImage(currentIndex,words,avatarUrl,nickName);
//                                ProjectOperationListener.getInstance(context).showImage(1,GlobalValues.PROJECT_IMAGES.get(0),true,words,avatarUrl,nickName);
                            }
                            if (img_nums==GlobalValues.PROJECT_IMAGES.size()){
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        atlasDialog.projectTipAnimateOut();
                                    }
                                },1000 * 2);
                            }
                        }else if (isDownloaded&&miniProgramProjection.getImg_nums()==1){
                            ProjectOperationListener.getInstance(context).showImage(1,path,true,words,avatarUrl,nickName);
                        }
                    }else if (action==5){
                        if (handler!=null){
                            handler.removeCallbacks(mProjectShowImageRunnable);
                        }
                        String fileName = jsonObject.getString("filename");
                        String url = jsonObject.getString("url");

                        String selection=DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=? ";
                        String[] selectionArgs=new String[]{fileName};
                        List<MediaLibBean> listPlayList = DBHelper.get(context).findNewPlayListByWhere(selection,selectionArgs);
                        List<MediaLibBean> listMutlicast = DBHelper.get(context).findMutlicastMediaLibByWhere(selection,selectionArgs);
                        if (listPlayList!=null&&listPlayList.size()>0){
                            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) +fileName;
                            File file = new File(path);
                            if (file.exists()){
                                ProjectOperationListener.getInstance(context).showVod(fileName,"",0,false,true);
                            }
                        }else if (listMutlicast!=null&&listMutlicast.size()>0){
                            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast) +fileName;
                            File file = new File(path);
                            if (file.exists()){
                                ProjectOperationListener.getInstance(context).showVod(fileName,"",0,false,true);
                            }
                        }else{
                            ProjectOperationListener.getInstance(context).showVideo(url,0,true);
                        }

                    }else if(action==9){
                        if (handler!=null){
                            handler.removeCallbacks(mProjectShowImageRunnable);
                        }
                        String selection=DBHelper.MediaDBInfo.FieldName.VID + "=? ";
                        String[] selectionArgs=new String[]{"17614"};
                        List<MediaLibBean> listPlayList = DBHelper.get(context).findNewPlayListByWhere(selection,selectionArgs);
                        List<MediaLibBean> listMutlicast = DBHelper.get(context).findMutlicastMediaLibByWhere(selection,selectionArgs);
                        if (listPlayList!=null&&listPlayList.size()>0){
                            MediaLibBean bean = listPlayList.get(0);
                            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) +bean.getName();
                            File file = new File(path);
                            if (file.exists()){
                                VodAction vodAction = new VodAction(context, "17614", path, 0, false, true);
                                ProjectionManager.getInstance().enqueueAction(vodAction);
                            }
                        }else if (listMutlicast!=null&&listMutlicast.size()>0){
                            MediaLibBean bean = listMutlicast.get(0);
                            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast) +bean.getName();
                            File file = new File(path);
                            if (file.exists()){
                                VodAction vodAction = new VodAction(context, "17614", path, 0, false, true);
                                ProjectionManager.getInstance().enqueueAction(vodAction);
                            }
                        }

                        Handler handler=new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                MiniProgramQrCodeWindowManager.get(context).setCurrentPlayMediaId("17614");
                                ((SavorApplication) getApplication()).showMiniProgramQrCodeWindow(ConstantValues.MINI_PROGRAM_CALL_TYPE);
                            }
                        },1000);

                    }else if(action==101){
                        if (handler!=null){
                            handler.removeCallbacks(mProjectShowImageRunnable);
                        }
                        Intent intent = new Intent(context,MonkeyGameActivity.class);
                        intent.putExtra("miniProgramProjection",miniProgramProjection);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        postMiniProgramProjectionParam(action,miniProgramProjection);

                    }else if (action==102||action==105){
                        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
                            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
                            activity.startGame();
                            if (action!=105){
                                postMiniProgramProjectionParam(action,miniProgramProjection);
                            }
                        }

                    }else if (action==103){

                        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
                            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
                            activity.addWeixinAvatarToGame(miniProgramProjection);
                            postMiniProgramProjectionParam(action,miniProgramProjection);
                        }

                    }else if (action==104){
                        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
                            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
                            activity.exitGame();
                        }
                    }else if (action==999){
                        if (miniProgramProjection==null){
                            return;
                        }
                        boolean isDownloaded=false;
                        String url = miniProgramProjection.getUrl();
                        String fileName = miniProgramProjection.getFilename();

                        if (TextUtils.isEmpty(url)){
                            return;
                        }
                        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.lottery) +fileName;
                        File file = new File(path);
                        if (file.exists()){
                            file.delete();
                        }
                        OSSUtils ossUtils = new OSSUtils(context,
                                BuildConfig.OSS_BUCKET_NAME,
                                url,
                                file);
                        HashMap<String,Object> params = new HashMap<>();
                        params.put("action","1");
                        params.put("openid",miniProgramProjection.getOpenid());
                        params.put("order_time",System.currentTimeMillis());
                        params.put("video_id",miniProgramProjection.getVideo_id());
                        postProjectionVideosLog(params);

                        isDownloaded = ossUtils.syncDownload();


                        if (isDownloaded){
                            params = new HashMap<>();
                            params.put("action","2");
                            params.put("openid",miniProgramProjection.getOpenid());
                            params.put("video_id",miniProgramProjection.getVideo_id());
                            params.put("order_time",System.currentTimeMillis());
                            postProjectionVideosLog(params);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public void onMiniConnected() {
        //TODO:当建立NETTY连接以后请求接口获取小程序地址
        LogUtils.i("CurrentActivity.................." + ActivitiesManager.getInstance().getCurrentActivity());
        session.setHeartbeatMiniNetty(true);

    }

    private void projectShowImage(int currentIndex,String words,String avatarUrl,String nickName){
        if (GlobalValues.PROJECT_IMAGES!=null&&GlobalValues.PROJECT_IMAGES.size()>0){
            boolean flag = true;
            if (GlobalValues.PROJECT_IMAGES.size()>currentIndex){
                String uri = GlobalValues.PROJECT_IMAGES.get(currentIndex);
                ProjectOperationListener.getInstance(context).showImage(1,uri,true,words,avatarUrl,nickName);
            }else{
                flag = false;
            }
            if (handler!=null){
                if (flag){
                    handler.postDelayed(mProjectShowImageRunnable,INTERVAL_TIME);
                }else {
                    handler.removeCallbacks(mProjectShowImageRunnable);
                }
            }


        }

    }

    private Runnable  mProjectShowImageRunnable = new Runnable(){

        @Override
        public void run() {
            currentIndex ++;
            projectShowImage(currentIndex,words,avatarUrl,nickName);
        }
    };

    @Override
    public void onMiniReconnect() {
            Intent intent = new Intent(this,MiniProgramNettyService.class);
            startService(intent);
    }

    @Override
    public void onMiniCloseIcon() {
        Activity activity = ActivitiesManager.getInstance().getSpecialActivity(AdsPlayerActivity.class);
        if (activity!=null && activity instanceof AdsPlayerActivity){
            ((SavorApplication) getApplication()).hideMiniProgramQrCodeWindow();
        }
    }


    ApiRequestListener apiRequestListener = new ApiRequestListener() {
        @Override
        public void onSuccess(AppApi.Action method, Object obj) {
            switch (method){

            }
        }

        @Override
        public void onError(AppApi.Action method, Object obj) {

        }

        @Override
        public void onNetworkFailed(AppApi.Action method) {

        }
    };
    /**
     * 上传小程序投屏参数到云
     * */
    private void postMiniProgramProjectionParam(int action,MiniProgramProjection programProjection){
        if (programProjection==null){
            return;
        }
        HashMap<String,Object> params = new HashMap<>();
        switch (action){
            case 101:
                params.put("action","1");
                params.put("activity_id",programProjection.getActivity_id());
                params.put("order_time",System.currentTimeMillis());
                postProjectionGamesLog(params);
                break;
            case 102:
                params.put("action","3");
                params.put("activity_id",programProjection.getActivity_id());
                params.put("order_time",System.currentTimeMillis());
                postProjectionGamesLog(params);
                break;

            case 103:
                params.put("action","2");
                params.put("activity_id",programProjection.getActivity_id());
                params.put("openid",programProjection.getOpenid());
                params.put("order_time",System.currentTimeMillis());
                postProjectionGamesLog(params);
                break;
        }
    }

    /**
     * 视频投屏数据上报
     * @param params
     */
    private void postProjectionVideosLog(HashMap<String,Object> params){
        AppApi.postProjectionVideosParam(context,apiRequestListener,params);
    }

    /**
     * 图片投屏数据上报
     * @param params
     */
    private void postProjectionImagesLog(HashMap<String,Object> params){
        AppApi.postProjectionImagesParam(context,apiRequestListener,params);
    }

    /**
     * 互动游戏数据上传
     * */
    private void postProjectionGamesLog(HashMap<String,Object> params){
        AppApi.postProjectionGamesParam(context,apiRequestListener,params);
    }

    public interface DownloadProgressListener{
        void getDownloadProgress(long currentSize, long totalSize);
    }
}
