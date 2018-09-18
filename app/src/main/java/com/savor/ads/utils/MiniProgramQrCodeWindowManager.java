package com.savor.ads.utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.savor.ads.R;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.log.LogReportUtil;

import java.io.File;
import java.util.HashMap;

/**
 * Created by zhanghq on 2018/7/9.
 */

public class MiniProgramQrCodeWindowManager {
    private String ACTION_SHOW_START="1";
    private String ACTION_SHOW_END = "2";
    private Session session;
    private Handler mHandler = new Handler();
    private Context context;
    private LogReportUtil logReportUtil;
    private String mediaId;
    private String preMediaId;
    private static MiniProgramQrCodeWindowManager mInstance;
    final WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
    final WindowManager.LayoutParams wmBigParams = new WindowManager.LayoutParams();
    WindowManager mWindowManager;
    private LinearLayout mFloatLayout;
    private LinearLayout mBigFloatLayout;
    private boolean mIsAdded;
    private boolean mIsHandling;
    private String currentTime = null;

        public MiniProgramQrCodeWindowManager(Context mContext){
        this.context = mContext;
        session = Session.get(context);
        logReportUtil = LogReportUtil.get(context);
        if (mIsHandling) {
            return;
        }
        mIsHandling = true;

        final String ssid = AppUtils.getShowingSSID(context);


        //获取WindowManager
        mWindowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        //设置window type
        wmBigParams.type = wmParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置图片格式，效果为背景透明
        wmBigParams.format = wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmBigParams.flags = wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        wmBigParams.gravity = Gravity.LEFT|Gravity.CENTER_VERTICAL;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = DensityUtil.dip2px(context, 10);
        wmParams.y = DensityUtil.dip2px(context, 10);

        wmBigParams.x = DensityUtil.dip2px(context, 110);
        wmBigParams.y = DensityUtil.dip2px(context, 30);
        //设置悬浮窗口长宽数据
        wmParams.width = DensityUtil.dip2px(context, 188);
        wmParams.height = DensityUtil.dip2px(context, 188*1.2f);
        wmBigParams.width = DensityUtil.dip2px(context, 400);
        wmBigParams.height = DensityUtil.dip2px(context,400);
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_miniprogram_qrcode, null);
        mBigFloatLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_miniprogram_big_qrcode,null);
    }

    public static MiniProgramQrCodeWindowManager get(Context context){
        if (mInstance==null){
            mInstance = new MiniProgramQrCodeWindowManager(context);
        }
        return mInstance;

    }


    public void setCurrentPlayMediaId(String mediaid){
        this.mediaId = mediaid;
    }


    public void showQrCode(final Context context, final String url,final boolean isSmall) {
        LogUtils.d("showQrCode");
        if (TextUtils.isEmpty(url)) {
            LogUtils.e("Code is empty, will not show code window!!");
            return;
        }

        if (isSmall){
            final ImageView qrCodeIv = (ImageView) mFloatLayout.findViewById(R.id.iv_mini_program_qrcode);

            LogUtils.v("QrCodeWindowManager 开始addView");

            if (Looper.myLooper() == Looper.getMainLooper()) {
                addToWindow(context, url, qrCodeIv,isSmall);
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        addToWindow(context, url,qrCodeIv,isSmall);
                    }
                });
            }
        }else{
            final ImageView bigQRCodeIv = (ImageView) mBigFloatLayout.findViewById(R.id.iv_mini_program_big_qrcode);
            if (Looper.myLooper() == Looper.getMainLooper()) {
                addToWindow(context, url, bigQRCodeIv,isSmall);
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        addToWindow(context, url,bigQRCodeIv,isSmall);
                    }
                });
            }
        }

    }

    private void addToWindow(final Context context,final String url,final ImageView qrCodeIv,final boolean isSmall) {

        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.cache) + "getBoxQr.jpg";
        File tarFile = new File(path);
        if (Session.get(context).isDownloadMiniProgramIcon()&&tarFile.exists()){
            if (isSmall){
                ImageView qrCodeIV = (ImageView) mFloatLayout.findViewById(R.id.iv_mini_program_qrcode);

                Uri uri = Uri.fromFile(tarFile);
                qrCodeIV.setImageURI(uri);
            }else {
                ImageView qrCodeIV = (ImageView) mBigFloatLayout.findViewById(R.id.iv_mini_program_big_qrcode);

                Uri uri = Uri.fromFile(tarFile);
                qrCodeIV.setImageURI(uri);
            }

            handleWindowLayout();

        }else{
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
                    handleWindowLayout();
                    return false;
                }
            });
        }


    }

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (preMediaId.equals("17614")){
                    mWindowManager.removeViewImmediate(mBigFloatLayout);
                }else{
                    mWindowManager.removeViewImmediate(mFloatLayout);
                }
                mIsAdded = false;
                String id = currentTime;
                String box_mac = session.getEthernetMac();
                String media_id = preMediaId;
                String action = ACTION_SHOW_END;
                String log_time = String.valueOf(System.currentTimeMillis());;
//                sendMiniProgramIconShowLog(id,box_mac,media_id,log_time,action);
                Log.d("mpqcwm","sendMiniProgramIconShowLog(id="+id+"|box_mac="+box_mac+"|media_id="+media_id+"|log_time="+log_time+"|action="+action);
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

    private void handleWindowLayout(){
        try {
            if (mIsAdded&&context!=null&&mFloatLayout!=null) {
                //移除悬浮窗口
                if (preMediaId.equals("17614")){
                    mWindowManager.removeViewImmediate(mBigFloatLayout);
                }else{
                    mWindowManager.removeViewImmediate(mFloatLayout);
                }
                mIsAdded = false;
                String id = currentTime;
                String box_mac = session.getEthernetMac();
                String media_id = preMediaId;
                String log_time = String.valueOf(System.currentTimeMillis());
                String action = ACTION_SHOW_END;
//                sendMiniProgramIconShowLog(id,box_mac,media_id,log_time,action);
                Log.d("mpqcwm","sendMiniProgramIconShowLog(id="+id+"|box_mac="+box_mac+"|media_id="+media_id+"|log_time="+log_time+"|action="+action);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if (mFloatLayout.getParent() == null) {
            //设置悬浮窗口长宽数据
            if (mediaId.equals("17614")){
                mWindowManager.addView(mBigFloatLayout, wmBigParams);
            }else {
                mWindowManager.addView(mFloatLayout, wmParams);
            }

            LogUtils.v("QrCodeWindowManager addView SUCCESS");
//                    LogFileUtil.write("QrCodeWindowManager addView SUCCESS");
            currentTime = String.valueOf(System.currentTimeMillis());
            String id = currentTime;
            String box_mac = session.getEthernetMac();
            String media_id = mediaId;
            String action = ACTION_SHOW_START;
            String log_time = currentTime;
//            sendMiniProgramIconShowLog(id,box_mac,media_id,log_time,action);
            preMediaId = mediaId;
            Log.d("mpqcwm","sendMiniProgramIconShowLog(id="+id+"|box_mac="+box_mac+"|media_id="+media_id+"|log_time="+log_time+"|action="+action);
        }
        mHandler.removeCallbacks(mHideRunnable);
        mHandler.postDelayed(mHideRunnable,1000*60*2);
        mIsHandling = false;
        mIsAdded = true;
    }

    /**
     *
     * @param id 开始结束成对存在的流水号
     * @param box_mac 机顶盒mac
     * @param media_id 当前播放视频id
     * @param log_time 二维码动作时间
     * @param action 二维码动作是开始还是结束
     */
    private void sendMiniProgramIconShowLog(String id,String box_mac,String media_id,String log_time,String action){
        HashMap<String,Object> params = new HashMap<>();
        params.put("id",id);
        params.put("box_mac",box_mac);
        params.put("media_id",media_id);
        params.put("log_time",log_time);
        params.put("action",action);
        AppApi.postMiniProgramIconShowLog(context,requestListener,params);

    }

    ApiRequestListener requestListener = new ApiRequestListener() {
        @Override
        public void onSuccess(AppApi.Action method, Object obj) {

        }

        @Override
        public void onError(AppApi.Action method, Object obj) {

        }

        @Override
        public void onNetworkFailed(AppApi.Action method) {

        }
    };
}
