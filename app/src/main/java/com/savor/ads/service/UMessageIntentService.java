package com.savor.ads.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.RTBPushItem;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.umeng.message.UmengMessageService;
import com.umeng.message.entity.UMessage;

import org.android.agoo.common.AgooConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
                if (1 == type) {
                    // RTB推送
                    ArrayList<RTBPushItem> rtbPushItems = new Gson().fromJson(jsonObject.getString("data"), new TypeToken<ArrayList<RTBPushItem>>() {
                    }.getType());

                    if (rtbPushItems != null) {
                        for (RTBPushItem item : rtbPushItems) {
                            item.setRemain_time(item.getRemain_time() * 1000 + System.currentTimeMillis());
                        }
                    }
                    Session.get(this).setRTBPushItems(rtbPushItems);

                    if (AppUtils.fillPlaylist(this, null, 1)) {
                        sendBroadcast(new Intent(ConstantValues.UPDATE_PLAYLIST_ACTION));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}