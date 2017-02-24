package com.savor.ads;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import java.text.DateFormat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jar.savor.box.ServiceUtil;
import com.jar.savor.box.services.RemoteService;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.QrCodeWindowManager;
import com.savor.ads.utils.ShellUtils;
import com.savor.ads.utils.WifiApUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 检测播放时间
                AppUtils.checkPlayTime(SavorApplication.this);

//                // 设置并打开热点
//                IntentFilter filter = new IntentFilter();
//                filter.addAction(WIFI_AP_STATE_CHANGED_ACTION);
//                registerReceiver(mWifiStateBroadcastReceiver, filter);
//
//                boolean success = AppUtils.setWifiApEnabled(SavorApplication.this, true);
//                LogFileUtil.writeApInfo("SavorApplication setWifiApEnabled " + (success ? "success" : "failed"));
//
//                mHandler.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        int state = AppUtils.getWifiAPState(SavorApplication.this);
//                        LogFileUtil.writeApInfo("SavorApplication wifi ap state =  " + state);
//                    }
//                }, 2000);
            }
        }).start();
    }

    //监听wifi热点的状态变化
    public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";
    public static final String EXTRA_WIFI_AP_STATE = "wifi_state";
    public static int WIFI_AP_STATE_DISABLING = 10;
    public static int WIFI_AP_STATE_DISABLED = 11;
    public static int WIFI_AP_STATE_ENABLING = 12;
    public static int WIFI_AP_STATE_ENABLED = 13;
    public static int WIFI_AP_STATE_FAILED = 14;
    //监听wifi热点状态变化
    private BroadcastReceiver mWifiStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int cstate = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1);
                if (cstate == WIFI_AP_STATE_ENABLED) {
                    LogUtils.e("SavorApplication BroadcastReceiver AP_STATE_ENABLED state=" + cstate);
                    LogFileUtil.writeApInfo("BroadcastReceiver AP_STATE_ENABLED state=" + cstate);

                    // 卸载wlan1网卡，发现wlan1存在时会导致热点连不上
                    ShellUtils.unmountWlan1();
                } else if (cstate == WIFI_AP_STATE_DISABLED || cstate == WIFI_AP_STATE_FAILED) {
                    LogUtils.e("BroadcastReceiver AP_STATE_FAILED state=" + cstate);
                    LogFileUtil.writeApInfo("SavorApplication BroadcastReceiver AP_STATE_FAILED state=" + cstate);
                }
            }
        }
    };

    private LinearLayout mFloatLayout;
    private TextView mUsedMemoryTv;
    private TextView mAllocatedMemoryTv;
    private TextView mSysTotalMemoryTv;
    private TextView mNetSpeedTv;
    private TextView mNetQualityTv;
    private WindowManager mWindowManager;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            setText();
            mHandler.sendEmptyMessageDelayed(0, 500);
            return true;
        }
    });

    private void showPerformanceInfo() {
        final WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        //获取WindowManager
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        //设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.RIGHT | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
//        wmParams.x = DensityUtil.dip2px(context, 60);
//        wmParams.y = DensityUtil.dip2px(context, 60);

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.layout_performance, null);

        mUsedMemoryTv = (TextView) mFloatLayout.findViewById(R.id.tv_app_used_memory);
        mAllocatedMemoryTv = (TextView) mFloatLayout.findViewById(R.id.tv_app_allocated_memory);
        mSysTotalMemoryTv = (TextView) mFloatLayout.findViewById(R.id.tv_sys_total_memory);
        mNetSpeedTv = (TextView) mFloatLayout.findViewById(R.id.tv_net_speed);
        mNetQualityTv = (TextView) mFloatLayout.findViewById(R.id.tv_net_quality);

        if (mFloatLayout.getParent() == null) {
            mWindowManager.addView(mFloatLayout, wmParams);
        }

        mHandler.sendEmptyMessageDelayed(0, 500);
    }

    private void setText() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memoryInfo);
        try {
            Runtime info = Runtime.getRuntime();
            mUsedMemoryTv.setText("已用内存：" + ((info.totalMemory() - info.freeMemory()) / (1024 * 1024)) + "M");
            mAllocatedMemoryTv.setText("可用总内存：" + (info.totalMemory() / (1024 * 1024)) + "M");
            mSysTotalMemoryTv.setText("系统可用内存：" + (memoryInfo.availMem / (1024 * 1024)) + "M");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 显示二维码
     */
    public void showQrCodeWindow() {
        mQrCodeWindowManager.showQrCode(this);
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
