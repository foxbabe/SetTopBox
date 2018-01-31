package com.savor.ads.projection.action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.savor.ads.activity.LotteryActivity;
import com.savor.ads.activity.ScreenProjectionActivity;
import com.savor.ads.bean.RstrSpecialty;
import com.savor.ads.database.DBHelper;
import com.savor.ads.projection.ProjectPriority;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.LogUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhang.haiqiang on 2017/12/6.
 */

public class GreetingThenSpecialtyAction extends ProjectionActionBase implements Serializable {
    private transient Context mContext;
    private String mWords;
    private int mTemplate;
    private int mDuration;
    private ArrayList<String> mMediaPaths;
    private int mInterval;
    private boolean mIsNewDevice;

    public GreetingThenSpecialtyAction(Context context, String words, int template, int duration, boolean isNewDevice) {
        super();

        mPriority = ProjectPriority.HIGH;
        this.mContext = context;
        this.mWords = words;
        this.mTemplate = template;
        this.mDuration = duration;
        this.mIsNewDevice = isNewDevice;

        mMediaPaths = new ArrayList<>();
        List<RstrSpecialty> specialties = DBHelper.get(mContext).findSpecialtyByWhere(null, null);

        if (specialties != null && specialties.size() > 0) {
            for (RstrSpecialty specialty : specialties) {
                mMediaPaths.add(specialty.getMedia_path());
            }
        }
        if (mMediaPaths.size() > 1) {
            mInterval = 10;
        } else {
            mInterval = 20;
        }
    }

    @Override
    public void execute() {
        onActionBegin();

        // 跳转或将参数设置到ScreenProjectionActivity
        Bundle data = new Bundle();
        data.putString(ScreenProjectionActivity.EXTRA_TYPE, ConstantValues.PROJECT_TYPE_RSTR_GREETING_THEN_SPECIALTY);
        data.putSerializable(ScreenProjectionActivity.EXTRA_GREETING_WORD, mWords);
        data.putInt(ScreenProjectionActivity.EXTRA_GREETING_TEMPLATE, mTemplate);
        data.putInt(ScreenProjectionActivity.EXTRA_GREETING_DURATION, mDuration);
        data.putSerializable(ScreenProjectionActivity.EXTRA_SPECIALTY_LIST, mMediaPaths);
        data.putInt(ScreenProjectionActivity.EXTRA_SPECIALTY_INTERVAL, mInterval);
        data.putBoolean(ScreenProjectionActivity.EXTRA_IS_NEW_DEVICE, mIsNewDevice);
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
}
