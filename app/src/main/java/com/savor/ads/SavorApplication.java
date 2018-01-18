package com.savor.ads;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;
import android.util.Log;

import com.jar.savor.box.ServiceUtil;
import com.jar.savor.box.services.RemoteService;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.ResponseErrorMessage;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCode;
import com.savor.ads.utils.KeyCodeConstant;
import com.savor.ads.utils.KeyCodeConstantGiec;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.QrCodeWindowManager;
import com.umeng.message.IUmengCallback;
import com.umeng.message.IUmengRegisterCallback;
import com.umeng.message.PushAgent;

import java.io.DataOutputStream;
import java.io.File;

/**
 * Created by Administrator on 2016/12/9.
 */

public class SavorApplication extends MultiDexApplication implements ApiRequestListener {

    private QrCodeWindowManager mQrCodeWindowManager;
    private ServiceConnection mConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        // 设置异常捕获处理类
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        // 初始化文件记录类
        LogFileUtil.init();
        // 映射真实健值
        mappingKeyCode();

        mQrCodeWindowManager = new QrCodeWindowManager();
        // 启动投屏类操作处理的Service
//        startScreenProjectionService();

        // 检测播放时间
        AppUtils.checkPlayTime(SavorApplication.this);

        initPush();
    }


    private Handler mHandler = new Handler();

    private void initPush() {
        if (AppUtils.isMstar()) {
            initMStarPush();
        } else {
            initGiecPush();
        }
    }

    private void initMStarPush() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                boolean isCopySuccess = false;
                if (getPackageName().equals(AppUtils.getProcessName(SavorApplication.this)) &&
                        !GlobalValues.IS_UPUSH_SO_COPY_SUCCESS) {
                    File innerLibDir = new File("/sdcard/inner_so/");
                    if (!innerLibDir.exists()) {
                        innerLibDir.mkdirs();
                    }
                    if (innerLibDir.exists()) {
                        FileUtils.copyFilesFromAssets(SavorApplication.this, "inner_so/", innerLibDir.getPath());
                    }

//                    File outerLibDir = new File("/sdcard/outer_so/");
//                    if (!outerLibDir.exists()) {
//                        outerLibDir.mkdirs();
//                    }
//                    if (outerLibDir.exists()) {
//                        FileUtils.copyFilesFromAssets(SavorApplication.this, "outer_so/", outerLibDir.getPath());
//                    }

                    Process proc = null;
                    try {
                        proc = Runtime.getRuntime().exec("su");

                        DataOutputStream dos = new DataOutputStream(proc.getOutputStream());
                        dos.writeBytes("mount -o remount,rw /system\n");
                        dos.flush();

//                            dos.writeBytes("chmod -R 755 /sdcard/outer_so/\n");
//                            dos.flush();
//                            Thread.sleep(100);

//                            dos.writeBytes("mkdir /system/priv-app/savormedia/lib/\n");
//                            dos.flush();
//                            Thread.sleep(100);
//
//                            dos.writeBytes("mkdir /system/priv-app/savormedia/lib/arm/\n");
//                            dos.flush();
//                            Thread.sleep(100);

                        dos.writeBytes("cp /sdcard/inner_so/* /data/data/com.savor.ads/lib/\n");
                        dos.flush();
                        Thread.sleep(2000);

                        dos.writeBytes("chmod -R 755 /data/data/com.savor.ads/lib/\n");
                        dos.flush();
                        Thread.sleep(200);

                        //                        dos.writeBytes("cp /sdcard/outer_so/ /system/lib/\n");
                        //                        dos.flush();
                        //                        Thread.sleep(2000);

                        dos.close();

                        GlobalValues.IS_UPUSH_SO_COPY_SUCCESS = true;
                        isCopySuccess = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        GlobalValues.IS_UPUSH_SO_COPY_SUCCESS = false;
                        isCopySuccess = false;
                    } finally {
                        if (proc != null) {
                            try {
                                proc.destroy();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    isCopySuccess = true;
                }

                LogFileUtil.write("Copy so file success? " + isCopySuccess);
                if (isCopySuccess) {
                    LogUtils.d("copy so success");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            PushAgent pushAgent = PushAgent.getInstance(SavorApplication.this);
                            pushAgent.setPushIntentServiceClass(UMessageIntentService.class);
                            //注册推送服务，每次调用register方法都会回调该接口
                            pushAgent.register(new IUmengRegisterCallback() {

                                @Override
                                public void onSuccess(String deviceToken) {
                                    //注册成功会返回device token
                                    Log.e("register", "UPush register success, deviceToken is " + deviceToken);
                                    LogFileUtil.write("UPush register success, deviceToken is " + deviceToken);

                                    GlobalValues.IS_UPUSH_REGISTER_SUCCESS = true;
                                    reportDeviceToken(deviceToken);

                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            PushAgent.getInstance(SavorApplication.this).onAppStart();
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(String s, String s1) {
                                    Log.e("register", "UPush register failed, s is " + s + ", s1 is " + s1);
                                    LogFileUtil.write("UPush register failed, s is " + s + ", s1 is " + s1);
                                    GlobalValues.IS_UPUSH_REGISTER_SUCCESS = false;
                                }
                            });
                        }
                    });
                }
            }
        }).start();
    }

    private void initGiecPush() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                boolean isCopySuccess = false;
                if (getPackageName().equals(AppUtils.getProcessName(SavorApplication.this)) &&
                        !GlobalValues.IS_UPUSH_SO_COPY_SUCCESS) {
                    File innerLibDir = new File("/sdcard/inner_so/"/*ConstantValues.APK_INSTALLED_PATH + "lib/arm/"*/);
                    if (!innerLibDir.exists()) {
                        innerLibDir.mkdirs();
                    }
                    if (innerLibDir.exists()) {
                        FileUtils.copyFilesFromAssets(SavorApplication.this, "inner_so/", innerLibDir.getPath());
                    }

//                    File outerLibDir = new File("/sdcard/outer_so/");
//                    if (!outerLibDir.exists()) {
//                        outerLibDir.mkdirs();
//                    }
//                    if (outerLibDir.exists()) {
//                        FileUtils.copyFilesFromAssets(SavorApplication.this, "outer_so/", outerLibDir.getPath());
//                    }

                    Process proc = null;
                    try {
                        proc = Runtime.getRuntime().exec("su");

                        DataOutputStream dos = new DataOutputStream(proc.getOutputStream());
                        dos.writeBytes("mount -o remount,rw /system\n");
                        dos.flush();

//                        dos.writeBytes("chmod -R 755 /sdcard/outer_so/\n");
//                        dos.flush();
//                        Thread.sleep(100);

                        dos.writeBytes("mkdir /system/priv-app/savormedia/lib/\n");
                        dos.flush();
                        Thread.sleep(100);

                        dos.writeBytes("mkdir /system/priv-app/savormedia/lib/arm/\n");
                        dos.flush();
                        Thread.sleep(100);

                        dos.writeBytes("cp /sdcard/inner_so/* /system/priv-app/savormedia/lib/arm/\n");
                        dos.flush();
                        Thread.sleep(2000);

                        dos.writeBytes("chmod -R 755 /system/priv-app/savormedia/lib/\n");
                        dos.flush();
                        Thread.sleep(200);

                        //                        dos.writeBytes("cp /sdcard/outer_so/ /system/lib/\n");
                        //                        dos.flush();
                        //                        Thread.sleep(2000);

                        dos.close();

                        GlobalValues.IS_UPUSH_SO_COPY_SUCCESS = true;
                        isCopySuccess = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        GlobalValues.IS_UPUSH_SO_COPY_SUCCESS = false;
                        isCopySuccess = false;
                    } finally {
                        if (proc != null) {
                            try {
                                proc.destroy();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    isCopySuccess = true;
                }

                LogFileUtil.write("Copy so file success? " + isCopySuccess);
                if (isCopySuccess) {
                    LogUtils.d("copy so success");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            PushAgent pushAgent = PushAgent.getInstance(SavorApplication.this);
                            pushAgent.setPushIntentServiceClass(UMessageIntentService.class);
                            //注册推送服务，每次调用register方法都会回调该接口
                            pushAgent.register(new IUmengRegisterCallback() {

                                @Override
                                public void onSuccess(String deviceToken) {
                                    //注册成功会返回device token
                                    Log.e("register", "UPush register success, deviceToken is " + deviceToken);
                                    LogFileUtil.write("UPush register success, deviceToken is " + deviceToken);

                                    GlobalValues.IS_UPUSH_REGISTER_SUCCESS = true;
                                    reportDeviceToken(deviceToken);

                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            PushAgent.getInstance(SavorApplication.this).onAppStart();
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(String s, String s1) {
                                    Log.e("register", "UPush register failed, s is " + s + ", s1 is " + s1);
                                    LogFileUtil.write("UPush register failed, s is " + s + ", s1 is " + s1);
                                    GlobalValues.IS_UPUSH_REGISTER_SUCCESS = false;
                                }
                            });
                        }
                    });
                }
            }
        }).start();
    }

    private void reportDeviceToken(String deviceToken) {
        AppApi.reportDeviceToken(this, this, deviceToken);
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

    /**
     * 映射真实健值
     */
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
            KeyCode.KEY_CODE_UDISK_COPY = KeyCodeConstant.KEY_CODE_UDISK_COPY;
            KeyCode.KEY_CODE_SHOW_PLAYLIST = KeyCodeConstant.KEY_CODE_SHOW_PLAYLIST;
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
            KeyCode.KEY_CODE_UDISK_COPY = KeyCodeConstantGiec.KEY_CODE_UDISK_COPY;
            KeyCode.KEY_CODE_SHOW_PLAYLIST = KeyCodeConstantGiec.KEY_CODE_SHOW_PLAYLIST;
        }
    }

    @Override
    public void onSuccess(AppApi.Action method, Object obj) {
        if (AppApi.Action.CP_POST_DEVICE_TOKEN_JSON.equals(method)) {
            LogUtils.d("Report DeviceToken onSuccess!");
            LogFileUtil.write("Report DeviceToken onSuccess!");
        }
    }

    @Override
    public void onError(AppApi.Action method, Object obj) {
        if (AppApi.Action.CP_POST_DEVICE_TOKEN_JSON.equals(method)) {
            String msg = "";
            if (obj instanceof ResponseErrorMessage) {
                msg = ((ResponseErrorMessage) obj).getMessage();
            }
            LogUtils.d("Report DeviceToken onError! msg is " + msg);
            LogFileUtil.write("Report DeviceToken onError! msg is " + msg);
        }
    }

    @Override
    public void onNetworkFailed(AppApi.Action method) {
        if (AppApi.Action.CP_POST_DEVICE_TOKEN_JSON.equals(method)) {
            LogUtils.d("Report DeviceToken onNetworkFailed!");
            LogFileUtil.write("Report DeviceToken onNetworkFailed!");
        }
    }
}
