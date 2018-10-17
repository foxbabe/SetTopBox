package com.savor.ads.callback;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.jar.savor.box.interfaces.OnRemoteOperationListener;
import com.jar.savor.box.vo.CodeVerifyBean;
import com.jar.savor.box.vo.HitEggResponseVo;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.PptRequestVo;
import com.jar.savor.box.vo.PptVideoRequestVo;
import com.jar.savor.box.vo.PrepareResponseVoNew;
import com.jar.savor.box.vo.QueryPosBySessionIdResponseVo;
import com.jar.savor.box.vo.ResponseT;
import com.jar.savor.box.vo.RotateResponseVo;
import com.jar.savor.box.vo.SeekResponseVo;
import com.jar.savor.box.vo.StopResponseVo;
import com.jar.savor.box.vo.VolumeResponseVo;
import com.savor.ads.SavorApplication;
import com.savor.ads.activity.LotteryActivity;
import com.savor.ads.activity.ScreenProjectionActivity;
import com.savor.ads.bean.AwardTime;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.projection.ProjectionManager;
import com.savor.ads.projection.action.AdvAction;
import com.savor.ads.projection.action.GreetingAction;
import com.savor.ads.projection.action.GreetingThenSpecialtyAction;
import com.savor.ads.projection.action.ImageAction;
import com.savor.ads.projection.action.PlayAction;
import com.savor.ads.projection.action.PptAction;
import com.savor.ads.projection.action.RotateAction;
import com.savor.ads.projection.action.SeekAction;
import com.savor.ads.projection.action.ShowEggAction;
import com.savor.ads.projection.action.SpecialtyAction;
import com.savor.ads.projection.action.StopAction;
import com.savor.ads.projection.action.VideoAction;
import com.savor.ads.projection.action.VideoPptAction;
import com.savor.ads.projection.action.VodAction;
import com.savor.ads.projection.action.VolumeAction;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 点播、投屏类操作的接收回调
 * Created by zhanghq on 2016/12/14.
 */

public class ProjectOperationListener implements OnRemoteOperationListener {
    private final Context mContext;

    private static ProjectOperationListener instance;
    public static ProjectOperationListener getInstance(Context context) {
        if (instance == null) {
            instance = new ProjectOperationListener(context.getApplicationContext());
        }
        return instance;
    }

    private ProjectOperationListener(Context context) {
        mContext = context;
    }

    @Override
    public PrepareResponseVoNew showVod(String mediaName, String vodType, int position, boolean isFromWeb, boolean isNewDevice) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        String vid = "";
        String url = "";
        boolean vodCheckPass = true;
        DBHelper dbHelper = DBHelper.get(mContext);

        if ("2".equals(vodType)) {
            // 酒楼宣传片点播
            List<MediaLibBean> list = dbHelper.findPlayListByWhere(DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?", new String[]{mediaName});

            if (list != null && !list.isEmpty()) {
                MediaLibBean bean = list.get(0);
                String filePath = AppUtils.getFilePath(mContext, AppUtils.StorageFile.media) + bean.getName();
                String md5 = bean.getMd5();
                File file = new File(filePath);
                if (file.exists()) {
                    String vodMd5 = AppUtils.getEasyMd5(file);
                    if (!vodMd5.equals(md5)) {
//                                    file.delete();
//                                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.PLAYLIST,
//                                            DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?", new String[]{url});

                        localResult.setInfo("该点播视频无法播放，请稍后再试");
                        vodCheckPass = false;
                    }
                } else {
                    localResult.setInfo("没有找到点播视频！");
                    vodCheckPass = false;
                }

                url = filePath;
                vid = bean.getVid();
            } else {
                localResult.setInfo("没有找到点播视频！");
                vodCheckPass = false;
            }
        } else {
            // 普通点播视频点播
            List<MediaLibBean> list = dbHelper.findMutlicastMediaLibByWhere(DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?", new String[]{mediaName});

            if (list != null && !list.isEmpty()) {
                MediaLibBean bean = list.get(0);
                String filePath = AppUtils.getFilePath(mContext, AppUtils.StorageFile.multicast) + bean.getName();
                String filePath2 = AppUtils.getFilePath(mContext,AppUtils.StorageFile.media)+bean.getName();
                String md5 = bean.getMd5();
                File file = new File(filePath);
                File file2 = new File(filePath2);
                if (file.exists()||file2.exists()) {
                    String vodMd5 = null;
                    if (file.exists()){
                        vodMd5 = AppUtils.getEasyMd5(file);
                        url = filePath;
                    }else{
                        vodMd5 = AppUtils.getEasyMd5(file2);
                        url = filePath2;
                    }

                    if (TextUtils.isEmpty(vodMd5)||!vodMd5.equals(md5)) {
                        file.delete();
                        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB,
                                DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?", new String[]{mediaName});

                        localResult.setInfo("该点播视频无法播放，请稍后再试");
                        vodCheckPass = false;
                    }
                } else {
                    localResult.setInfo("没有找到点播视频！");
                    vodCheckPass = false;
                }


                vid = bean.getVid();
            } else {
                localResult.setInfo("没有找到点播视频！");
                vodCheckPass = false;
            }
        }

//        dbHelper.close();
        if (vodCheckPass) {
            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_ID)) {
                GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
            }

            localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
            localResult.setInfo("加载成功！");

            VodAction vodAction = new VodAction(mContext, vid, url, position, isFromWeb, isNewDevice);
            ProjectionManager.getInstance().enqueueAction(vodAction);
        } else {
            localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
        }

        return localResult;
    }

    @Override
    public PrepareResponseVoNew showImage(int imageType, int rotation, boolean isThumbnail, String seriesId, boolean isNewDevice) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        if (isThumbnail) {
            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_ID)) {
                GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
            }

            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
        } else {
            // 大图的时候不生成新的ProjectId
            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID);
        }
        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

//        // 跳转或将参数设置到ScreenProjectionActivity
//        Bundle data = new Bundle();
//        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_PICTURE);
//        data.putInt(ScreenProjectionActivity.EXTRA_IMAGE_ROTATION, rotation);
//        data.putBoolean(ScreenProjectionActivity.EXTRA_IS_THUMBNAIL, isThumbnail);
//        data.putInt(ScreenProjectionActivity.EXTRA_IMAGE_TYPE, imageType);
//        data.putString(ScreenProjectionActivity.EXTRA_MEDIA_ID, seriesId);
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity && !((ScreenProjectionActivity) activity).isBeenStopped()) {
//            LogUtils.d("Listener will setNewProjection");
//            ((ScreenProjectionActivity) activity).setNewProjection(data);
//        } else {
//            if (ActivitiesManager.getInstance().getCurrentActivity() == null) {
//                LogUtils.d("Listener will startActivity in new task");
//                Intent intent = new Intent(mContext, ScreenProjectionActivity.class);
//                intent.putExtras(data);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                mContext.startActivity(intent);
//            } else {
//                LogUtils.d("Listener will startActivity in " + activity);
//                Intent intent = new Intent(activity, ScreenProjectionActivity.class);
//                intent.putExtras(data);
//                activity.startActivity(intent);
//            }
//        }

        ImageAction imageAction = new ImageAction(mContext, imageType, rotation, isThumbnail,seriesId, isNewDevice);
        ProjectionManager.getInstance().enqueueAction(imageAction);

        return localResult;
    }

    @Override
    public PrepareResponseVoNew showImage(int imageType, String imagePath,boolean isThumbnail,String avatarUrl,String nickname) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        if (isThumbnail) {
            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_ID)) {
                GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
            }

            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
        } else {
            // 大图的时候不生成新的ProjectId
            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID);
        }
        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

        ImageAction imageAction = new ImageAction(mContext, imageType, imagePath,isThumbnail);
        ProjectionManager.getInstance().enqueueAction(imageAction);

        return localResult;
    }

    @Override
    public PrepareResponseVoNew showImage(int imageType, String imagePath,boolean isThumbnail,String words,String avatarUrl,String nickname) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        if (isThumbnail) {
            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_ID)) {
                GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
            }

            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
        } else {
            // 大图的时候不生成新的ProjectId
            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID);
        }
        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

        ImageAction imageAction = new ImageAction(mContext, imageType, imagePath,isThumbnail,words,avatarUrl,nickname);
        ProjectionManager.getInstance().enqueueAction(imageAction);

        return localResult;
    }

    @Override
    public void showPpt(PptRequestVo currentPptRequest, boolean isNewDevice, String deviceId) {
        PptAction imageAction = new PptAction(mContext, currentPptRequest, isNewDevice, deviceId);
        ProjectionManager.getInstance().enqueueAction(imageAction);
    }

    @Override
    public void showVideoPpt(PptVideoRequestVo currentPptRequest, boolean isNewDevice, String deviceId) {
        VideoPptAction imageAction = new VideoPptAction(mContext, currentPptRequest, isNewDevice, deviceId);
        ProjectionManager.getInstance().enqueueAction(imageAction);
    }

    @Override
    public void showSpecialty(ArrayList<String> mediaPath, int interval, boolean isNewDevice) {
        SpecialtyAction specialtyAction = new SpecialtyAction(mContext, mediaPath, interval, isNewDevice);
        ProjectionManager.getInstance().enqueueAction(specialtyAction);
    }

    @Override
    public void showGreeting(String word, int template, int duration, boolean isNewDevice) {
        GreetingAction greetingAction = new GreetingAction(mContext, word, template, duration, isNewDevice);
        ProjectionManager.getInstance().enqueueAction(greetingAction);
    }

    @Override
    public void showAdv(ArrayList<String> mediaPath, boolean isNewDevice) {
        AdvAction advAction = new AdvAction(mContext, mediaPath, isNewDevice);
        ProjectionManager.getInstance().enqueueAction(advAction);
    }

    @Override
    public void showGreetingThenSpecialty(String word, int template, int duration, ArrayList<String> specialtyPaths, int interval, boolean isNewDevice) {
        GreetingThenSpecialtyAction greetingAction = new GreetingThenSpecialtyAction(mContext, word, template, duration, specialtyPaths, interval, isNewDevice);
        ProjectionManager.getInstance().enqueueAction(greetingAction);
    }

    @Override
    public PrepareResponseVoNew showVideo(String videoPath, int position, boolean isNewDevice) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_ID)) {
            GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
        }

        localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

//        // 跳转或将参数设置到ScreenProjectionActivity
//        Bundle data = new Bundle();
//        data.putString(ScreenProjectionActivity.EXTRA_URL, videoPath);
//        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_VIDEO);
//        data.putInt(ScreenProjectionActivity.EXTRA_VIDEO_POSITION, position);
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity && !((ScreenProjectionActivity) activity).isBeenStopped()) {
//            LogUtils.d("Listener will setNewProjection");
//            ((ScreenProjectionActivity) activity).setNewProjection(data);
//        } else {
//            if (ActivitiesManager.getInstance().getCurrentActivity() == null) {
//                LogUtils.d("Listener will startActivity in new task");
//                Intent intent = new Intent(mContext, ScreenProjectionActivity.class);
//                intent.putExtras(data);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                mContext.startActivity(intent);
//            } else {
//                LogUtils.d("Listener will startActivity in " + activity);
//                Intent intent = new Intent(activity, ScreenProjectionActivity.class);
//                intent.putExtras(data);
//                activity.startActivity(intent);
//            }
//        }

        VideoAction videoAction = new VideoAction(mContext, videoPath, position, isNewDevice);
        ProjectionManager.getInstance().enqueueAction(videoAction);

        return localResult;
    }

    @Override
    public PrepareResponseVoNew showVideo(String videoPath, int position, boolean isNewDevice,String avatarUrl,String nickname) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_ID)) {
            GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
        }

        localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

        VideoAction videoAction = new VideoAction(mContext, videoPath, position, isNewDevice,avatarUrl,nickname);
        ProjectionManager.getInstance().enqueueAction(videoAction);

        return localResult;
    }

//    /**
//     * 调整播放进度
//     *
//     * @param position
//     * @return
//     */
//    @Override
//    public SeekResponseVo seek(int position) {
////        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
////        if (activity instanceof ScreenProjectionActivity) {
////            return ((ScreenProjectionActivity) activity).seekTo(position);
////        } else {
////            return null;
////        }
//
//        SeekAction seekAction = new SeekAction(position);
//        ProjectionManager.getInstance().enqueueAction(seekAction);
//
//        SeekResponseVo responseVo = new SeekResponseVo();
//        responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
//        responseVo.setInfo("成功");
//        return responseVo;
//    }

    @Override
    public SeekResponseVo seek(int position, String projectId) {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
//                return ((ScreenProjectionActivity) activity).seekTo(position);
                SeekAction seekAction = new SeekAction(position);
                ProjectionManager.getInstance().enqueueAction(seekAction);

                SeekResponseVo responseVo = new SeekResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                responseVo.setInfo("成功");
                return responseVo;
            } else {
                SeekResponseVo responseVo = new SeekResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
//        } else {
//            SeekResponseVo responseVo = new SeekResponseVo();
//            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//            responseVo.setInfo("操作失败");
//            return responseVo;
//        }
    }

//    /**
//     * 暂停、恢复播放
//     *
//     * @return
//     */
//    @Override
//    public PlayResponseVo play(int action) {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
//            return ((ScreenProjectionActivity) activity).togglePlay(action);
//        } else {
//            return null;
//        }
//    }

    @Override
    public PlayResponseVo play(int action, String projectId) {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
//                return ((ScreenProjectionActivity) activity).togglePlay(action);

                PlayAction playAction = new PlayAction(action, projectId);
                ProjectionManager.getInstance().enqueueAction(playAction);

                PlayResponseVo responseVo = new PlayResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

                return responseVo;
            } else {
                PlayResponseVo responseVo = new PlayResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
//        } else {
//            PlayResponseVo responseVo = new PlayResponseVo();
//            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//            responseVo.setInfo("操作失败");
//            return responseVo;
//        }
    }

//    /**
//     * 停止投屏
//     *
//     * @return
//     */
//    @Override
//    public StopResponseVo stop() {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
//            ((ScreenProjectionActivity) activity).stop(true);
//
//            StopResponseVo stopResponseVo = new StopResponseVo();
//            stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
//
//            return stopResponseVo;
//        } else {
//            return null;
//        }
//    }

    @Override
    public StopResponseVo stop(String projectId) {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (!GlobalValues.IS_LOTTERY) {
            if (TextUtils.isEmpty(projectId)) {
                StopResponseVo responseVo = new StopResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }

//            if (activity instanceof ScreenProjectionActivity) {
                if (projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
//                    ((ScreenProjectionActivity) activity).stop(true);
                    StopAction stopAction = new StopAction(projectId, false, false);
                    ProjectionManager.getInstance().enqueueAction(stopAction);

                    StopResponseVo stopResponseVo = new StopResponseVo();
                    stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

                    return stopResponseVo;
                } else {
                    StopResponseVo responseVo = new StopResponseVo();
                    responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                    responseVo.setInfo("操作失败");
                    return responseVo;
                }
//            } else {
//                if (projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
//                    // 播放正在准备，还没来得及跳到投屏页
//                    StopResponseVo stopResponseVo = new StopResponseVo();
//                    stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//                    stopResponseVo.setInfo("正在准备投屏，请稍候再试");
//                    return stopResponseVo;
//                } else if (projectId.equals(GlobalValues.LAST_PROJECT_ID)) {
//                    // 播放已结束
//                    StopResponseVo stopResponseVo = new StopResponseVo();
//                    stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
//                    return stopResponseVo;
//                } else {
//                    StopResponseVo responseVo = new StopResponseVo();
//                    responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//                    responseVo.setInfo("操作失败");
//                    return responseVo;
//                }
//            }
        } else {

//            if (activity instanceof LotteryActivity) {
//                if (projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
//                    ((LotteryActivity) activity).stop();
            StopAction stopAction = new StopAction(projectId, true, false);
            ProjectionManager.getInstance().enqueueAction(stopAction);

                    StopResponseVo stopResponseVo = new StopResponseVo();
                    stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

                    return stopResponseVo;
//                } else {
//                    StopResponseVo responseVo = new StopResponseVo();
//                    responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
//                    responseVo.setInfo("操作失败");
//                    return responseVo;
//                }

//            } else {
//                StopResponseVo responseVo = new StopResponseVo();
//                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
//                responseVo.setInfo("操作失败");
//                return responseVo;
//            }
        }
    }

    @Override
    public void rstrStop() {
        StopAction stopAction = new StopAction(null, false, true);
        ProjectionManager.getInstance().enqueueAction(stopAction);
    }

    /**
     * 旋转图片
     *
     * @param rotateDegree
     * @return
     */
//    @Override
//    public RotateResponseVo rotate(int rotateDegree) {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
//            return ((ScreenProjectionActivity) activity).rotate(rotateDegree);
//        } else {
//            return null;
//        }
//    }

    @Override
    public RotateResponseVo rotate(int rotateDegree, String projectId) {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
//                return ((ScreenProjectionActivity) activity).rotate(rotateDegree);

                RotateAction rotateAction = new RotateAction(rotateDegree, projectId);
                ProjectionManager.getInstance().enqueueAction(rotateAction);

                RotateResponseVo responseVo = new RotateResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                responseVo.setInfo("成功");
                responseVo.setRotateValue(rotateDegree);
                return responseVo;
            } else {
                RotateResponseVo responseVo = new RotateResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
//        } else {
//            RotateResponseVo responseVo = new RotateResponseVo();
//            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//            responseVo.setInfo("操作失败");
//            return responseVo;
//        }
    }

//    @Override
//    public VolumeResponseVo volume(int action) {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
//            return ((ScreenProjectionActivity) activity).volume(action);
//        } else {
//            // 不在ScreenProjectionActivity页面时认为已经播放完毕结束ScreenProjectionActivity了
//            VolumeResponseVo responseVo = new VolumeResponseVo();
//            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//            return responseVo;
//        }
//    }

    @Override
    public VolumeResponseVo volume(int action, String projectId) {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
//                return ((ScreenProjectionActivity) activity).volume(action);

                VolumeAction volumeAction = new VolumeAction(action, projectId);
                ProjectionManager.getInstance().enqueueAction(volumeAction);

                VolumeResponseVo responseVo = new VolumeResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                return responseVo;
            } else {
                VolumeResponseVo responseVo = new VolumeResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
//        } else {
//            VolumeResponseVo responseVo = new VolumeResponseVo();
//            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//            responseVo.setInfo("操作失败");
//            return responseVo;
//        }
    }

    /**
     * 点播者查询播放进度等信息
     *
     * @return
     */
//    @Override
//    public Object query() {
//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity instanceof ScreenProjectionActivity) {
//            return ((ScreenProjectionActivity) activity).query();
//        } else {
//            // 不在ScreenProjectionActivity页面时认为已经播放完毕结束ScreenProjectionActivity了
//            QueryPosBySessionIdResponseVo queryResponse = new QueryPosBySessionIdResponseVo();
//            queryResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_VIDEO_COMPLETE);
//            return queryResponse;
//        }
//    }

    @Override
    public Object query(String projectId) {
        if (TextUtils.isEmpty(projectId)) {
            QueryPosBySessionIdResponseVo responseVo = new QueryPosBySessionIdResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
            return responseVo;
        }

        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            if (projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                return ((ScreenProjectionActivity) activity).query();
            } else {
                QueryPosBySessionIdResponseVo responseVo = new QueryPosBySessionIdResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                return responseVo;
            }
        } else {
            if (projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                // 播放正在准备，还没来得及跳到投屏页
                QueryPosBySessionIdResponseVo queryResponse = new QueryPosBySessionIdResponseVo();
                queryResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                queryResponse.setPos(0);
                return queryResponse;
            } else if (projectId.equals(GlobalValues.LAST_PROJECT_ID)) {
                // 播放已结束
                QueryPosBySessionIdResponseVo queryResponse = new QueryPosBySessionIdResponseVo();
                queryResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_VIDEO_COMPLETE);
                return queryResponse;
            }
        }

        QueryPosBySessionIdResponseVo queryResponse = new QueryPosBySessionIdResponseVo();
        queryResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
        return queryResponse;
    }

    @Override
    public void showCode() {
        if (mContext instanceof SavorApplication) {
            ((SavorApplication) mContext).showQrCodeWindow(null);
        }
    }

    @Override
    public ResponseT<CodeVerifyBean> verify(String code) {
        ResponseT responseT = new ResponseT();
        Session session = Session.get(mContext);
        if (!TextUtils.isEmpty(code) && code.equals(session.getAuthCode())) {
            responseT.setCode(10000);
            CodeVerifyBean bean = new CodeVerifyBean();
            bean.setBox_id(session.getBoxId());
            bean.setBox_ip(AppUtils.getLocalIPAddress());
            bean.setBox_mac(session.getEthernetMac());
            bean.setHotel_id(session.getBoiteId());
            bean.setRoom_id(session.getRoomId());
            bean.setSsid(AppUtils.getShowingSSID(mContext));
            responseT.setResult(bean);
        } else {
            responseT.setCode(10001);
            responseT.setMsg("输入有误，请重新输入");
        }
        return responseT;
    }

    @Override
    public PrepareResponseVoNew showEgg(String date, int hunger) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();

        // 检测抽奖时间
        if (Session.get(mContext).getPrizeInfo() != null) {
            String nowStr = AppUtils.getCurTime("yyyy-MM-dd");
            if (nowStr.equals(Session.get(mContext).getPrizeInfo().getDate_time())) {
                String tips = "";
                boolean checkPass = false;
                for (AwardTime awardTime : Session.get(mContext).getPrizeInfo().getAward_time()) {
                    if (awardTime != null) {
                        tips += awardTime.getStart_time() + "-" + awardTime.getEnd_time() + "；";
                        Date now = new Date();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        try {
                            Date startDate = simpleDateFormat.parse(Session.get(mContext).getPrizeInfo().getDate_time() + " " + awardTime.getStart_time());
                            Date endDate = simpleDateFormat.parse(Session.get(mContext).getPrizeInfo().getDate_time() + " " + awardTime.getEnd_time());
                            if (now.compareTo(startDate) >= 0 && now.compareTo(endDate) < 0) {
                                checkPass = true;
                                break;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (!checkPass) {
                    localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    localResult.setInfo("游戏时间为" + tips + "准备好姿势来砸吧！");
                    return localResult;
                }
            } else {
                Session.get(mContext).setPrizeInfo(null);
                localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                localResult.setInfo("当前包间没有参与活动");
                return localResult;
            }
        } else {
            localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            localResult.setInfo("当前包间没有参与活动");
            return localResult;
        }

        if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_ID)) {
            GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
        }

        localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

        // 校验日期，不一致不让中
        String boxDate = AppUtils.getCurTime("yyyyMMdd");
        if (!boxDate.equals(date)) {
            LogUtils.e("手机日期与机顶盒日期不一致！机顶盒日期为" + boxDate + " 手机日期为" + date);
            hunger = 0;
        }

//        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
//        if (activity == null) {
//            LogUtils.d("Listener will startActivity in new task");
//            Intent intent = new Intent(mContext, LotteryActivity.class);
//            intent.putExtra(LotteryActivity.EXTRA_HUNGER, hunger);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            mContext.startActivity(intent);
//        } else {
//            if (activity instanceof ScreenProjectionActivity) {
//                ((ScreenProjectionActivity) activity).stop(false);
//            }
//
//            LogUtils.d("Listener will startActivity in " + activity);
//            Intent intent = new Intent(activity, LotteryActivity.class);
//            intent.putExtra(LotteryActivity.EXTRA_HUNGER, hunger);
//            activity.startActivity(intent);
//        }

        ShowEggAction showEggAction = new ShowEggAction(mContext, hunger);
        ProjectionManager.getInstance().enqueueAction(showEggAction);

        return localResult;
    }

    @Override
    public HitEggResponseVo hitEgg(String projectId) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof LotteryActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                return ((LotteryActivity) activity).hitEgg();
            } else {
                HitEggResponseVo responseVo = new HitEggResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("砸蛋操作失败");
                return responseVo;
            }
        } else {
            HitEggResponseVo responseVo = new HitEggResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("游戏超时啦，请重新启动");
            return responseVo;
        }
    }
}
