package com.savor.ads.projection.action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.jar.savor.box.vo.PptVideoRequestVo;
import com.savor.ads.activity.LotteryActivity;
import com.savor.ads.activity.ScreenProjectionActivity;
import com.savor.ads.bean.PptVideo;
import com.savor.ads.projection.ProjectPriority;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.io.Serializable;

/**
 * Created by zhang.haiqiang on 2017/6/12.
 */

public class VideoPptAction extends ProjectionActionBase implements Serializable {

    private transient Context mContext;
    private PptVideoRequestVo pptRequestVo;
    private boolean isNewDevice;

    public VideoPptAction(Context mContext, PptVideoRequestVo pptRequestVo, boolean isNewDevice, String deviceId) {
        super();

        mPriority = ProjectPriority.HIGH;
        this.mContext = mContext;
        this.pptRequestVo = pptRequestVo;
        this.isNewDevice = isNewDevice;

        if (pptRequestVo != null && pptRequestVo.getVideos() != null) {
            for (PptVideo pptVideo: pptRequestVo.getVideos()) {
                pptVideo.setName(AppUtils.getFilePath(mContext, AppUtils.StorageFile.ppt) + deviceId +
                        File.separator + pptVideo.getName());
            }
        }
    }

    @Override
    public void execute() {
        onActionBegin();

        // 跳转或将参数设置到ScreenProjectionActivity
        Bundle data = new Bundle();
        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_RSTR_VIDEO_PPT);
        data.putSerializable(ScreenProjectionActivity.EXTRA_VIDEO_PPT_CONFIG, pptRequestVo);
        data.putBoolean(ScreenProjectionActivity.EXTRA_IS_NEW_DEVICE, isNewDevice);
        data.putSerializable(ScreenProjectionActivity.EXTRA_PROJECT_ACTION, this);
        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity && !((ScreenProjectionActivity) activity).isBeenStopped()) {
            LogUtils.d("Listener will setNewProjection");
            ((ScreenProjectionActivity) activity).setNewProjection(data);
        } else {
            if (activity == null) {
                LogUtils.d("Listener will startActivity in new task");
                Intent intent = new Intent(mContext, ScreenProjectionActivity.class);
                intent.putExtras(data);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
            } else {
                if (activity instanceof LotteryActivity) {
                    ((LotteryActivity) activity).stop(false, null);
                }

                LogUtils.d("Listener will startActivity in " + activity);
                Intent intent = new Intent(activity, ScreenProjectionActivity.class);
                intent.putExtras(data);
                activity.startActivity(intent);
            }
        }
    }

    @Override
    public String toString() {
        return "VideoPptAction{" +
                "mContext=" + mContext +
                ", videoPptRequestVo=" + pptRequestVo +
                '}';
    }
}
