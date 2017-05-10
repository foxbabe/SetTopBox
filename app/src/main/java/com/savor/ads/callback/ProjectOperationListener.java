package com.savor.ads.callback;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.jar.savor.box.interfaces.OnRemoteOperationListener;
import com.jar.savor.box.vo.CodeVerifyBean;
import com.jar.savor.box.vo.HitEggResponseVo;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.PrepareRequestVo;
import com.jar.savor.box.vo.PrepareResponseVo;
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
import com.savor.ads.bean.OnDemandBean;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 点播、投屏类操作的接收回调
 * Created by zhanghq on 2016/12/14.
 */

public class ProjectOperationListener implements OnRemoteOperationListener {
    private final Application mContext;

    public ProjectOperationListener(Application context) {
        mContext = context;
    }

    @Override
    public PrepareResponseVo prepare(PrepareRequestVo prepareRequestVo) {
        PrepareResponseVo localResult = new PrepareResponseVo();

        if (prepareRequestVo == null) {
            localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            localResult.setInfo("参数为空！");
        } else {

            String url = prepareRequestVo.getAsseturl();
            String assetType = prepareRequestVo.getAssettype();
            String type = assetType;
            String vid = "";

            boolean vodCheckPass = true;

            if (assetType.equals(ConstantValues.PROJECT_TYPE_VIDEO)) {
                if (prepareRequestVo.getAction().equals(ConstantValues.PROJECT_TYPE_VIDEO_VOD)) {
                    type = ConstantValues.PROJECT_TYPE_VIDEO_VOD;
                    // 视频标题
                    url = prepareRequestVo.getAssetname();
                    DBHelper dbHelper = DBHelper.get(mContext);
                    if (prepareRequestVo.getVodType() == 2) {
                        // 酒楼宣传片点播
                        List<PlayListBean> list = dbHelper.findPlayListByWhere(DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?", new String[]{url});

                        if (list != null && !list.isEmpty()) {
                            PlayListBean bean = list.get(0);
                            String filePath = AppUtils.getFilePath(mContext, AppUtils.StorageFile.media) + bean.getMedia_name();
                            String md5 = bean.getMd5();
                            File file = new File(filePath);
                            if (file.exists()) {
                                String vodMd5 = AppUtils.getMD5Method(file);
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
                        List<OnDemandBean> list = dbHelper.findMutlicastMediaLibByWhere(DBHelper.MediaDBInfo.FieldName.TITLE + "=?", new String[]{url});

                        if (list != null && !list.isEmpty()) {
                            OnDemandBean bean = list.get(0);
                            String filePath = AppUtils.getFilePath(mContext, AppUtils.StorageFile.multicast) + bean.getTitle();
                            String md5 = bean.getMd5();
                            File file = new File(filePath);
                            if (file.exists()) {
                                String vodMd5 = AppUtils.getMD5Method(file);
                                if (!vodMd5.equals(md5)) {
                                    file.delete();
                                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB,
                                            DBHelper.MediaDBInfo.FieldName.TITLE + "=?", new String[]{url});

                                    localResult.setInfo("该点播视频无法播放，请稍后再试");
                                    vodCheckPass = false;
                                }
                            } else {
                                localResult.setInfo("没有找到点播视频！");
                                vodCheckPass = false;
                            }

                            url = filePath;
                            vid = bean.getVodId();
                        } else {
                            localResult.setInfo("没有找到点播视频！");
                            vodCheckPass = false;
                        }
                    }


//                    dbHelper.close();
                } else {
                    type = ConstantValues.PROJECT_TYPE_VIDEO;
                }
            }

            if (vodCheckPass) {
                localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                localResult.setInfo("加载成功！");

                // 跳转或将参数设置到ScreenProjectionActivity
                Bundle data = new Bundle();
                data.putString(ScreenProjectionActivity.EXTRA_URL, url);
                data.putString(ScreenProjectionActivity.EXTRA_TYPE, type);
                data.putString(ScreenProjectionActivity.EXTRA_MEDIA_ID, vid);
                Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
                if (activity instanceof ScreenProjectionActivity && !((ScreenProjectionActivity) activity).isBeenStopped()) {
                    LogUtils.d("Listener will setNewProjection");
                    ((ScreenProjectionActivity) activity).setNewProjection(data);
                } else {
                    if (ActivitiesManager.getInstance().getCurrentActivity() == null) {
                        LogUtils.d("Listener will startActivity in new task");
                        Intent intent = new Intent(mContext, ScreenProjectionActivity.class);
                        intent.putExtras(data);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    } else {
                        LogUtils.d("Listener will startActivity in " + activity);
                        Intent intent = new Intent(activity, ScreenProjectionActivity.class);
                        intent.putExtras(data);
                        activity.startActivity(intent);
                    }
                }
            } else {
                localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            }
        }
        return localResult;
    }

    @Override
    public PrepareResponseVoNew showVod(String mediaName, String vodType, int position, boolean isFromWeb) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        String vid = "";
        String url = "";
        boolean vodCheckPass = true;
        DBHelper dbHelper = DBHelper.get(mContext);

        if ("2".equals(vodType)) {
            // 酒楼宣传片点播
            List<PlayListBean> list = dbHelper.findPlayListByWhere(DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?", new String[]{mediaName});

            if (list != null && !list.isEmpty()) {
                PlayListBean bean = list.get(0);
                String filePath = AppUtils.getFilePath(mContext, AppUtils.StorageFile.media) + bean.getMedia_name();
                String md5 = bean.getMd5();
                File file = new File(filePath);
                if (file.exists()) {
                    String vodMd5 = AppUtils.getMD5Method(file);
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
            List<OnDemandBean> list = dbHelper.findMutlicastMediaLibByWhere(DBHelper.MediaDBInfo.FieldName.TITLE + "=?", new String[]{mediaName});

            if (list != null && !list.isEmpty()) {
                OnDemandBean bean = list.get(0);
                String filePath = AppUtils.getFilePath(mContext, AppUtils.StorageFile.multicast) + bean.getTitle();
                String md5 = bean.getMd5();
                File file = new File(filePath);
                if (file.exists()) {
                    String vodMd5 = AppUtils.getMD5Method(file);
                    if (!vodMd5.equals(md5)) {
                        file.delete();
                        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.MULTICASTMEDIALIB,
                                DBHelper.MediaDBInfo.FieldName.TITLE + "=?", new String[]{url});

                        localResult.setInfo("该点播视频无法播放，请稍后再试");
                        vodCheckPass = false;
                    }
                } else {
                    localResult.setInfo("没有找到点播视频！");
                    vodCheckPass = false;
                }

                url = filePath;
                vid = bean.getVodId();
            } else {
                localResult.setInfo("没有找到点播视频！");
                vodCheckPass = false;
            }
        }

//        dbHelper.close();
        if (vodCheckPass) {
            localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
            localResult.setInfo("加载成功！");

            // 跳转或将参数设置到ScreenProjectionActivity
            Bundle data = new Bundle();
            data.putString(ScreenProjectionActivity.EXTRA_URL, url);
            data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_VIDEO_VOD);
            data.putString(ScreenProjectionActivity.EXTRA_MEDIA_ID, vid);
            data.putInt(ScreenProjectionActivity.EXTRA_VIDEO_POSITION, position);
            data.putBoolean(ScreenProjectionActivity.EXTRA_IS_FROM_WEB, isFromWeb);

            Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
            if (activity instanceof ScreenProjectionActivity && !((ScreenProjectionActivity) activity).isBeenStopped()) {
                LogUtils.e("Listener will setNewProjection");
                ((ScreenProjectionActivity) activity).setNewProjection(data);
            } else {
                if (ActivitiesManager.getInstance().getCurrentActivity() == null) {
                    LogUtils.e("Listener will startActivity in new task");
                    Intent intent = new Intent(mContext, ScreenProjectionActivity.class);
                    intent.putExtras(data);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                } else {
                    LogUtils.e("Listener will startActivity in " + activity.toString());
                    Intent intent = new Intent(activity, ScreenProjectionActivity.class);
                    intent.putExtras(data);
                    activity.startActivity(intent);
                }
            }
        } else {
            localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
        }

        return localResult;
    }

    @Override
    public PrepareResponseVoNew showImage(int imageType, int rotation, boolean isThumbnail, String seriesId) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        if (isThumbnail) {
            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
        } else {
            // 大图的时候不生成新的ProjectId
            localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID);
        }
        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

        // 跳转或将参数设置到ScreenProjectionActivity
        Bundle data = new Bundle();
        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_PICTURE);
        data.putInt(ScreenProjectionActivity.EXTRA_IMAGE_ROTATION, rotation);
        data.putBoolean(ScreenProjectionActivity.EXTRA_IS_THUMBNAIL, isThumbnail);
        data.putInt(ScreenProjectionActivity.EXTRA_IMAGE_TYPE, imageType);
        data.putString(ScreenProjectionActivity.EXTRA_MEDIA_ID, seriesId);
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity && !((ScreenProjectionActivity) activity).isBeenStopped()) {
            LogUtils.d("Listener will setNewProjection");
            ((ScreenProjectionActivity) activity).setNewProjection(data);
        } else {
            if (ActivitiesManager.getInstance().getCurrentActivity() == null) {
                LogUtils.d("Listener will startActivity in new task");
                Intent intent = new Intent(mContext, ScreenProjectionActivity.class);
                intent.putExtras(data);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                LogUtils.d("Listener will startActivity in " + activity);
                Intent intent = new Intent(activity, ScreenProjectionActivity.class);
                intent.putExtras(data);
                activity.startActivity(intent);
            }
        }
        return localResult;
    }

    @Override
    public PrepareResponseVoNew showVideo(String videoPath, int position) {
        PrepareResponseVoNew localResult = new PrepareResponseVoNew();
        localResult.setProjectId(GlobalValues.CURRENT_PROJECT_ID = UUID.randomUUID().toString());
        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

        // 跳转或将参数设置到ScreenProjectionActivity
        Bundle data = new Bundle();
        data.putString(ScreenProjectionActivity.EXTRA_URL, videoPath);
        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_VIDEO);
        data.putInt(ScreenProjectionActivity.EXTRA_VIDEO_POSITION, position);
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity && !((ScreenProjectionActivity) activity).isBeenStopped()) {
            LogUtils.d("Listener will setNewProjection");
            ((ScreenProjectionActivity) activity).setNewProjection(data);
        } else {
            if (ActivitiesManager.getInstance().getCurrentActivity() == null) {
                LogUtils.d("Listener will startActivity in new task");
                Intent intent = new Intent(mContext, ScreenProjectionActivity.class);
                intent.putExtras(data);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                LogUtils.d("Listener will startActivity in " + activity);
                Intent intent = new Intent(activity, ScreenProjectionActivity.class);
                intent.putExtras(data);
                activity.startActivity(intent);
            }
        }
        return localResult;
    }

    /**
     * 调整播放进度
     *
     * @param position
     * @return
     */
    @Override
    public SeekResponseVo seek(int position) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            return ((ScreenProjectionActivity) activity).seekTo(position);
        } else {
            return null;
        }
    }

    @Override
    public SeekResponseVo seek(int position, String projectId) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                return ((ScreenProjectionActivity) activity).seekTo(position);
            } else {
                SeekResponseVo responseVo = new SeekResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
        } else {
            SeekResponseVo responseVo = new SeekResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("操作失败");
            return responseVo;
        }
    }

    /**
     * 暂停、恢复播放
     *
     * @return
     */
    @Override
    public PlayResponseVo play(int action) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            return ((ScreenProjectionActivity) activity).togglePlay(action);
        } else {
            return null;
        }
    }

    @Override
    public PlayResponseVo play(int action, String projectId) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                return ((ScreenProjectionActivity) activity).togglePlay(action);
            } else {
                PlayResponseVo responseVo = new PlayResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
        } else {
            PlayResponseVo responseVo = new PlayResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("操作失败");
            return responseVo;
        }
    }

    /**
     * 停止投屏
     *
     * @return
     */
    @Override
    public StopResponseVo stop() {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            ((ScreenProjectionActivity) activity).stop();

            StopResponseVo stopResponseVo = new StopResponseVo();
            stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

            return stopResponseVo;
        } else {
            return null;
        }
    }

    @Override
    public StopResponseVo stop(String projectId) {
        if (TextUtils.isEmpty(projectId)) {
            StopResponseVo responseVo = new StopResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
            responseVo.setInfo("操作失败");
            return responseVo;
        }

        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            if (projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                ((ScreenProjectionActivity) activity).stop();
                StopResponseVo stopResponseVo = new StopResponseVo();
                stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

                return stopResponseVo;
            } else {
                StopResponseVo responseVo = new StopResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
        } else if (activity instanceof LotteryActivity) {
            ((LotteryActivity) activity).stop();
            StopResponseVo stopResponseVo = new StopResponseVo();
            stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

            return stopResponseVo;
        } else {
            if (projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                // 播放正在准备，还没来得及跳到投屏页
                StopResponseVo stopResponseVo = new StopResponseVo();
                stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                stopResponseVo.setInfo("正在准备投屏，请稍候再试");
                return stopResponseVo;
            } else if (projectId.equals(GlobalValues.LAST_PROJECT_ID)) {
                // 播放已结束
                StopResponseVo stopResponseVo = new StopResponseVo();
                stopResponseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                return stopResponseVo;
            } else {
                StopResponseVo responseVo = new StopResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
        }
    }

    /**
     * 旋转图片
     *
     * @param rotateDegree
     * @return
     */
    @Override
    public RotateResponseVo rotate(int rotateDegree) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            return ((ScreenProjectionActivity) activity).rotate(rotateDegree);
        } else {
            return null;
        }
    }

    @Override
    public RotateResponseVo rotate(int rotateDegree, String projectId) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                return ((ScreenProjectionActivity) activity).rotate(rotateDegree);
            } else {
                RotateResponseVo responseVo = new RotateResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
        } else {
            RotateResponseVo responseVo = new RotateResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("操作失败");
            return responseVo;
        }
    }

    @Override
    public VolumeResponseVo volume(int action) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            return ((ScreenProjectionActivity) activity).volume(action);
        } else {
            // 不在ScreenProjectionActivity页面时认为已经播放完毕结束ScreenProjectionActivity了
            VolumeResponseVo responseVo = new VolumeResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            return responseVo;
        }
    }

    @Override
    public VolumeResponseVo volume(int action, String projectId) {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                return ((ScreenProjectionActivity) activity).volume(action);
            } else {
                VolumeResponseVo responseVo = new VolumeResponseVo();
                responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED);
                responseVo.setInfo("操作失败");
                return responseVo;
            }
        } else {
            VolumeResponseVo responseVo = new VolumeResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("操作失败");
            return responseVo;
        }
    }

    /**
     * 点播者查询播放进度等信息
     *
     * @return
     */
    @Override
    public Object query() {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            return ((ScreenProjectionActivity) activity).query();
        } else {
            // 不在ScreenProjectionActivity页面时认为已经播放完毕结束ScreenProjectionActivity了
            QueryPosBySessionIdResponseVo queryResponse = new QueryPosBySessionIdResponseVo();
            queryResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_VIDEO_COMPLETE);
            return queryResponse;
        }
    }

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
            boolean checkPass = false;
            for (AwardTime awardTime : Session.get(mContext).getPrizeInfo().getAward_time()) {
                if (awardTime != null) {
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
                localResult.setInfo("当前时间不可抽奖");
                return localResult;
            }
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

        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity == null) {
            LogUtils.d("Listener will startActivity in new task");
            Intent intent = new Intent(mContext, LotteryActivity.class);
            intent.putExtra(LotteryActivity.EXTRA_HUNGER, hunger);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } else {
            LogUtils.d("Listener will startActivity in " + activity);
            Intent intent = new Intent(activity, LotteryActivity.class);
            intent.putExtra(LotteryActivity.EXTRA_HUNGER, hunger);
            activity.startActivity(intent);

            if (activity instanceof LotteryActivity) {
                ((LotteryActivity) activity).exitImmediately();
            }
        }
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
                responseVo.setInfo("操作失败");
                return responseVo;
            }
        } else {
            HitEggResponseVo responseVo = new HitEggResponseVo();
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("操作失败");
            return responseVo;
        }
    }
}
