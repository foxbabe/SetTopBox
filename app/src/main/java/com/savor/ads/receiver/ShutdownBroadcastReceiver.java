package com.savor.ads.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.savor.ads.utils.ShowMessage;

public class ShutdownBroadcastReceiver extends BroadcastReceiver {
    public ShutdownBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
            ShowMessage.showToast(context, "系统关机");
        }
    }
}
