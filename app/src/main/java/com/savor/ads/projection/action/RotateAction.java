package com.savor.ads.projection.action;

import android.app.Activity;
import android.text.TextUtils;

import com.savor.ads.activity.ScreenProjectionActivity;
import com.savor.ads.projection.ProjectPriority;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.GlobalValues;

/**
 * Created by zhang.haiqiang on 2017/5/22.
 */

public class RotateAction extends ProjectionActionBase {
    private int rotateDegree;
    private String projectId;

    public RotateAction(int rotateDegree, String projectId) {
        super();

        mPriority = ProjectPriority.NORMAL;
        this.rotateDegree = rotateDegree;
        this.projectId = projectId;
    }

    @Override
    public void execute() {
        onActionBegin();

        Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
        if (activity instanceof ScreenProjectionActivity) {
            if (!TextUtils.isEmpty(projectId) && projectId.equals(GlobalValues.CURRENT_PROJECT_ID)) {
                ((ScreenProjectionActivity) activity).rotate(rotateDegree);
            }
        }

        onActionEnd();
    }

    @Override
    public String toString() {
        return "RotateAction{" +
                "rotateDegree=" + rotateDegree +
                ", projectId='" + projectId + '\'' +
                '}';
    }
}
