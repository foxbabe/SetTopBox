package com.savor.ads.utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.Image;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.savor.ads.R;
import com.savor.ads.core.Session;

/**
 * Created by zhanghq on 2018/7/9.
 */

public class MiniProgramQrCodeWindowManager {

    private Handler mHandler = new Handler();
    private Context context;
    WindowManager mWindowManager;
    private LinearLayout mFloatLayout;

    private boolean mIsAdded;
    private boolean mIsHandling;

    public MiniProgramQrCodeWindowManager(Context mContext){
        this.context = mContext;
    }

    public void showQrCode(final Context context, final String url) {
        LogUtils.d("showQrCode");
        if (TextUtils.isEmpty(url)) {
            LogUtils.e("Code is empty, will not show code window!!");
            return;
        }
//        Session.get(context).setAuthCode(code);

//        mHandler.removeCallbacks(mHideRunnable);
//        mHandler.postDelayed(mHideRunnable, 10 * 1000);

        if (mIsHandling||mIsAdded) {
            return;
        }
        mIsHandling = true;

        final String ssid = AppUtils.getShowingSSID(context);

        final WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
        //获取WindowManager
        mWindowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        //设置window type
        wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置图片格式，效果为背景透明
        wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = DensityUtil.dip2px(context, 10);
        wmParams.y = DensityUtil.dip2px(context, 10);

        //设置悬浮窗口长宽数据
        wmParams.width = DensityUtil.dip2px(context, 188);
        wmParams.height = DensityUtil.dip2px(context, 188*1.2f);

        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_miniprogram_qrcode, null);

        final ImageView qrCodeIv = (ImageView) mFloatLayout.findViewById(R.id.iv_mini_program_qrcode);

        LogUtils.v("QrCodeWindowManager 开始addView");
//        LogFileUtil.write("QrCodeWindowManager 开始addView");
        if (Looper.myLooper() == Looper.getMainLooper()) {
            addToWindow(context, url, qrCodeIv, wmParams);
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    addToWindow(context, url,qrCodeIv,  wmParams);
                }
            });
        }
    }

    private void addToWindow(final Context context,final String url,final ImageView qrCodeIv,final WindowManager.LayoutParams wmParams) {

        GlideImageLoader.loadImageWithoutCache(context, url, qrCodeIv, new RequestListener() {
            @Override
            public boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
                mIsHandling = false;
                mIsAdded = false;
                ShowMessage.showToast(context, "加载二维码失败");
                return false;
            }

            @Override
            public boolean onResourceReady(Object resource, Object model, Target target, boolean isFromMemoryCache, boolean isFirstResource) {

                if (mFloatLayout.getParent() == null) {
                    mWindowManager.addView(mFloatLayout, wmParams);
                    LogUtils.v("QrCodeWindowManager addView SUCCESS");
//                    LogFileUtil.write("QrCodeWindowManager addView SUCCESS");
                }
                mHandler.removeCallbacks(mHideRunnable);
                mHandler.postDelayed(mHideRunnable,1000*60*2);
                mIsHandling = false;
                mIsAdded = true;
                return false;
            }
        });
    }

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (context!=null&&mFloatLayout!=null) {
                    //移除悬浮窗口
                    mWindowManager.removeViewImmediate(mFloatLayout);
                }

            }catch (Exception e){
                e.printStackTrace();
            }


        }
    };

    public void hideQrCode() {
        if (mIsAdded) {
            mIsAdded = false;
            mHandler.removeCallbacks(mHideRunnable);
            mHandler.post(mHideRunnable);
        }



    }
}
