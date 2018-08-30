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
import com.savor.ads.oss.OSSUtils;
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
import java.util.ArrayList;
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
         * 1:呼玛  2：投屏：3 退出投屏 4:投屏多张图片（包括单张）5:点播机顶盒内存在的视频
         * 101：发起游戏 102:开始游戏 103:加入游戏 104:退出游戏
         */

        if (ConstantValues.NETTY_MINI_PROGRAM_COMMAND.equals(msg)){
            if (!TextUtils.isEmpty(content)){
                try {
                    JSONObject jsonObject = new JSONObject(content);
                    int action = jsonObject.getInt("action");
                    if (action==1){
                        int code = jsonObject.getInt("code");
                        if (!(ActivitiesManager.getInstance().getCurrentActivity() instanceof MainActivity)) {
                            if (getApplication() instanceof SavorApplication) {
                                ((SavorApplication) getApplication()).showQrCodeWindow(code+"");
                            }
                        }
                    }else if(action==2){
                      boolean isDownloaded=false;
                      String url = jsonObject.getString("url");
                      String fileName = jsonObject.getString("filename");
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
                          if (2==resourceType){
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
                          }

                          isDownloaded = ossUtils.syncDownload();
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
                     if (isDownloaded){
                         MobclickAgent.onEvent(context,"screenProjctionDownloadSuccess"+file.getName());
                        if (1==resourceType){
                         if (!TextUtils.isEmpty(GlobalValues.PROJECTION_WORDS)){
                             ProjectOperationListener.getInstance(context).showImage(1,path,true,GlobalValues.PROJECTION_WORDS);
                         }else{
                             ProjectOperationListener.getInstance(context).showImage(1,path,true);
                         }
                        }else if (2==resourceType){
                            ProjectOperationListener.getInstance(context).showVideo(path,0,true);
                        }
                     }else{
                         MobclickAgent.onEvent(context,"screenProjctionDownloadError"+file.getName());
                     }
                    }else if (action==3){
                        ProjectOperationListener.getInstance(context).stop(GlobalValues.CURRENT_PROJECT_ID);
                    }else if (action==4){

                        final MiniProgramProjection miniProgramProjection = gson.fromJson(content, new TypeToken<MiniProgramProjection>() {}.getType());
                        final Handler handler=new Handler(Looper.getMainLooper());
                        if (miniProgramProjection==null|| TextUtils.isEmpty(miniProgramProjection.getUrl())){
                            return;
                        }
                        final String openid = miniProgramProjection.getOpenid();
                        final int img_nums = miniProgramProjection.getImg_nums();
                        final String forscreen_id = miniProgramProjection.getForscreen_id();
                        final String url = miniProgramProjection.getUrl();
                        final String words = miniProgramProjection.getForscreen_char();
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
                                    }else if (!TextUtils.isEmpty(GlobalValues.CURRENT_OPEN_ID)
                                            &&openid.equals(GlobalValues.CURRENT_OPEN_ID)){
                                        if (!GlobalValues.CURRRNT_PROJECT_ID.equals(forscreen_id)){
                                            GlobalValues.PROJECT_IMAGES.clear();
                                            GlobalValues.CURRRNT_PROJECT_ID = forscreen_id;
                                            GlobalValues.PROJECTION_WORDS = words;
                                        }
                                    }else{
                                        GlobalValues.PROJECT_IMAGES.clear();
                                        GlobalValues.CURRENT_OPEN_ID = openid;
                                        GlobalValues.CURRRNT_PROJECT_ID = forscreen_id;
                                        GlobalValues.PROJECTION_WORDS = words;
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
                        if (isDownloaded&&img_nums>1){
                            if (!TextUtils.isEmpty(openid)){
                                GlobalValues.PROJECT_IMAGES.add(path);
                            }
                            if (GlobalValues.PROJECT_IMAGES!=null&&GlobalValues.PROJECT_IMAGES.size()==1){
                                ProjectOperationListener.getInstance(context).showImage(1,GlobalValues.PROJECT_IMAGES.get(0),true,words);
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
                            ProjectOperationListener.getInstance(context).showImage(1,path,true,words);
                        }
                    }else if (action==5){
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
                                ProjectOperationListener.getInstance(context).showVideo(path,0,true);
                            }
                        }else if (listMutlicast!=null&&listMutlicast.size()>0){
                            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast) +fileName;
                            File file = new File(path);
                            if (file.exists()){
                                ProjectOperationListener.getInstance(context).showVideo(path,0,true);
                            }
                        }else{
                            ProjectOperationListener.getInstance(context).showVideo(url,0,true);
                        }

                    }else if(action==101){
                        MiniProgramProjection programProjection = gson.fromJson(content, new TypeToken<MiniProgramProjection>() {}.getType());

                        Intent intent = new Intent(context,MonkeyGameActivity.class);
                        intent.putExtra("miniProgramProjection",programProjection);
                        startActivity(intent);
                    }else if (action==102){
                        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
                            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
                            activity.startGame();
                        }
                    }else if (action==103){
                        MiniProgramProjection programProjection = gson.fromJson(content, new TypeToken<MiniProgramProjection>() {}.getType());
                        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
                            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
                            activity.addWeixinAvatarToGame(programProjection);
                        }

                    }else if (action==104){
                        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
                            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
                            activity.exitGame();
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
//        getMiniProgramQRCode();
        LogUtils.i("CurrentActivity.................." + ActivitiesManager.getInstance().getCurrentActivity());
        session.setHeartbeatMiniNetty(true);
//        if ((ActivitiesManager.getInstance().getCurrentActivity() instanceof AdsPlayerActivity)) {
//            Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//
//            ((AdsPlayerActivity) activity).showMiniProgramQrCodeWindow();
//            LogFileUtil.write("MiniProgramNettyService showMiniProgramQrCodeWindow");
//        }

    }


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


    public interface DownloadProgressListener{
        void getDownloadProgress(long currentSize, long totalSize);
    }
}
