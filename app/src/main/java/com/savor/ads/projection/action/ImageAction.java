package com.savor.ads.projection.action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.savor.ads.activity.LotteryActivity;
import com.savor.ads.activity.ScreenProjectionActivity;
import com.savor.ads.projection.ProjectPriority;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.LogUtils;

import java.io.Serializable;

/**
 * Created by zhang.haiqiang on 2017/5/22.
 */

public class ImageAction extends ProjectionActionBase implements Serializable {
    private transient Context mContext;
    private int imageType;
    private String imagePath;
    private int rotation;
    private boolean isThumbnail;
    private String seriesId;
    private boolean isNewDevice;

    public ImageAction(Context context, int imageType, int rotation, boolean isThumbnail, String seriesId, boolean isNewDevice) {
        super();

        mPriority = ProjectPriority.HIGH;
        mContext = context;
        this.imageType = imageType;
        this.rotation = rotation;
        this.isThumbnail = isThumbnail;
        this.seriesId = seriesId;
        this.isNewDevice = isNewDevice;
    }
    public ImageAction(Context context, int imageType, String imagePath) {
        super();

        mPriority = ProjectPriority.HIGH;
        mContext = context;
        this.imageType = imageType;
        this.imagePath = imagePath;
    }

    @Override
    public void execute() {
        onActionBegin();

        // 跳转或将参数设置到ScreenProjectionActivity
        Bundle data = new Bundle();
        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_PICTURE);
        data.putString(ScreenProjectionActivity.EXTRA_IMAGE_PATH,imagePath);
        data.putInt(ScreenProjectionActivity.EXTRA_IMAGE_ROTATION, rotation);
        data.putBoolean(ScreenProjectionActivity.EXTRA_IS_THUMBNAIL, isThumbnail);
        data.putInt(ScreenProjectionActivity.EXTRA_IMAGE_TYPE, imageType);
        data.putString(ScreenProjectionActivity.EXTRA_MEDIA_ID, seriesId);
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
        return "ImageAction{" +
                "imageType=" + imageType +
                ", rotation=" + rotation +
                ", isThumbnail=" + isThumbnail +
                ", seriesId='" + seriesId + '\'' +
                '}';
    }
}
