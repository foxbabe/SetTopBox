package com.savor.ads.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.BuildConfig;
import com.savor.ads.bean.Push4GProjection;
import com.savor.ads.bean.PushRTBItem;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.Session;
import com.savor.ads.oss.OSSUtils;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlideImageLoader;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.umeng.message.UmengMessageService;
import com.umeng.message.entity.UMessage;

import org.android.agoo.common.AgooConstants;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class UMessageIntentService extends UmengMessageService {

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
                                    ProjectOperationListener.getInstance(context).showImage(1,path,true);
                                }else if (2==push4GProjection.getResource_type()){
                                    ProjectOperationListener.getInstance(context).showVideo(path,0,true);
                                }
                            }

                        }
                    }else{
                        ProjectOperationListener.getInstance(context).stop(GlobalValues.CURRENT_PROJECT_ID);
                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}