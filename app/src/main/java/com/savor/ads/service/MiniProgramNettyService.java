package com.savor.ads.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.savor.ads.BuildConfig;
import com.savor.ads.SavorApplication;
import com.savor.ads.activity.AdsPlayerActivity;
import com.savor.ads.activity.MainActivity;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.oss.OSSUtils;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.MiniProgramQrCodeWindowManager;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import cn.savor.small.netty.MiniProNettyClient;
import cn.savor.small.netty.NettyClient;
import io.netty.bootstrap.Bootstrap;

/**
 * 启动小程序Netty服务
 * Created by zhanghq on 2018/07/09.
 */
public class MiniProgramNettyService extends IntentService implements MiniProNettyClient.MiniNettyMsgCallback {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    private Context context;
    private Session session;
    public MiniProgramNettyService() {
        super("");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        session = Session.get(context);

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
        //action字段  1:呼玛  2：投屏：3 退出投屏
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
                     isDownloaded = ossUtils.syncDownload();
                     if (isDownloaded){
                         MobclickAgent.onEvent(context,"screenProjctionDownloadSuccess"+file.getName());
//                        if (1==push4GProjection.getResource_type()){
                            ProjectOperationListener.getInstance(context).showImage(1,path,true);
//                        }else if (2==push4GProjection.getResource_type()){
//                            ProjectOperationListener.getInstance(context).showVideo(path,0,true);
//                        }
                     }else{
                         MobclickAgent.onEvent(context,"screenProjctionDownloadError"+file.getName());
                     }
                    }else if (action==3){
                        ProjectOperationListener.getInstance(context).stop(GlobalValues.CURRENT_PROJECT_ID);
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
//        if (!(ActivitiesManager.getInstance().getCurrentActivity() instanceof MainActivity)) {
//            if (getApplication() instanceof SavorApplication) {
//                ((SavorApplication) getApplication()).showMiniProgramQrCodeWindow();
//            }
//        }

        if ((ActivitiesManager.getInstance().getCurrentActivity() instanceof AdsPlayerActivity)) {
            Activity activity = ActivitiesManager.getInstance().getCurrentActivity();

                ((AdsPlayerActivity) activity).showMiniProgramQrCodeWindow();

        }
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
            ((AdsPlayerActivity) activity).hideMiniProgramQrCodeWindow();
        }
    }


    ApiRequestListener apiRequestListener = new ApiRequestListener() {
        @Override
        public void onSuccess(AppApi.Action method, Object obj) {

        }

        @Override
        public void onError(AppApi.Action method, Object obj) {

        }

        @Override
        public void onNetworkFailed(AppApi.Action method) {

        }
    };
}
