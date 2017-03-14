package com.savor.ads.callback;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.jar.savor.box.interfaces.OnRemoteOperationListener;
import com.jar.savor.box.vo.BaseResponse;
import com.jar.savor.box.vo.PlayRequestVo;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.PrepareRequestVo;
import com.jar.savor.box.vo.PrepareResponseVo;
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
import com.savor.ads.activity.ScreenProjectionActivity;
import com.savor.ads.bean.OnDemandBean;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.database.DBHelper;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.util.List;

/**
 * 点播、投屏类操作的接收回调
 * Created by zhanghq on 2016/12/14.
 */

public class ProjectOperationListener implements OnRemoteOperationListener {
    private final Context mContext;

    public ProjectOperationListener(Context context) {
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


                    dbHelper.close();
                } else {
                    type = ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN;
                }
            }

            if (vodCheckPass) {
                localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                localResult.setInfo("加载成功！");

                // 跳转或将参数设置到ScreenProjectionActivity
                Bundle data = new Bundle();
                data.putString(ScreenProjectionActivity.EXTRA_URL, url);
                data.putString(ScreenProjectionActivity.EXTRA_TYPE, type);
                data.putString(ScreenProjectionActivity.EXTRA_VID, vid);
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
    public BaseResponse showVod(String mediaName, String vodType) {
        BaseResponse localResult = new BaseResponse();
        String type = ConstantValues.PROJECT_TYPE_VIDEO_VOD;
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

        dbHelper.close();
        if (vodCheckPass) {
            localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
            localResult.setInfo("加载成功！");

            // 跳转或将参数设置到ScreenProjectionActivity
            Bundle data = new Bundle();
            data.putString(ScreenProjectionActivity.EXTRA_URL, url);
            data.putString(ScreenProjectionActivity.EXTRA_TYPE, type);
            data.putString(ScreenProjectionActivity.EXTRA_VID, vid);

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

        return localResult;
    }

    @Override
    public BaseResponse showImage() {
        BaseResponse localResult = new BaseResponse();

        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

        // 跳转或将参数设置到ScreenProjectionActivity
        Bundle data = new Bundle();
        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_PICTURE);
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
    public BaseResponse showVideo(String videoPath) {
        BaseResponse localResult = new BaseResponse();

        localResult.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        localResult.setInfo("加载成功！");

        // 跳转或将参数设置到ScreenProjectionActivity
        Bundle data = new Bundle();
        data.putString(ScreenProjectionActivity.EXTRA_URL, videoPath);
        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_VIDEO_2SCREEN);
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

    /**
     * 停止投屏
     *
     * @return
     */
    @Override
    public StopResponseVo stop() {
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            return ((ScreenProjectionActivity) activity).stop();
        } else {
            return null;
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
}
