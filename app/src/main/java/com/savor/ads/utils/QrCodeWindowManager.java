package com.savor.ads.utils;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.savor.ads.R;
import com.savor.ads.core.Session;

import java.io.File;

/**
 * Created by zhanghq on 2016/12/10.
 */

public class QrCodeWindowManager {

    private Handler mHandler = new Handler();

    WindowManager mWindowManager;
    private LinearLayout mFloatLayout;

    private boolean mIsAdded;
    private boolean mIsHandling;

    public void showQrCode(final Context context) {
        LogUtils.e("showQrCode");
        mHandler.removeCallbacks(mHideRunnable);
        mHandler.postDelayed(mHideRunnable, 10 * 1000);
        if (mIsAdded || mIsHandling) {
            return;
        }
        mIsHandling = true;
        // 开始创建二维码
        final String filePath = AppUtils.getSDCardPath() + "qrcode.jpg";
        LogUtils.v("QrCodeWindowManager 开始拼接二维码内容");
        LogFileUtil.write("QrCodeWindowManager 开始拼接二维码内容");
//        String boxUrl = "192.168.43.1&" + Session.get(context).getBoiteId() + "&" + Session.get(context).getRoomId();
//        String boxUrl = AppUtils.getWifiApIp() + "&" + Session.get(context).getBoiteId() + "&" + Session.get(context).getRoomId();

        LogUtils.v("QrCodeWindowManager 开始获取SSID");
        LogFileUtil.write("QrCodeWindowManager 开始获取SSID");
        String ssid = "";
        if (AppUtils.isWifiEnabled(context)) {
            ssid = AppUtils.getWifiName(context);
        } else {
            ssid = AppUtils.getWifiApName(context);
        }
        LogUtils.v("QrCodeWindowManager 开始获取AP IP");
        LogFileUtil.write("QrCodeWindowManager 开始获取AP IP");
        String boxUrl = ConstantValues.APP_DOWN_LINK + "?" +
                "ip=" + AppUtils.getWifiApIp() + "&bid=" + Session.get(context).getBoiteId() +
                "&rid=" + Session.get(context).getRoomId() + "&sid=" + ssid;
        File file = new File(filePath);
        if (!boxUrl.equals(ConstantValues.QRCODE_CONTENT) || !file.exists()) {
            if (file.exists()) {
                file.delete();
            }
            LogUtils.v("QrCodeWindowManager 开始创建二维码图片");
            LogFileUtil.write("QrCodeWindowManager 开始创建二维码图片");
            boolean success = BarcodeUtil.createQRImage(boxUrl, 500, 500,
                    BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_redian), filePath);
            // 二维码创建失败则直接返回
            if (!success) {
                LogFileUtil.write("QrCodeWindowManager 创建二维码图片失败");
                mIsHandling = false;
                return;
            }
        }
        ConstantValues.QRCODE_CONTENT = boxUrl;

        final WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        //获取WindowManager
        mWindowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        //设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = DensityUtil.dip2px(context, 60);
        wmParams.y = DensityUtil.dip2px(context, 60);

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_qrcode, null);

        final ImageView qrCodeIv = (ImageView) mFloatLayout.findViewById(R.id.iv_qrcode);
        final TextView wifiNameTv = (TextView) mFloatLayout.findViewById(R.id.tv_wifi_name);

        LogUtils.v("QrCodeWindowManager 开始addView");
        LogFileUtil.write("QrCodeWindowManager 开始addView");
        if (Looper.myLooper() == Looper.getMainLooper()) {
            addToWindow(context, filePath, qrCodeIv, wifiNameTv, wmParams, ssid);
        } else {
            final String finalSsid = ssid;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    addToWindow(context, filePath, qrCodeIv, wifiNameTv, wmParams, finalSsid);
                }
            });
        }
    }

    private void addToWindow(final Context context, String filePath, ImageView qrCodeIv, TextView wifiNameTv,
                             final WindowManager.LayoutParams wmParams, String ssid) {
        GlideImageLoader.loadImageWithoutCache(context, filePath, qrCodeIv, new RequestListener() {
            @Override
            public boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
                mIsHandling = false;
                ShowMessage.showToast(context, "加载二维码失败");
                return false;
            }

            @Override
            public boolean onResourceReady(Object resource, Object model, Target target, boolean isFromMemoryCache, boolean isFirstResource) {

                if (mFloatLayout.getParent() == null) {
                    mWindowManager.addView(mFloatLayout, wmParams);
                }

                mIsHandling = false;
                mIsAdded = true;
                return false;
            }
        });
        if (AppUtils.isWifiEnabled(context)) {
            wifiNameTv.setText("WiFi:" + ssid);
        } else {
            wifiNameTv.setText("WiFi:" + ssid);
        }
    }

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mIsHandling = false;
            if (mIsAdded) {
                mIsAdded = false;
                hideQrCode();
            }
        }
    };

    public void hideQrCode() {
        if (mFloatLayout.getParent() != null) {
            //移除悬浮窗口
            mWindowManager.removeViewImmediate(mFloatLayout);
        }
    }
}
