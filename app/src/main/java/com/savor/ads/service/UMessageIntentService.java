package com.savor.ads.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.BuildConfig;
import com.savor.ads.bean.Push4GProjection;
import com.savor.ads.bean.PushRTBItem;
import com.savor.ads.bean.UpgradeInfo;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.oss.OSSUtils;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlideImageLoader;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.ShellUtils;
import com.savor.ads.utils.ShowMessage;
import com.savor.ads.utils.UpdateUtil;
import com.umeng.message.UmengMessageService;
import com.umeng.message.entity.UMessage;

import org.android.agoo.common.AgooConstants;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class UMessageIntentService extends UmengMessageService {
    private UpgradeInfo upgradeInfo;
    @Override
    public void onMessage(Context context, Intent intent) {
        Log.d("UMessageIntentService", "onMessage");

        try {
            String message = intent.getStringExtra(AgooConstants.MESSAGE_BODY);
            UMessage msg = new UMessage(new JSONObject(message));
            LogUtils.d("message=" + message);      //消息体
            LogFileUtil.writeKeyLogInfo("UPush onMessage custom is " + msg.custom);

            if (!TextUtils.isEmpty(msg.custom)) {
                JSONObject jsonObject = new JSONObject(msg.custom);
                int type = jsonObject.getInt("type");
                //1:RTB推送;2:移动网络4g投屏
                if (ConstantValues.PUSH_TYPE_RTB_ADS == type) {
                    // RTB推送
                    ArrayList<PushRTBItem> pushRTBItems = new Gson().fromJson(jsonObject.getString("data"), new TypeToken<ArrayList<PushRTBItem>>() {
                    }.getType());

                    if (pushRTBItems != null) {
                        for (PushRTBItem item : pushRTBItems) {
                            item.setRemain_time(item.getRemain_time() * 1000 + System.currentTimeMillis());
                        }
                    }
                    Session.get(this).setRTBPushItems(pushRTBItems);

                    if (AppUtils.fillPlaylist(this, null, 1)) {
                        sendBroadcast(new Intent(ConstantValues.UPDATE_PLAYLIST_ACTION));
                    }
                }else if (ConstantValues.PUSH_TYPE_4G_PROJECTION==type){
                    //action.1投屏，0结束投屏
                    if (1==jsonObject.getInt("action")){
                        Push4GProjection push4GProjection = new Gson().fromJson(jsonObject.getString("data"), new TypeToken<Push4GProjection>() {
                        }.getType());
                        if (push4GProjection!=null){
                            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.lottery) + push4GProjection.getResource_name();
                            boolean isDownloaded=false;
                            //resource_type:1图片，2视频
                            File file = new File(path);
                            if (file.exists()){
                                isDownloaded = true;
                            }else{
                                OSSUtils ossUtils = new OSSUtils(context,
                                        BuildConfig.OSS_BUCKET_NAME,
                                        push4GProjection.getResource_url(),
                                        file);
                                isDownloaded = ossUtils.syncDownload();
                            }

                            if (isDownloaded){
                                if (1==push4GProjection.getResource_type()){
                                    ProjectOperationListener.getInstance(context).showImage(1,path,true,"","");
                                }else if (2==push4GProjection.getResource_type()){
                                    ProjectOperationListener.getInstance(context).showVideo(path,0,true);
                                }
                            }

                        }
                    }else{
                        ProjectOperationListener.getInstance(context).stop(GlobalValues.CURRENT_PROJECT_ID);
                    }

                }else if (ConstantValues.PUSH_TYPE_SHELL_COMMAND==type){
                    //action:0不返回shell结果内容，1返回shell结果内容
                    int action = jsonObject.getInt("action");
                    final List<String> list = new Gson().fromJson(jsonObject.getString("data"), new TypeToken<List<String>>() {
                    }.getType());
                    if (list!=null&&list.size()>0){
                        Handler handler=new Handler(Looper.getMainLooper());
                        handler.post(new Runnable(){
                            public void run(){
                                ShowMessage.showToast(getApplicationContext(),list.toString());
                            }
                        });
                        JSONArray jsonArray = ShellUtils.universalShellCommandMethod(list,action);
                        if (action==1&&jsonArray!=null){
                            postShellCommandResult(context,jsonArray);
                        }
                    }

                }else if (ConstantValues.PUSH_TYPE_UPDATE==type){
                    int action = jsonObject.getInt("action");
                    upgradeInfo = new Gson().fromJson(jsonObject.getString("data"), new TypeToken<UpgradeInfo>() {
                    }.getType());
                    if (upgradeInfo==null){
                        return;
                    }
                    Handler handler=new Handler(Looper.getMainLooper());
                    handler.post(new Runnable(){
                        public void run(){
                            ShowMessage.showToast(getApplicationContext(),"推送新版本，开始下载，准备升级");
                        }
                    });

                    downloadApk(context);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void postShellCommandResult(Context context,JSONArray jsonArray){
        AppApi.postShellCommandResult(context,apiRequestListener,jsonArray);
    }

    private void downloadApk(Context context){
        AppApi.downVersion(upgradeInfo.getApkUrl(), context, apiRequestListener, 2);
    }

    ApiRequestListener apiRequestListener  = new ApiRequestListener() {
        @Override
        public void onSuccess(AppApi.Action method, Object obj) {
            switch (method){
                case CP_POST_SHELL_COMMAND_RESULT_JSON:

                    break;
                case SP_GET_UPGRADEDOWN:
                    if (obj instanceof File) {
                        File f = (File) obj;
                        handleUpdateResult(f);
                    }
                    break;
            }
        }

        @Override
        public void onError(AppApi.Action method, Object obj) {
            switch (method){
                case CP_POST_SHELL_COMMAND_RESULT_JSON:

                    break;
                case SP_GET_UPGRADEDOWN:

                    break;
            }
        }

        @Override
        public void onNetworkFailed(AppApi.Action method) {

        }
    };

    //处理升级结果
    private void handleUpdateResult(File file){
        if (upgradeInfo==null){
            return;
        }
        byte[] fRead;
        String md5Value = null;
        try {
            fRead = FileUtils.readFileToByteArray(file);
            md5Value = AppUtils.getMD5(fRead);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //比较本地文件MD5是否与服务器文件一致，如果一致则启动安装
        String fileName = file.getName();
        if (ConstantValues.APK_DOWNLOAD_FILENAME.equals(fileName)) {
            if (upgradeInfo!=null&&md5Value != null && md5Value.equals(upgradeInfo.getApkMd5())) {
                //升级APK
                if (AppUtils.isMstar()) {
                    UpdateUtil.updateApk(file);
                } else {
                    UpdateUtil.updateApk4Giec(file);
                }
            }
        }
    }
}