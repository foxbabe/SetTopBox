package com.savor.ads.activity;

import android.graphics.Bitmap;
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

import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.QueryPosBySessionIdResponseVo;
import com.jar.savor.box.vo.RotateResponseVo;
import com.jar.savor.box.vo.SeekResponseVo;
import com.jar.savor.box.vo.VolumeResponseVo;
import com.savor.ads.R;
import com.savor.ads.SavorApplication;
import com.savor.ads.customview.CircleProgressBar;
import com.savor.ads.customview.SavorVideoView;
import com.savor.ads.log.LogReportUtil;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.DensityUtil;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCodeConstant;
import com.savor.ads.utils.LogUtils;

import java.util.ArrayList;

public class ScreenProjectionActivity extends BaseActivity {

    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_MEDIA_ID = "extra_vid";
    public static final String EXTRA_VIDEO_POSITION = "extra_video_position";
    public static final String EXTRA_IMAGE_ROTATION = "extra_image_rotation";
    public static final String EXTRA_IS_THUMBNAIL = "extra_is_thumbnail";
    public static final String EXTRA_IMAGE_TYPE = "extra_image_type";

    /**
     * 投屏静止状态持续时间，超时自动退出投屏
     */
    private static final int PROJECT_DURATION = 1000 * 30 * 5;
    /**
     * 文件投屏持续时间
     */
    private static final int PROJECT_DURATION_FILE = 1000 * 60 * 5;

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
    private String mMediaId;
    /**
     * 是否是缩略图（只有投图会传进来）
     */
    private boolean mIsThumbnail;
    /**
     * 图片类型
     * 1：普通图片；
     * 2：文件图片；
     * 3：幻灯片图片；
     */
    private int mImageType;

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
            LogUtils.e("mExitProjectionRunnable " + ScreenProjectionActivity.this.hashCode());
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
     * 视频初始位置
     */
    private int mVideoInitPosition;
    /**
     * 日志用的投屏动作记录标识
     */
    private String mUUID;

    private boolean mIsFirstResume = true;

    private int mCurrentVolume = 60;
    private String mType;
    private String mInnerType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.e("onCreate " + this.hashCode());
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

            mappingLogType();

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
        mMediaId = bundle.getString(EXTRA_MEDIA_ID, "");
        mVideoInitPosition = bundle.getInt(EXTRA_VIDEO_POSITION);
        mImageRotationDegree = bundle.getInt(EXTRA_IMAGE_ROTATION);
        mIsThumbnail = bundle.getBoolean(EXTRA_IS_THUMBNAIL, true);
        mImageType = bundle.getInt(EXTRA_IMAGE_TYPE);
    }

    private void exitProjection() {
        LogUtils.e("will exitProjection " + this.hashCode());
        mIsBeenStopped = true;
        if (ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) ||
                ConstantValues.PROJECT_TYPE_VIDEO.equals(mProjectType)) {
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
            mSavorVideoView.setMediaFiles(list, 0, mVideoInitPosition * 1000);
        } else if (ConstantValues.PROJECT_TYPE_VIDEO.equals(mProjectType)) {
            // 视频投屏
            mSavorVideoView.setVisibility(View.VISIBLE);
            mImageArea.setVisibility(View.GONE);

            ArrayList<String> list = new ArrayList<>();
            list.add(mMediaPath);
            mSavorVideoView.release();
            mSavorVideoView.setMediaFiles(list, 0, mVideoInitPosition * 1000);
        } else if (ConstantValues.PROJECT_TYPE_PICTURE.equals(mProjectType)) {
            // 图片投屏
            mSavorVideoView.setVisibility(View.GONE);
            mSavorVideoView.release();
            mImageArea.setVisibility(View.VISIBLE);

            // 展示图片
            if (GlobalValues.CURRENT_PROJECT_BITMAP != null) {
                if (mImageView.getDrawable() != null) {
                    if (mImageView.getDrawable() instanceof BitmapDrawable) {
                        BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();
                        if (bitmapDrawable.getBitmap() != GlobalValues.CURRENT_PROJECT_BITMAP) {
                            bitmapDrawable.getBitmap().recycle();
                        }
                    }
                }

                // 图片分辨率过高的话ImageView加载会黑屏，这里缩小后再加载
                if (GlobalValues.CURRENT_PROJECT_BITMAP.getWidth() > DensityUtil.getScreenRealSize(this).x) {
                    GlobalValues.CURRENT_PROJECT_BITMAP = Bitmap.createScaledBitmap(GlobalValues.CURRENT_PROJECT_BITMAP,
                            DensityUtil.getScreenRealSize(this).x,
                            GlobalValues.CURRENT_PROJECT_BITMAP.getHeight() * DensityUtil.getScreenRealSize(this).x / GlobalValues.CURRENT_PROJECT_BITMAP.getWidth(), true);
                }
                mImageView.setImageBitmap(GlobalValues.CURRENT_PROJECT_BITMAP);
            }

            if (mIsThumbnail) {
                // 只有当传过来是缩略图时才去重置ImageView状态
                mImageView.setRotation(0);
                mImageView.setScaleX(1);
                mImageView.setScaleY(1);
                rotatePicture();
            }
        }

        if (!ConstantValues.PROJECT_TYPE_PICTURE.equals(mProjectType) || mIsThumbnail) {
            LogReportUtil.get(mContext).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "start", mType, mMediaId,
                    GlobalValues.CURRENT_PROJECT_DEVICE_ID, mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(),
                    mInnerType);
        }

        if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_NAME)) {
            mProjectTipTv.setText(GlobalValues.CURRENT_PROJECT_DEVICE_NAME + "正在投屏");
            mProjectTipTv.setVisibility(View.VISIBLE);
        } else {
            mProjectTipTv.setVisibility(View.GONE);
        }

        // 只有是投图片时才开始计划定时退出投屏
        if (ConstantValues.PROJECT_TYPE_PICTURE.equals(mProjectType) && mIsThumbnail) {
            rescheduleToExit(true);
        }
    }

    /**
     * 设置新投放源
     *
     * @param bundle
     */
    public void setNewProjection(Bundle bundle) {
        // mContentUUID不为空说明之前有一次互动，先记一次end
        if (!TextUtils.isEmpty(mMediaId) && !TextUtils.isEmpty(mUUID) &&
                (!ConstantValues.PROJECT_TYPE_PICTURE.equals(mProjectType) || mIsThumbnail)) {
            LogReportUtil.get(mContext).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "end", mType, mMediaId,
                    GlobalValues.CURRENT_PROJECT_DEVICE_ID, mSession.getVersionName(), mSession.getAdvertMediaPeriod(),
                    mSession.getMulticastMediaPeriod(), mInnerType);
        }

        handleBundleData(bundle);

        mappingLogType();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                handleProjectRequest();
            }
        });
    }

    private void mappingLogType() {
        if (TextUtils.isEmpty(mUUID)) {
            mUUID = String.valueOf(System.currentTimeMillis());
        }
        if (ConstantValues.PROJECT_TYPE_PICTURE.equals(mProjectType)) {
            mType = "projection";
            switch (mImageType) {
                case 1:
                    mInnerType = "pic";
                    if (mIsThumbnail) {
                        mMediaId = String.valueOf(System.currentTimeMillis());
                    }
                    break;
                case 2:
                    mInnerType = "file";
                    break;
                case 3:
                    mInnerType = "ppt";
                    break;
            }
        } else if (ConstantValues.PROJECT_TYPE_VIDEO.equals(mProjectType)) {
            mType = "projection";
            mInnerType = "video";
            mMediaId = String.valueOf(System.currentTimeMillis());
        } else if (ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType)) {
            mType = "vod";
            mInnerType = "video";
        }
    }

    /**
     * 更改进度
     *
     * @param position
     * @return
     */
    public SeekResponseVo seekTo(int position) {
        SeekResponseVo responseVo = new SeekResponseVo();
        if (!ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) &&
                !ConstantValues.PROJECT_TYPE_VIDEO.equals(mProjectType)) {
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("失败");
        } else {
            if (mSavorVideoView.isInPlaybackState()) {
                mSavorVideoView.seekTo(position * 1000);
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                responseVo.setInfo("成功");
            } else {
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                responseVo.setInfo("失败");
            }
        }
        return responseVo;
    }

    /**
     * 播放、暂停
     *
     * @param action
     * @return
     */
    public PlayResponseVo togglePlay(int action) {
        PlayResponseVo responseVo = new PlayResponseVo();
        responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
        responseVo.setInfo("操作失败");
        if (!ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) &&
                !ConstantValues.PROJECT_TYPE_VIDEO.equals(mProjectType)) {
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("失败");
        } else {
            if (0 == action) {
                // 暂停
                if (mSavorVideoView.tryPause()) {
                    responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                    responseVo.setInfo("暂停");

                    rescheduleToExit(true);
                }
            } else if (1 == action) {
                // 播放
                if (mSavorVideoView.tryPlay()) {
                    responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
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
     * @return
     */
    public void stop() {
        LogUtils.e("StopResponseVo will exitProjection " + this.hashCode());
        mIsBeenStopped = true;
        mHandler.post(mExitProjectionRunnable);
    }

    /**
     * 旋转投屏图片
     *
     * @param rotateDegree
     * @return
     */
    public RotateResponseVo rotate(int rotateDegree) {
        RotateResponseVo responseVo = new RotateResponseVo();
        if (ConstantValues.PROJECT_TYPE_PICTURE.equals(mProjectType)) {
            mImageRotationDegree = (mImageRotationDegree + rotateDegree) % 360;

            mHandler.post(mRotateImageRunnable);

            rescheduleToExit(true);

            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
            responseVo.setInfo("成功");
            responseVo.setRotateValue(rotateDegree);
        } else {
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("失败");
        }
        return responseVo;
    }

    /**
     * 查询播放进度
     *
     * @return
     */
    public Object query() {
        QueryPosBySessionIdResponseVo queryResponse = new QueryPosBySessionIdResponseVo();
        if (!ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) &&
                !ConstantValues.PROJECT_TYPE_VIDEO.equals(mProjectType)) {
            queryResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
        } else {
            queryResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
            int mCurrPos = mSavorVideoView.getCurrentPosition();

            queryResponse.setPos(mCurrPos);
        }

        return queryResponse;
    }

    public VolumeResponseVo volume(int action) {
        VolumeResponseVo responseVo = new VolumeResponseVo();
        switch (action) {
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
        responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
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
        LogUtils.e("rescheduleToExit scheduleNewOne=" + scheduleNewOne + " " + this.hashCode());
        mHandler.removeCallbacks(mExitProjectionRunnable);
        if (scheduleNewOne) {
            int duration = PROJECT_DURATION;
            if (2 == mImageType) {
                duration = PROJECT_DURATION_FILE;
            }
            mHandler.postDelayed(mExitProjectionRunnable, duration);
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
                ((SavorApplication) getApplication()).showQrCodeWindow(null);
                handled = true;
                break;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIsFirstResume &&
                (ConstantValues.PROJECT_TYPE_VIDEO_VOD.equals(mProjectType) || ConstantValues.PROJECT_TYPE_VIDEO.equals(mProjectType))) {
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
        LogUtils.e("onDestroy " + this.hashCode());
        super.onDestroy();

        // 清空消息队列
        mHandler.removeCallbacksAndMessages(null);

        // 记录业务日志
        LogReportUtil.get(mContext).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                String.valueOf(System.currentTimeMillis()), "end", mType, mMediaId, GlobalValues.CURRENT_PROJECT_DEVICE_ID,
                mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(), mInnerType);

        // 释放资源
        mSavorVideoView.release();
        if (mImageView.getDrawable() != null) {
            if (mImageView.getDrawable() instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();
                bitmapDrawable.getBitmap().recycle();
            }
        }

        // 重置全局变量
        GlobalValues.LAST_PROJECT_DEVICE_ID = GlobalValues.CURRENT_PROJECT_DEVICE_ID;
        GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
        GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
        GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
        GlobalValues.CURRENT_PROJECT_ID = null;
    }

    private SavorVideoView.PlayStateCallback mPlayStateCallback = new SavorVideoView.PlayStateCallback() {
        @Override
        public boolean onMediaComplete(int index, boolean isLast) {
            LogUtils.w("activity onMediaComplete " + this.hashCode());
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
