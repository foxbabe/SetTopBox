package com.savor.ads.projection.action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.savor.ads.activity.LotteryActivity;
import com.savor.ads.activity.ScreenProjectionActivity;
import com.savor.ads.projection.ProjectPriority;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.LogUtils;

import java.io.Serializable;

/**
 * Created by zhang.haiqiang on 2017/5/22.
 */

public class ShowEggAction extends ProjectionActionBase implements Serializable {

    private transient Context mContext;
    private int hunger;

    public ShowEggAction(Context context, int hunger) {
        super();

        mPriority = ProjectPriority.HIGH;
        mContext = context;
        this.hunger = hunger;
    }

    @Override
    public void execute() {
        onActionBegin();

        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity == null) {
            LogUtils.d("Listener will startActivity in new task");
            Intent intent = new Intent(mContext, LotteryActivity.class);
            intent.putExtra(LotteryActivity.EXTRA_HUNGER, hunger);
            intent.putExtra(LotteryActivity.EXTRA_ACTION, this);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        } else {
            if (activity instanceof ScreenProjectionActivity) {
                ((ScreenProjectionActivity) activity).stop(false, null);
            }

            LogUtils.d("Listener will startActivity in " + activity);
            Intent intent = new Intent(activity, LotteryActivity.class);
            intent.putExtra(LotteryActivity.EXTRA_HUNGER, hunger);
            intent.putExtra(LotteryActivity.EXTRA_ACTION, this);
            activity.startActivity(intent);
        }
    }

    @Override
    public String toString() {
        return "ShowEggAction{" +
                "hunger=" + hunger +
                '}';
    }
}
