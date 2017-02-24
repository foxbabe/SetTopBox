package com.savor.ads.activity;

import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.jar.savor.box.vo.PlayRequstVo;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.QueryPosBySessionIdResponseVo;
import com.jar.savor.box.vo.QueryRequestVo;
import com.jar.savor.box.vo.RotateRequestVo;
import com.jar.savor.box.vo.RotateResponseVo;
import com.jar.savor.box.vo.SeekRequestVo;
import com.jar.savor.box.vo.SeekResponseVo;
import com.jar.savor.box.vo.StopRequestVo;
import com.jar.savor.box.vo.StopResponseVo;
import com.jar.savor.box.vo.VolumeRequestVo;
import com.jar.savor.box.vo.VolumeResponseVo;
import com.savor.ads.R;
import com.savor.ads.SavorApplication;
import com.savor.ads.customview.CircleProgressBar;
import com.savor.ads.customview.SavorVideoView;
import com.savor.ads.log.LogReportUtil;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlideImageLoader;
import com.savor.ads.utils.KeyCodeConstant;
import com.savor.ads.utils.LogUtils;

import java.util.ArrayList;

public class ScreenProjectionActivity extends BaseActivity {

    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_VID = "extra_vid";
    public static final String EXTRA_VNAME = "extra_vname";
    public static final String EXTRA_DEVICE_ID = "extra_device_id";
    public static final String EXTRA_DEVICE_NAME = "extra_device_name";

    /**
     * 投屏静止状态持续时间，超时自动退出投屏
     */
    private static final int PROJECT_DURATION = 1000 * 60 * 10;

    private static final int PROJECT_TIP_DURATION = 1000 * 5;

    /**
     * 投屏类型
     */
    private String mProjectType;
    /**
     * 媒体文件位置
     */
    private String mMediaPath;
    /**
     * 视频ID（只有点播会传进来）
     */
    private String mVideoId;
    /**
     * 投屏设备ID
     */
    private String mDeviceId;
    /**
     * 投屏设备名
     */
    private String mDeviceName;
    /**
     * 视频名字（只有点播会传进来）
     */
    private String mVideoName;

    private Handler mHandler = new Handler();

    /**
     * 旋转图片Runnable
     */
    private Runnable mRotateImageRunnable = new Runnable() {
        @Override
        public void run() {
            rotatePicture();
        }
    };
    /**
     * 退出投屏Runnable
     */
    private Runnable mExitProjectionRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.w("mExitProjectionRunnable " + ScreenProjectionActivity.this.hashCode());
            exitProjection();
        }
    };
    /**
     * 投屏提示退出Runnable
     */
    private Runnable mProjectTipOutRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.w("mProjectTipOutRunnable " + ScreenProjectionActivity.this.hashCode());
            projectTipAnimateOut();
        }
    };
    /**
     * 隐藏音量显示Runnable
     */
    private Runnable mHideVolumeViewRunnable = new Runnable() {
        @Override
        public void run() {
            mVolumeRl.setVisibility(View.GONE);
        }
    };
    /**
     * 隐藏静音显示Runnable
     */
    private Runnable mHideMuteViewRunnable = new Runnable() {
        @Override
        public void run() {
            mMuteIv.setVisibility(View.GONE);
        }
    };

    private SavorVideoView mSavorVideoView;
    private RelativeLayout mImageArea;
    private ImageView mImageView;
    private RelativeLayout mImageLoadingTip;
    private CircleProgressBar mImageLoadingPb;
    private TextView mImageLoadingTv;
    private TextView mProjectTipTv;
    private ImageView mMuteIv;
    private RelativeLayout mVolumeRl;
    private TextView mVolumeTv;
    private ProgressBar mVolumePb;
    /**
     * 图片旋转角度
     */
    private int mImageRotationDegree;
    /**
     * 日志用的播放记录标识
     */
    private String mUUID;

    private boolean mIsFirstResume = true;

    private int mCurrentVolume = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_projection);

        findView();
        setView();
        init();

        handleIntent();
    }

    private void findView() {
        mSavorVideoView = (SavorVideoView) findViewById(R.id.video_view);
        mImageArea = (RelativeLayout) findViewById(R.id.rl_image);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mImageLoadingTip = (RelativeLayout) findViewById(R.id.rl_loading_tip);
        mImageLoadingPb = (CircleProgressBar) findViewById(R.id.pb_image);
        mImageLoadingTv = (TextView) findViewById(R.id.tv_loading_tip);
        mProjectTipTv = (TextView) findViewById(R.id.tv_project_tip);
        mMuteIv = (ImageView) findViewById(R.id.iv_mute);
        mVolumeRl = (RelativeLayout) findViewById(R.id.rl_volume_view);
        mVolumeTv = (TextView) findViewById(R.id.tv_volume);
        mVolumePb = (ProgressBar) findViewById(R.id.pb_volume);
    }

    private void setView() {
        mSavorVideoView.setIfShowPauseBtn(true);
        mSavorVideoView.setIfShowLoading(true);
        mSavorVideoView.setLooping(false);
        mSavorVideoView.setPlayStateCallback(mPlayStateCallback);
    }

    private void init() {
        setVolume(mCurrentVolume);
    }

    private void handleIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle == null) {
            LogUtils.w("handleIntent will exitProjection " + this.hashCode());
            exitProjection();
        } else {
            handleBundleData(bundle);

            handleProjectRequest();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        projectTipAnimateIn();
    }

    private void handleBundleData(Bundle bundle) {
        mProjectType = bundle.getString(EXTRA_TYPE);
        mMediaPath = bundle.getString(EXTRA_URL);
        mVideoId = bundle.getString(EXTRA_VID);
        mVideoName = bundle.getString(EXTRA_VNAME);
        mDeviceId = bundle.getString(EXTRA_DEVICE_ID);
        mDeviceName = bundle.getString(EXTRA_DEVICE_NAME);
    }

    private void exitProjection() {
        LogUtils.w("will exitProjection " + this.hashCode());
        mIsBeenStopped = true;
        if (ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) ||
                ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN.equals(mProjectType)) {
            mSavorVideoView.release();
        }
        finish();
        LogUtils.w("finish done " + this.hashCode());
    }

    public void projectTipAnimateIn() {
        mHandler.removeCallbacks(mProjectTipOutRunnable);
        mHandler.postDelayed(mProjectTipOutRunnable, PROJECT_TIP_DURATION);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            doAnimationIn();
        } else {
            mProjectTipTv.post(new Runnable() {
                @Override
                public void run() {
                    doAnimationIn();
                }
            });
        }
    }

    private void doAnimationIn() {
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 1, Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        animation.setDuration(1000);
        animation.setFillAfter(true);
        mProjectTipTv.startAnimation(animation);
    }

    private void projectTipAnimateOut() {
        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_PARENT, 1,
                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
        animation.setDuration(1000);
        animation.setFillAfter(true);
        mProjectTipTv.startAnimation(animation);
    }

    //region 来自投屏的交互

    /**
     * 处理投屏
     * 根据不同的投屏类型
     */
    private void handleProjectRequest() {
        if (ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType)) {
            // 点播
            mSavorVideoView.setVisibility(View.VISIBLE);
            mImageArea.setVisibility(View.GONE);

            ArrayList<String> list = new ArrayList<>();
            list.add(mMediaPath);
            mSavorVideoView.release();
            mSavorVideoView.setMediaFiles(list);
        } else if (ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN.equals(mProjectType)) {
            // 视频投屏
            mSavorVideoView.setVisibility(View.VISIBLE);
            mImageArea.setVisibility(View.GONE);

            ArrayList<String> list = new ArrayList<>();
            list.add(mMediaPath);
            mSavorVideoView.release();
            mSavorVideoView.setMediaFiles(list);
        } else if (ConstantValues.PROJECT_TYPE_PICTURE.equals(mProjectType)) {
            // 图片投屏
            mSavorVideoView.setVisibility(View.GONE);
            mSavorVideoView.release();
            mImageArea.setVisibility(View.VISIBLE);

            mImageRotationDegree = 0;
            mImageView.setRotation(0);
            mImageView.setScaleX(1);
            mImageView.setScaleY(1);
//            mImageLoadingTip.setVisibility(View.VISIBLE);
//            mImageLoadingPb.setVisibility(View.VISIBLE);
//            mImageLoadingTv.setText("图片加载中...");

            if (TextUtils.isEmpty(mMediaPath)) {
                if (ConstantValues.PROJECT_BITMAP != null) {
                    if (mImageView.getDrawable() != null) {
                        if (mImageView.getDrawable() instanceof BitmapDrawable) {
                            BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();
                            bitmapDrawable.getBitmap().recycle();
                        }
                    }
                    mImageView.setImageBitmap(ConstantValues.PROJECT_BITMAP);
                }
            } else {
                GlideImageLoader.clearView(mImageView);
                GlideImageLoader.loadImageWithoutCache(this, mMediaPath, mImageView, new RequestListener() {
                    @Override
                    public boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
                        LogUtils.e("图片加载失败1: " + mMediaPath);
                        // 失败后再去加载一次
                        GlideImageLoader.loadImageWithoutCache(mContext, mMediaPath, mImageView, new RequestListener() {
                            @Override
                            public boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
//                            mImageLoadingPb.setVisibility(View.GONE);
//                            mImageLoadingTv.setText("图片加载失败");
                                LogUtils.e("图片加载失败2: " + mMediaPath);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Object resource, Object model, Target target, boolean isFromMemoryCache, boolean isFirstResource) {
                                mImageLoadingTip.setVisibility(View.GONE);
                                return false;
                            }
                        });
                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Object resource, Object model, Target target, boolean isFromMemoryCache, boolean isFirstResource) {
                        mImageLoadingTip.setVisibility(View.GONE);
                        return false;
                    }
                });
            }

            mUUID = String.valueOf(System.currentTimeMillis());
            LogReportUtil.get(mContext).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "projection", "pic", mVideoId,
                    "", mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(),
                    "");
        } else {
            // PDF等其它
            mSavorVideoView.setVisibility(View.GONE);
            mSavorVideoView.release();
            mImageArea.setVisibility(View.GONE);
        }


        if (!TextUtils.isEmpty(mDeviceName)) {
            mProjectTipTv.setText(mDeviceName + "正在投屏");
            mProjectTipTv.setVisibility(View.VISIBLE);
        } else {
            mProjectTipTv.setVisibility(View.GONE);
        }

        rescheduleToExit(true);
    }

    /**
     * 设置新投放源
     *
     * @param bundle
     */
    public void setNewProjection(Bundle bundle) {
        handleBundleData(bundle);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                handleProjectRequest();
            }
        });

    }

    /**
     * 更改进度
     *
     * @param seekRequestVo
     * @return
     */
    public SeekResponseVo seekTo(SeekRequestVo seekRequestVo) {
        SeekResponseVo responseVo = new SeekResponseVo();
        if (!ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) &&
                !ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN.equals(mProjectType)) {
            responseVo.setResult(-1);
            responseVo.setInfo("失败");
        } else {
            if (mSavorVideoView.isInPlaybackState()) {
                mSavorVideoView.seekTo(seekRequestVo.getAbsolutepos() * 1000);
                responseVo.setResult(0);
                responseVo.setInfo("成功");
            } else {
                responseVo.setResult(-1);
                responseVo.setInfo("失败");
            }
        }
        return responseVo;
    }

    /**
     * 播放、暂停
     *
     * @param playRequestVo
     * @return
     */
    public PlayResponseVo togglePlay(PlayRequstVo playRequestVo) {
        PlayResponseVo responseVo = new PlayResponseVo();
        responseVo.setResult(-1);
        responseVo.setInfo("操作失败");
        if (!ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) &&
                !ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN.equals(mProjectType)) {
            responseVo.setResult(-1);
            responseVo.setInfo("失败");
        } else {
            if (0 == playRequestVo.getRate()) {
                // 暂停
                if (mSavorVideoView.tryPause()) {
                    responseVo.setResult(0);
                    responseVo.setInfo("暂停");

                    rescheduleToExit(true);
                }
            } else if (1 == playRequestVo.getRate()) {
                // 播放
                if (mSavorVideoView.tryPlay()) {
                    responseVo.setResult(0);
                    responseVo.setInfo("播放");

                    rescheduleToExit(false);
                }
            }
        }
        return responseVo;
    }

    private boolean mIsBeenStopped;

    public boolean isBeenStopped() {
        return mIsBeenStopped;
    }

    /**
     * 停止投屏
     *
     * @param stopRequestVo
     * @return
     */
    public StopResponseVo stop(StopRequestVo stopRequestVo) {
        LogUtils.w("StopResponseVo will exitProjection " + this.hashCode());
        mHandler.post(mExitProjectionRunnable);
        mIsBeenStopped = true;

        StopResponseVo stopResponseVo = new StopResponseVo();
        stopResponseVo.setResult(0);
        return stopResponseVo;
    }

    /**
     * 旋转投屏图片
     *
     * @param rotateRequestVo
     * @return
     */
    public RotateResponseVo rotate(RotateRequestVo rotateRequestVo) {
        RotateResponseVo responseVo = new RotateResponseVo();
        if (ConstantValues.PROJECT_TYPE_PICTURE.equals(mProjectType)) {
            mImageRotationDegree = (mImageRotationDegree + rotateRequestVo.getRotatevalue()) % 360;

            mHandler.post(mRotateImageRunnable);

            rescheduleToExit(true);

            responseVo.setResult(0);
            responseVo.setInfo("成功");
            responseVo.setRotateValue(rotateRequestVo.getRotatevalue());
        } else {
            responseVo.setResult(-1);
            responseVo.setInfo("失败");
            responseVo.setRotateValue(100);
        }
        return responseVo;
    }

    /**
     * 查询播放进度
     *
     * @param queryRequestVo
     * @return
     */
    public Object query(QueryRequestVo queryRequestVo) {
        Object obj = null;
        /*if ("all".equalsIgnoreCase(queryRequestVo.getWhat())) {

        } else */

        // 获取播放进度
        if (queryRequestVo != null && queryRequestVo.getWhat() != null &&
                (queryRequestVo.getWhat()).contains("pos")) {
            QueryPosBySessionIdResponseVo queryResponse = new QueryPosBySessionIdResponseVo();
            if (!ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) &&
                    !ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN.equals(mProjectType)) {
                queryResponse.setResult(-1);
            } else {
                queryResponse.setResult(0);
                int mCurrPos = mSavorVideoView.getCurrentPosition();

                queryResponse.setPos(mCurrPos);
//                if (mCurrPos == -1) {
//                    queryResponse.setResult(-1);
//                } else {
//                    queryResponse.setResult(0);
//                }
            }
            obj = queryResponse;
        }/* else if ((queryRequestVo.getWhat()).contains("buf")) {
            QueryBufferBySessionIdResponseVo bufferBySessionIdResponseVo = new QueryBufferBySessionIdResponseVo();
            if (mLoadingComplete) {
                bufferBySessionIdResponseVo.setResult(0);
            } else {
                bufferBySessionIdResponseVo.setResult(1);
            }

            obj = bufferBySessionIdResponseVo;
        }*/
        return obj;
    }

    public VolumeResponseVo volume(VolumeRequestVo volumeRequestVo) {
        VolumeResponseVo responseVo = new VolumeResponseVo();
        switch (volumeRequestVo.getAction()) {
            case 1:
                // 静音
                setVolume(0);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showMuteView();
                        showVolume(0);
                    }
                });
                break;
            case 2:
                // 取消静音
//                int volume2 = getVolume();
//                if (volume2 >= 0) {
//                    mCurrentVolume = volume2;
//                }
                setVolume(mCurrentVolume);

                mHandler.removeCallbacks(mHideMuteViewRunnable);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mMuteIv.setVisibility(View.GONE);
                        showVolume(mCurrentVolume);
                    }
                });
                break;
            case 3:
                // 音量减
//                int volume3 = getVolume();
//                if (volume3 >= 0) {
//                    mCurrentVolume = volume3;
//                }
                mCurrentVolume -= 5;
                if (mCurrentVolume < 0) {
                    mCurrentVolume = 0;
                }
                setVolume(mCurrentVolume);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showVolume(mCurrentVolume);
                    }
                });
                break;
            case 4:
                // 音量加
//                int volume4 = getVolume();
//                if (volume4 >= 0) {
//                    mCurrentVolume = volume4;
//                }
                mCurrentVolume += 5;
                if (mCurrentVolume > 100) {
                    mCurrentVolume = 100;
                }
                setVolume(mCurrentVolume);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showVolume(mCurrentVolume);
                    }
                });
                break;
        }
        responseVo.setResult(0);
        responseVo.setVol(mCurrentVolume);
        return responseVo;
    }

    private void showMuteView() {
        mMuteIv.setVisibility(View.VISIBLE);
        mHandler.removeCallbacks(mHideMuteViewRunnable);
        mHandler.postDelayed(mHideMuteViewRunnable, 1000 * 3);
    }

    private void showVolume(int currentVolume) {
        mVolumePb.setProgress(currentVolume);
        mVolumeTv.setText(currentVolume + "");
        mVolumeRl.setVisibility(View.VISIBLE);
        mHandler.removeCallbacks(mHideVolumeViewRunnable);
        mHandler.postDelayed(mHideVolumeViewRunnable, 1000 * 5);
    }

    /**
     * 重置定期退出页面计划
     *
     * @param scheduleNewOne 是否重置
     */
    private void rescheduleToExit(boolean scheduleNewOne) {
        mHandler.removeCallbacks(mExitProjectionRunnable);
        if (scheduleNewOne) {
            mHandler.postDelayed(mExitProjectionRunnable, PROJECT_DURATION);
        }
    }

    /**
     * 旋转图片
     */
    private void rotatePicture() {
        mImageView.setRotation(mImageRotationDegree);
        if (mImageView.getDrawable() != null) {
            if (mImageRotationDegree == 90 || mImageRotationDegree == 270) {
                int viewWidth = mImageView.getDrawable().getIntrinsicWidth();// mImageView.getWidth();
                int viewHeight = mImageView.getDrawable().getIntrinsicHeight();//mImageView.getHeight();
                mImageView.setScaleX(viewHeight / (float) viewWidth);
                mImageView.setScaleY(viewHeight / (float) viewWidth);
            } else {
                mImageView.setScaleX(1);
                mImageView.setScaleY(1);
            }
        }
//        mImageView.setRotation(mImageRotationDegree);
//        if ((mImageRotationDegree / 90) % 2 != 0) {
//            DisplayMetrics dm = new DisplayMetrics();
//            getWindowManager().getDefaultDisplay().getMetrics(dm);
//            int winWidth = dm.widthPixels;
//            int winHeight = dm.heightPixels;
//
//            int imgWidth = bmp.getWidth();
//            int imgHeight = bmp.getHeight();
//
//            if (imgWidth > imgHeight) {
//                float ratio1 = (float) imgWidth / (float) winHeight;
//                float ratio2 = (float) imgHeight / (float) winWidth;
//                float sampleSize = 1.0f;
//                if (ratio1 >= ratio2 && ratio1 >= 1.0f) {
//                    sampleSize = ratio1;
//                } else if (ratio2 > ratio1 && ratio2 >= 1.0f) {
//                    sampleSize = ratio2;
//                }
//
//
//                Matrix matrix = new Matrix();
//
//                matrix.setScale(1.0f / (float) sampleSize, 1.0f / (float) sampleSize, bmp.getWidth() / 2,
//                        bmp.getHeight() / 2);
//                Bitmap createBmp = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
//                Canvas canvas = new Canvas(createBmp);
//                Paint paint = new Paint();
//                canvas.drawBitmap(bmp, matrix, paint);
//                mImageView.setImageBitmap(createBmp);
//            }
//
//        } else {
//            mImageView.setImageBitmap(bmp);
//        }
    }
    //endregion


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case KeyCodeConstant.KEY_CODE_BACK:
                finish();
                handled = true;
                break;
            // 呼出二维码
            case KeyCodeConstant.KEY_CODE_SHOW_QRCODE:
                ((SavorApplication) getApplication()).showQrCodeWindow();
                handled = true;
                break;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIsFirstResume &&
                (ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) || ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN.equals(mProjectType))) {
            mSavorVideoView.onResume();
        }
        mIsFirstResume = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSavorVideoView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSavorVideoView.onStop();
    }

    @Override
    protected void onDestroy() {
        LogUtils.d("onDestroy " + this.hashCode());
        super.onDestroy();
        mSavorVideoView.release();
        if (mImageView.getDrawable() != null) {
            if (mImageView.getDrawable() instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();
                bitmapDrawable.getBitmap().recycle();
            }
        }
        GlideImageLoader.clearView(mImageView);
        mHandler.removeCallbacksAndMessages(null);
        ConstantValues.CURRENT_PROJECT_DEVICE_ID = null;
    }

    private SavorVideoView.PlayStateCallback mPlayStateCallback = new SavorVideoView.PlayStateCallback() {
        @Override
        public boolean onMediaComplete(int index, boolean isLast) {
            LogUtils.w("activity onMediaComplete " + this.hashCode());
            // 这里只是为了防止到这里的时候mUUID没值，正常mUUID肯定会在onMediaPrepared()中赋值
            if (TextUtils.isEmpty(mUUID)) {
                mUUID = String.valueOf(System.currentTimeMillis());
            }
            if (ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType)) {
                LogReportUtil.get(mContext).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "end", "vod", mVideoId,
                        "", mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(),
                        "");
            }
            exitProjection();
            return false;
        }

        @Override
        public boolean onMediaError(int index, boolean isLast) {
            LogUtils.w("activity onMediaError " + this.hashCode());
            exitProjection();
            return false;
        }

        @Override
        public void onMediaPrepared(int index) {
            // 准备播放新视频时产生一个新的UUID作为日志标识
            mUUID = String.valueOf(System.currentTimeMillis());
            String action = "";
            String type = "";
            if (ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType)) {
                action = "start";
                type = "vod";
            } else if (ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN.equals(mProjectType)) {
                action = "projection";
                type = "video";
            }
            LogReportUtil.get(mContext).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), action, type, mVideoId,
                    "", mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(),
                    "");
            rescheduleToExit(false);
        }

        @Override
        public void onMediaPause(int index) {
        }

        @Override
        public void onMediaResume(int index) {
        }
    };
}
