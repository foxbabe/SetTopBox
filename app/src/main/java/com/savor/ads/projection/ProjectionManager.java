package com.savor.ads.projection;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.savor.ads.projection.action.ProjectionActionBase;
import com.savor.ads.utils.LogUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhang.haiqiang on 2017/5/22.
 */

public class ProjectionManager {

    private static ProjectionManager mInstance;

    private ProjectionManager() {
        mWaitingActions = new LinkedBlockingQueue<>();

        mExecuteThread = new ExecuteThread();
        mExecuteThread.start();
    }

    public static ProjectionManager getInstance() {
        if (mInstance == null) {
            mInstance = new ProjectionManager();
        }
        return mInstance;
    }

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ExecuteThread mExecuteThread;

    private ProjectionActionBase mCurrentAction;

    private BlockingQueue<ProjectionActionBase> mWaitingActions;

    public void enqueueAction(ProjectionActionBase action) {
        log("enqueueAction " + action.toString());
        if (mWaitingActions == null) {
            mWaitingActions = new LinkedBlockingQueue<>();
        }

        if (action.getPriority() == ProjectPriority.HIGH) {
            mWaitingActions.clear();
        }
        mWaitingActions.offer(action);
        mExecuteThread.setSuspend(false);
    }

    private class ExecuteThread extends Thread implements ProjectionListener {
        private Object mLocker = new Object();
        private boolean isSuspend = false;

        /**
         * 动作是否执行结束
         */
        private boolean mIsActionDone;

        public void setSuspend(boolean suspend) {
            log("setSuspend to " + suspend);
            if (!suspend) {
                synchronized (mLocker) {
                    mLocker.notifyAll();
                }
            }

            this.isSuspend = suspend;
        }

        @Override
        public void run() {
            while (true) {
                synchronized (mLocker) {
                    if (isSuspend) {
                        try {
                            mLocker.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                popAndExecute();

                if (!mIsActionDone) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                isSuspend = mWaitingActions.isEmpty();
            }
        }

        private void popAndExecute() {
            log("popAndExecute");

            if (mWaitingActions != null) {
                mCurrentAction = mWaitingActions.poll();
                if (mCurrentAction != null) {
                    log("current action is " + mCurrentAction.toString());
                    mCurrentAction.addActionListener(this);
                    mIsActionDone = false;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mCurrentAction.execute();
                        }
                    });
                }
            }
        }

        @Override
        public void onStart(ProjectionActionBase projection) {
            mIsActionDone = false;
        }

        @Override
        public void onEnd(ProjectionActionBase projection) {
            mIsActionDone = true;
        }
    }


    private static final boolean SHOW_LOG = true;
    public static void log(String msg) {
        if (SHOW_LOG) {
            Log.d("ProjectionManager", msg);
        }
    }
}
