package com.savor.ads;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.savor.ads.utils.LogUtils;
import com.umeng.message.UmengMessageService;
import com.umeng.message.entity.UMessage;

import org.android.agoo.common.AgooConstants;
import org.json.JSONException;
import org.json.JSONObject;

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
            LogUtils.d("custom=" + msg.custom);    //自定义消息的内容
            LogUtils.d("title=" + msg.title);      //通知标题
            LogUtils.d("text=" + msg.text);        //通知内容
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}