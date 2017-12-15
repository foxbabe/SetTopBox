package com.savor.ads;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.jar.savor.box.ServiceUtil;
import com.jar.savor.box.services.RemoteService;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCode;
import com.savor.ads.utils.KeyCodeConstant;
import com.savor.ads.utils.KeyCodeConstantGiec;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.QrCodeWindowManager;

/**
 * Created by Administrator on 2016/12/9.
 */

public class SavorApplication extends MultiDexApplication {

    private QrCodeWindowManager mQrCodeWindowManager;
    private ServiceConnection mConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        // 设置异常捕获处理类
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        // 初始化文件记录类
        LogFileUtil.init();

        mappingKeyCode();

        mQrCodeWindowManager = new QrCodeWindowManager();
        // 启动投屏类操作处理的Service
        startScreenProjectionService();

        // 检测播放时间
        AppUtils.checkPlayTime(SavorApplication.this);
    }

    /**
     * 显示二维码
     */
    public void showQrCodeWindow(String code) {
        if (TextUtils.isEmpty(code)) {
            code = Session.get(this).getAuthCode();
        }
        mQrCodeWindowManager.showQrCode(this, code);
    }


    private void startScreenProjectionService() {
        mConnection = ServiceUtil.registerService(ProjectOperationListener.getInstance(this));
//        bindService(new Intent(ServiceUtil.ACTION_REMOTE_SERVICE), connection, Service.BIND_AUTO_CREATE);
        bindService(new Intent(this, RemoteService.class), mConnection, Service.BIND_AUTO_CREATE);
    }

    public void stopScreenProjectionService() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }

    private void mappingKeyCode() {
        if (AppUtils.isMstar()) {
            KeyCode.KEY_CODE_ANT_IN = KeyCodeConstant.KEY_CODE_ANT_IN;
            KeyCode.KEY_CODE_AV_IN = KeyCodeConstant.KEY_CODE_AV_IN;
            KeyCode.KEY_CODE_BACK = KeyCodeConstant.KEY_CODE_BACK;
            KeyCode.KEY_CODE_CHANGE_MODE = KeyCodeConstant.KEY_CODE_CHANGE_MODE;
//            KeyCode.KEY_CODE_CHANGE_RESOLUTION = KeyCodeConstant.KEY_CODE_CHANGE_RESOLUTION;
            KeyCode.KEY_CODE_CHANGE_SIGNAL = KeyCodeConstant.KEY_CODE_CHANGE_SIGNAL;
            KeyCode.KEY_CODE_CHANNEL_LIST = KeyCodeConstant.KEY_CODE_CHANNEL_LIST;
            KeyCode.KEY_CODE_DOWN = KeyCodeConstant.KEY_CODE_DOWN;
            KeyCode.KEY_CODE_HDMI_IN = KeyCodeConstant.KEY_CODE_HDMI_IN;
            KeyCode.KEY_CODE_LEFT = KeyCodeConstant.KEY_CODE_LEFT;
            KeyCode.KEY_CODE_MANUAL_HEARTBEAT = KeyCodeConstant.KEY_CODE_MANUAL_HEARTBEAT;
            KeyCode.KEY_CODE_NEXT_ADS = KeyCodeConstant.KEY_CODE_NEXT_ADS;
            KeyCode.KEY_CODE_PLAY_PAUSE = KeyCodeConstant.KEY_CODE_PLAY_PAUSE;
            KeyCode.KEY_CODE_PREVIOUS_ADS = KeyCodeConstant.KEY_CODE_PREVIOUS_ADS;
            KeyCode.KEY_CODE_RIGHT = KeyCodeConstant.KEY_CODE_RIGHT;
            KeyCode.KEY_CODE_SETTING = KeyCodeConstant.KEY_CODE_SETTING;
//            KeyCode.KEY_CODE_SHOW_APP_INSTALLED = KeyCodeConstant.KEY_CODE_SHOW_APP_INSTALLED;
            KeyCode.KEY_CODE_SHOW_INFO = KeyCodeConstant.KEY_CODE_SHOW_INFO;
            KeyCode.KEY_CODE_SHOW_QRCODE = KeyCodeConstant.KEY_CODE_SHOW_QRCODE;
            KeyCode.KEY_CODE_SYSTEM_SETTING = KeyCodeConstant.KEY_CODE_SYSTEM_SETTING;
            KeyCode.KEY_CODE_UP = KeyCodeConstant.KEY_CODE_UP;
            KeyCode.KEY_CODE_UPLOAD_CHANNEL_INFO = KeyCodeConstant.KEY_CODE_UPLOAD_CHANNEL_INFO;
            KeyCode.KEY_CODE_UDISK_UPDATE = KeyCodeConstant.KEY_CODE_UDISK_UPDATE;
        } else {
            KeyCode.KEY_CODE_ANT_IN = KeyCodeConstantGiec.KEY_CODE_ANT_IN;
            KeyCode.KEY_CODE_AV_IN = KeyCodeConstantGiec.KEY_CODE_AV_IN;
            KeyCode.KEY_CODE_BACK = KeyCodeConstantGiec.KEY_CODE_BACK;
            KeyCode.KEY_CODE_CHANGE_MODE = KeyCodeConstantGiec.KEY_CODE_CHANGE_MODE;
            KeyCode.KEY_CODE_CHANGE_RESOLUTION = KeyCodeConstantGiec.KEY_CODE_CHANGE_RESOLUTION;
            KeyCode.KEY_CODE_CHANGE_SIGNAL = KeyCodeConstantGiec.KEY_CODE_CHANGE_SIGNAL;
            KeyCode.KEY_CODE_CHANNEL_LIST = KeyCodeConstantGiec.KEY_CODE_CHANNEL_LIST;
            KeyCode.KEY_CODE_DOWN = KeyCodeConstantGiec.KEY_CODE_DOWN;
            KeyCode.KEY_CODE_HDMI_IN = KeyCodeConstantGiec.KEY_CODE_HDMI_IN;
            KeyCode.KEY_CODE_LEFT = KeyCodeConstantGiec.KEY_CODE_LEFT;
            KeyCode.KEY_CODE_MANUAL_HEARTBEAT = KeyCodeConstantGiec.KEY_CODE_MANUAL_HEARTBEAT;
            KeyCode.KEY_CODE_NEXT_ADS = KeyCodeConstantGiec.KEY_CODE_NEXT_ADS;
            KeyCode.KEY_CODE_PLAY_PAUSE = KeyCodeConstantGiec.KEY_CODE_PLAY_PAUSE;
            KeyCode.KEY_CODE_PREVIOUS_ADS = KeyCodeConstantGiec.KEY_CODE_PREVIOUS_ADS;
            KeyCode.KEY_CODE_RIGHT = KeyCodeConstantGiec.KEY_CODE_RIGHT;
            KeyCode.KEY_CODE_SETTING = KeyCodeConstantGiec.KEY_CODE_SETTING;
            KeyCode.KEY_CODE_SHOW_APP_INSTALLED = KeyCodeConstantGiec.KEY_CODE_SHOW_APP_INSTALLED;
            KeyCode.KEY_CODE_SHOW_INFO = KeyCodeConstantGiec.KEY_CODE_SHOW_INFO;
            KeyCode.KEY_CODE_SHOW_QRCODE = KeyCodeConstantGiec.KEY_CODE_SHOW_QRCODE;
            KeyCode.KEY_CODE_SYSTEM_SETTING = KeyCodeConstantGiec.KEY_CODE_SYSTEM_SETTING;
            KeyCode.KEY_CODE_UP = KeyCodeConstantGiec.KEY_CODE_UP;
            KeyCode.KEY_CODE_UPLOAD_CHANNEL_INFO = KeyCodeConstantGiec.KEY_CODE_UPLOAD_CHANNEL_INFO;
            KeyCode.KEY_CODE_UDISK_UPDATE = KeyCodeConstantGiec.KEY_CODE_UDISK_UPDATE;
        }
    }
}
