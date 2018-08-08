package com.savor.ads.service;

import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.megvii.facepp.sdk.Facepp;
import com.megvii.licensemanager.sdk.LicenseManager;
import com.savor.ads.R;
import com.savor.ads.activity.AdsPlayerActivity;
import com.savor.ads.bean.FaceLogBean;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.log.FaceDetectLogUtil;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.ShowMessage;
import com.savor.ads.utils.face.CameraHelper;
import com.savor.ads.utils.face.ConUtil;
import com.savor.ads.utils.face.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class FaceDetectService extends Service implements Camera.PreviewCallback {
    private final String TAG = "FaceDetectService";

    /**
     * 最小预览界面的分辨率
     */
    private static final int MIN_PREVIEW_PIXELS = 480 * 320;
    /**
     * 最大宽高比差
     */
    private static final double MAX_ASPECT_DISTORTION = 0.15;

    private CameraHelper mCameraHelper;
    private Camera cameraInst = null;
    private LicenseManager licenseManager;
    private Facepp facepp;

    private int min_face_size = 10;
    private int detection_interval = 25;
    private float roi_ratio = 0.8f;
    private Display mDisplay;
    private Camera.Size previewSize;
    private HandlerThread mHandlerThread = new HandlerThread("facepp");
    private Handler mHandler;

    /**当前观看的人脸和最近一次识别到的时间，Key为trackId*/
    private HashMap<Integer, Long> mFaceTrackTime = new HashMap<>();
    /**当前正在观看的人脸集合, Key为trackId*/
    private volatile ConcurrentHashMap<Integer, FaceLogBean> mWatchingMap = new ConcurrentHashMap<>();
    private static final int DETECT_THRESHOLD = 2 * 1000;

    private boolean isSuccess;
    float confidence;
    float pitch, yaw, roll;
    private SurfaceTexture mSurfaceTexture;

    public FaceDetectService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        licenseManager = new LicenseManager(this);
        facepp = new Facepp();
        mCameraHelper = new CameraHelper(this);

        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    DetectBinder binder = new DetectBinder();
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtils.d("onBind");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                requireLicense();
            }
        });
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.d("onUnbind");
        return super.onUnbind(intent);
    }

    public void notifyPlayStart(String mediaId, String mediaType) {
        if (mediaId != null && mWatchingMap != null) {
            for (FaceLogBean bean : mWatchingMap.values()) {
                bean.setMediaIds(mediaId);
                bean.setType(mediaType);
                bean.setStartTime(System.currentTimeMillis());
            }
        }
    }

    public void notifyPlayComplete(String mediaId) {
        if (mediaId != null && mWatchingMap != null) {
            for (FaceLogBean bean : mWatchingMap.values()) {
                bean.setEndTime(System.currentTimeMillis());
                FaceDetectLogUtil.getInstance(FaceDetectService.this).writeFaceRecord(bean);
            }
        }
    }

    private void requireLicense() {
        try {
            licenseManager.setExpirationMillis(Facepp.getApiExpirationMillis(this, ConUtil.getFileContent(this, R.raw
                    .megviifacepp_0_4_7_model)));

            String uuid = ConUtil.getUUIDString(this);
            long apiName = Facepp.getApiName();

            licenseManager.setAuthTimeBufferMillis(0);

            // TODO 测试KEY不管传什么时长授权有效期都只有1天，换成正式KEY后需把授权时长换成KEY对应的时长
            licenseManager.takeLicenseFromNetwork(uuid, Util.API_KEY, Util.API_SECRET, apiName,
                    LicenseManager.DURATION_30DAYS, "Landmark", "1", true, new LicenseManager.TakeLicenseCallback() {
                        @Override
                        public void onSuccess() {
                            authState(true);
                        }

                        @Override
                        public void onFailed(int i, byte[] bytes) {
                            authState(false);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void authState(boolean isSuccess) {
        if (isSuccess) {
            if (facepp != null) {
                initFaceDetect();
            }
        } else {
            LogUtils.e("授权失败！！将重试！！！");
            requireLicense();
        }
    }

    private void initFaceDetect() {
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mDisplay = window.getDefaultDisplay();

        if (null == cameraInst) {
            cameraInst = getCameraInstance(0);// Camera.open();
        }

        // 即便没有外接摄像头，打开后置摄像头cameraInst也不会为null，而是在startPreview时抛出异常
        if (cameraInst != null) {
            initCamera();
//            Angle = 360 - mICamera.Angle;
//            if (isBackCamera)
//                Angle = mICamera.Angle;


            int width = previewSize.width;
            int height = previewSize.height;

            int left = 0;
            int top = 0;
            int right = width;
            int bottom = height;
            if (true) {
                float line = height * roi_ratio;
                left = (int) ((width - line) / 2.0f);
                top = (int) ((height - line) / 2.0f);
                right = width - left;
                bottom = height - top;
            }

            String errorCode = facepp.init(this, ConUtil.getFileContent(this, R.raw.megviifacepp_0_4_7_model));
            if (TextUtils.isEmpty(errorCode)) {
                Facepp.FaceppConfig faceppConfig = facepp.getFaceppConfig();
                faceppConfig.interval = detection_interval;
                faceppConfig.minFaceSize = min_face_size;
                faceppConfig.roi_left = left;
                faceppConfig.roi_top = top;
                faceppConfig.roi_right = right;
                faceppConfig.roi_bottom = bottom;
//                if (isOneFaceTrackig)
//                    faceppConfig.one_face_tracking = 1;
//                else
                faceppConfig.one_face_tracking = 0;
//                String[] array = getResources().getStringArray(R.array.trackig_mode_array);
//                if (trackModel.equals(array[0]))
                faceppConfig.detectionMode = Facepp.FaceppConfig.DETECTION_MODE_TRACKING;
//                else if (trackModel.equals(array[1]))
//                    faceppConfig.detectionMode = Facepp.FaceppConfig.DETECTION_MODE_TRACKING_ROBUST;
//                else if (trackModel.equals(array[2]))
//                    faceppConfig.detectionMode = Facepp.FaceppConfig.DETECTION_MODE_TRACKING_FAST;

                facepp.setFaceppConfig(faceppConfig);
            } else {
                ShowMessage.showToast(this, errorCode);
//                mDialogUtil.showDialog(errorCode);
            }

            try {
                mSurfaceTexture = new SurfaceTexture(66);
                cameraInst.setPreviewTexture(mSurfaceTexture);
                cameraInst.setPreviewCallback(this);
                cameraInst.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                ShowMessage.showToast(this, "打开相机失败");
                LogFileUtil.writeException(new Throwable("Open camera failed! The exception is " + e.getMessage()));
            }
        } else {
            ShowMessage.showToast(this, "打开相机失败");
            LogFileUtil.writeException(new Throwable("Open camera failed!"));
//            mDialogUtil.showDialog(getResources().getString(R.string.camera_error));
        }
    }

    private Camera getCameraInstance(final int id) {
        Camera camera = null;
        try {
            camera = mCameraHelper.openCamera(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return camera;
    }

    private void initCamera() {
        Camera.Parameters parameters = cameraInst.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        if (previewSize == null) {
            previewSize = findBestPreviewResolution();
        }
//        Log.e("initCamera", "previewSize="+previewSize.width+"x"+previewSize.height);
        parameters.setPreviewSize(previewSize.width, previewSize.height);


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦
//        } else {
//            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        }

        //控制图像的正确显示方向
        setDisplayOrientation(parameters, cameraInst);
        try {
            cameraInst.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cameraInst.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
    }

    /**
     * 找出最适合的预览界面分辨率
     *
     * @return
     */
    private Camera.Size findBestPreviewResolution() {
        Camera.Parameters cameraParameters = cameraInst.getParameters();
        Camera.Size defaultPreviewResolution = cameraParameters.getPreviewSize();

        List<Camera.Size> rawSupportedSizes = cameraParameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            return defaultPreviewResolution;
        }

        // 按照分辨率从大到小排序
        List<Camera.Size> supportedPreviewResolutions = new ArrayList<Camera.Size>(rawSupportedSizes);
        Collections.sort(supportedPreviewResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        StringBuilder previewResolutionSb = new StringBuilder();
        for (Camera.Size supportedPreviewResolution : supportedPreviewResolutions) {
            previewResolutionSb.append(supportedPreviewResolution.width).append('x').append(supportedPreviewResolution.height)
                    .append(' ');
        }
        Log.d(TAG, "Supported preview resolutions: " + previewResolutionSb);


        // 移除不符合条件的分辨率
        double screenAspectRatio = (double) mDisplay.getWidth()
                / (double) mDisplay.getHeight();
//        double screenAspectRatio = (double) DensityUtil.getScreenWidth(mActivity)
//                / (double) DensityUtil.getScreenHeight(mActivity);
        Iterator<Camera.Size> it = supportedPreviewResolutions.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewResolution = it.next();
            int width = supportedPreviewResolution.width;
            int height = supportedPreviewResolution.height;

            // 移除低于下限的分辨率，尽可能取高分辨率
            if (width * height < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            // 在camera分辨率与屏幕分辨率宽高比不相等的情况下，找出差距最小的一组分辨率
            // 由于camera的分辨率是width>height，我们设置的portrait模式中，width<height
            // 因此这里要先交换然preview宽高比后在比较
            boolean isCandidatePortrait = width > height;
            int maybeFlippedWidth = isCandidatePortrait ? height : width;
            int maybeFlippedHeight = isCandidatePortrait ? width : height;
            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            // 找到与屏幕分辨率完全匹配的预览界面分辨率直接返回
//            if (maybeFlippedWidth == DensityUtil.getScreenWidth(mActivity)
//                    && maybeFlippedHeight == DensityUtil.getScreenHeight(mActivity)) {
//                return supportedPreviewResolution;
//            }
            if (maybeFlippedWidth == mDisplay.getWidth()) {
                if (maybeFlippedHeight == mDisplay.getHeight()) {
                    return supportedPreviewResolution;
                } else if (maybeFlippedHeight < mDisplay.getHeight() && maybeFlippedHeight > maybeFlippedWidth) {
//                    mPreviewSurfaceView.getHolder().setFixedSize(maybeFlippedWidth, maybeFlippedHeight);
                    return supportedPreviewResolution;
                }
            }
        }

        // 如果没有找到合适的，并且还有候选的像素，则设置其中最大比例的，对于配置比较低的机器不太合适
        if (!supportedPreviewResolutions.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewResolutions.get(0);
            return largestPreview;
        }

        // 没有找到合适的，就返回默认的

        return defaultPreviewResolution;
    }

    //控制图像的正确显示方向
    private void setDisplayOrientation(Camera.Parameters parameters, Camera camera) {

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(0, cameraInfo);
        int screenRotation = 0;
        // 由于这里只支持竖屏，所以screenRotation恒为0
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                screenRotation = 0;
                break;
            case Surface.ROTATION_90:
                screenRotation = 90;
                break;
            case Surface.ROTATION_180:
                screenRotation = 180;
                break;
            case Surface.ROTATION_270:
                screenRotation = 270;
                break;
        }
        int displayDegrees = 0;
        int imageRotation = 0;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayDegrees = (cameraInfo.orientation + screenRotation) % 360;
            displayDegrees = (360 - displayDegrees) % 360;  // compensate the mirror
            imageRotation = (cameraInfo.orientation + 90 - screenRotation + 360) % 360;
        } else {  // back-facing
            displayDegrees = (cameraInfo.orientation - screenRotation + 360) % 360;
            imageRotation = (cameraInfo.orientation + 270 + screenRotation) % 360;
        }
//        parameters.set("orientation", "portrait");
        parameters.setRotation(imageRotation);
        if (Build.VERSION.SDK_INT >= 8) {
            camera.setDisplayOrientation(displayDegrees);
        }
    }

    /**标识当前是第几次识别*/
    long mFrameIndex = 0;
    /**上一次VID*/
    String mLastVid;
    FaceDetectListener faceDetectListener;

    public void setFaceDetectListener(FaceDetectListener faceDetectListener) {
        this.faceDetectListener = faceDetectListener;
    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        Log.d("-------Eye--------", "onPreviewFrame");
        if (isSuccess)
            return;
        isSuccess = true;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // 累加检测计数
                if (mFrameIndex >= Long.MAX_VALUE) {
                    mFrameIndex = 1;
                } else {
                    mFrameIndex++;
                }

                if (facepp == null)
                    return;

                // 调SDK方法检测人脸
                int width = previewSize.width;
                int height = previewSize.height;
                final Facepp.Face[] faces = facepp.detect(data, width, height, Facepp.IMAGEMODE_NV21);

                HashMap<Integer, Long> newMap = new HashMap<Integer, Long>();
                if (faces != null) {
                    confidence = 0.0f;
                    Log.d("-------Eye--------", "face count=" +faces.length);
                    if (faces.length >= 0) {
                        // 检测到人脸

                        // 获取当前正在播放的视频ID
                        MediaLibBean mediaLibBean = null;
                        Activity ac = ActivitiesManager.getInstance().getCurrentActivity();
                        if (ac != null && ac instanceof AdsPlayerActivity) {
                            mediaLibBean = ((AdsPlayerActivity) ac).getCurrentMedia();
                        }

                        for (int c = 0; c < faces.length; c++) {
//                    if (is106Points)
//                        facepp.getLandmark(faces[c], Facepp.FPP_GET_LANDMARK106);
//                    else
                            facepp.getLandmark(faces[c], Facepp.FPP_GET_LANDMARK81);

//                    if (is3DPose) {
//                        facepp.get3DPose(faces[c]);
//                    }

                            Facepp.Face face = faces[c];

                            pitch = faces[c].pitch;        // 低、抬头弧度
                            yaw = faces[c].yaw;            // 左右转头弧度
                            roll = faces[c].roll;
                            confidence = faces[c].confidence;

                            // add code fragment start
                            if (isOverlap(face)) {
                                // 不认为是有效的观看姿态
                                Log.e("---------------------", "检测到侧脸！！！！");

                            } else {
                                long now = System.currentTimeMillis();

                                if (mFaceTrackTime.containsKey(face.trackID)) {
                                    long time = mFaceTrackTime.get(face.trackID);
                                    newMap.put(face.trackID, time);
                                    if (now - time > DETECT_THRESHOLD) {
                                        // 停留时长超过阈值，认为符合正在观看
                                        if (!mWatchingMap.containsKey(face.trackID)) {
                                            // 新增的观看者
                                            if (mediaLibBean != null && !TextUtils.isEmpty(mediaLibBean.getVid())) {
                                                FaceLogBean faceLogBean = new FaceLogBean();
                                                faceLogBean.setUuid(UUID.randomUUID().toString());
                                                faceLogBean.setStartTime(System.currentTimeMillis() - DETECT_THRESHOLD);
                                                faceLogBean.setNewestFrameIndex(mFrameIndex);
                                                faceLogBean.setTrackId(face.trackID);
                                                faceLogBean.setMediaIds(mediaLibBean.getVid());
                                                faceLogBean.setType(mediaLibBean.getType());
                                                mWatchingMap.put(face.trackID, faceLogBean);
                                            }
                                        } else {
                                            // 持续的观看者
                                            FaceLogBean faceLogBean = mWatchingMap.get(face.trackID);
                                            faceLogBean.setNewestFrameIndex(mFrameIndex);
                                        }
                                    }
                                } else {
                                    newMap.put(face.trackID, now);
                                }
                            }

                            Log.d("-------Eye--------", "left eye at " + "(" + face.points[0].x + ", " + face.points[0].y + ")" +
                                    " right eye at " + "(" + face.points[9].x + "," + face.points[9].y + ")" +
                                    "pitch = " + pitch + " yaw = " + yaw + " roll = " + roll + " confidence = " + confidence);
                            // add code fragment end

                        }

                    } else {
                        pitch = 0.0f;
                        yaw = 0.0f;
                        roll = 0.0f;
                    }
                }

                mFaceTrackTime = newMap;

                for (Integer trackId :
                        mWatchingMap.keySet()) {
                    FaceLogBean faceLogBean = mWatchingMap.get(trackId);
                    if (faceLogBean.getNewestFrameIndex() != mFrameIndex) {
                        // 当前人脸在最近一次识别中没有被识别到，认为该人脸已观看结束，记录日志
                        faceLogBean.setEndTime(System.currentTimeMillis());
                        FaceDetectLogUtil.getInstance(FaceDetectService.this).writeFaceRecord(faceLogBean);

                        mWatchingMap.remove(trackId);
                    }
                }

                if (faceDetectListener != null) {
                    faceDetectListener.onFaceChange(mWatchingMap);
                }
                isSuccess = false;
            }
        });
    }

    private boolean isOverlap(Facepp.Face face) {
        boolean overlap = false;
        double distanceLeftEye = Math.hypot(face.points[1].x - face.points[2].x, face.points[1].y - face.points[2].y);
        double distanceRightEye = Math.hypot(face.points[10].x - face.points[11].x, face.points[10].y - face.points[11].y);
        Log.d("-----", "distance=" + distanceLeftEye);
        // 侧头角度大于70度或仰头大于45度时认为不在观看
//        if (/*Math.abs(yaw) > Math.PI / 3 || */
//                Math.abs(pitch) > Math.PI / 4 || distanceLeftEye <= face.rect.width() / 10 || distanceRightEye <= face.rect.width() / 10) {
//            overlap = true;
//        }
        if ( Math.abs(yaw) > Math.PI / 3 || Math.abs(pitch) > Math.PI / 5) {
            overlap = true;
            LogFileUtil.write("----------------   检测到低头或仰头！！！！");
        }else if ( distanceLeftEye <= face.rect.width() / 10 || distanceRightEye <= face.rect.width() / 10) {
            overlap = true;
            LogFileUtil.write("----------------   检测到侧脸！！！！");
        }

        return overlap;
    }

    @Override
    public void onDestroy() {
        LogUtils.d("onDestroy");
        super.onDestroy();
        new Thread(new Runnable() {
            public void run() {
                if (cameraInst != null) {
                    LogUtils.d("Will stop and release camera");
                    try {
                        cameraInst.setPreviewCallback(null);
                        LogUtils.d("Will stopPreview");
                        cameraInst.stopPreview();
//                        cameraInst.unlock();
                        LogUtils.d("Will release");
                        cameraInst.release();
                        cameraInst = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (mSurfaceTexture != null) {
                    mSurfaceTexture.release();
                    mSurfaceTexture = null;
                }

                if (facepp != null) {
                    LogUtils.d("Will release facepp");
                    facepp.release();
                    facepp = null;
                }

                // 服务停止时说明已不在轮播状态，记录日志
                for (Integer trackId :
                        mWatchingMap.keySet()) {
                    FaceLogBean faceLogBean = mWatchingMap.get(trackId);
                    faceLogBean.setEndTime(System.currentTimeMillis());
                    FaceDetectLogUtil.getInstance(FaceDetectService.this).writeFaceRecord(faceLogBean);

                    mWatchingMap.remove(trackId);
                }
            }
        }).start();
    }

    public class DetectBinder extends Binder {
        public FaceDetectService getService() {
            return FaceDetectService.this;
        }
    }

    public interface FaceDetectListener {
        void onFaceChange(ConcurrentHashMap<Integer, FaceLogBean> watchingFaces);
    }
}
