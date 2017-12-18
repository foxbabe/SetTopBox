package com.savor.ads.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;

import com.savor.ads.activity.AdsPlayerActivity;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogUtils;

import cn.savor.small.netty.NettyClient;

public class GreetingService extends Service {

    public static final String EXTRA_DEVICE_ID = "extra_device_id";
    public static final String EXTRA_DEVICE_NAME = "extra_device_name";
    public static final String EXTRA_WORDS = "extra_words";
    public static final String EXTRA_TEMPLATE = "extra_template";

    public static final int DURATION = 1000 * 10;
    public static final int INTERVAL = 1000 * 60 * 10;
    public static final int MAX_COUNT = 6;

    private String deviceId;
    private String deviceName;
    private String words;
    private int template;

    private int mCount;

    private Handler mHandler = new Handler();

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mCount++;
            LogUtils.d("will show greeting in service, current count is " + mCount);

            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) &&
                    ActivitiesManager.getInstance().getCurrentActivity() instanceof AdsPlayerActivity &&
                    !GlobalValues.IS_BOX_BUSY) {
                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                GlobalValues.IS_RSTR_PROJECTION = true;
                GlobalValues.CURRENT_PROJECT_DEVICE_IP = "1.1.1.1";
                AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                ProjectOperationListener.getInstance(GreetingService.this).showGreeting(words, template, DURATION, false);
            }

            if (mCount >= MAX_COUNT) {
                LogUtils.d("reach max count, will stopSelf");
                stopSelf();
            } else {
                mHandler.postDelayed(mRunnable, INTERVAL);
            }
        }
    };

    public GreetingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
        deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME);
        words = intent.getStringExtra(EXTRA_WORDS);
        template = intent.getIntExtra(EXTRA_TEMPLATE, 1);
        mCount = 0;
        LogUtils.d("New greeting is coming, words is " + words);

        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, INTERVAL);
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRunnable);
        LogUtils.d("GreetingService is destroy");
    }
}
