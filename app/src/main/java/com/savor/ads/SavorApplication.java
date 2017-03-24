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
        mConnection = ServiceUtil.registerService(new ProjectOperationListener(this));
//        bindService(new Intent(ServiceUtil.ACTION_REMOTE_SERVICE), connection, Service.BIND_AUTO_CREATE);
        bindService(new Intent(this, RemoteService.class), mConnection, Service.BIND_AUTO_CREATE);
    }

    public void stopScreenProjectionService() {
        if (mConnection != null) {
            unbindService(mConnection);
        }
    }
}
