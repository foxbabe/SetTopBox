package com.savor.ads.activity;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.jar.savor.box.vo.HitEggResponseVo;
import com.savor.ads.R;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class LotteryActivity extends BaseActivity {

    private static final int MAX_FRAME = 5;
    private static final int MAX_STAY_DURATION = 2 * 60 * 1000;

    private int mHitSoundId, mBrokenSoundId;
    private SoundPool mSoundPool;

    private int mCurrentFrame;
    private FileWriter mwriter = null;
    private Handler mHandler = new Handler();

    private Runnable mExitLotteryRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.e("mExitProjectionRunnable " + LotteryActivity.this.hashCode());
            resetGlobalFlag();
            exitLottery();
        }
    };

    private Runnable mHitEggEffectRunnable = new Runnable() {
        @Override
        public void run() {
            playHitSound();
            doHitAnimation();
        }
    };

    private Runnable mBrokenEggEffectRunnable = new Runnable() {
        @Override
        public void run() {
            playBrokenSound();
            doBrokenAnimation();
        }
    };

    private int mLastFrameCount = -1;

    private RelativeLayout mRootLayout;
    private ImageView mEggIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery);

        findViews();
        loadSound();
    }

    private void findViews() {
        mRootLayout = (RelativeLayout) findViewById(R.id.rl_root);
        mEggIv = (ImageView) findViewById(R.id.iv_egg);
    }

    private void loadSound() {
        mSoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 0);
        mBrokenSoundId = mSoundPool.load(mContext, R.raw.broken, 1);
        mHitSoundId = mSoundPool.load(mContext, R.raw.hit, 1);
    }

    private void playHitSound() {
        if (mSoundPool != null && mHitSoundId > 0) {
            mSoundPool.play(mHitSoundId, 1, 1, 0, 0, 1);
        }
    }

    private void playBrokenSound() {
        if (mSoundPool != null && mBrokenSoundId > 0) {
            mSoundPool.play(mBrokenSoundId, 1, 1, 0, 0, 1);
        }
    }

    private void doHitAnimation() {

    }

    private void doBrokenAnimation() {

    }

    private void randomFrameCount() {
        Random random = new Random();
        if (mLastFrameCount == 0) {
            mLastFrameCount = random.nextInt(3);
            if (mLastFrameCount == 0) {
                mLastFrameCount = 1;
            }
        } else {
            mLastFrameCount = random.nextInt(3);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSoundPool != null) {
            mSoundPool.release();
        }
    }

    public HitEggResponseVo hitEgg() {
        HitEggResponseVo responseVo = new HitEggResponseVo();
        responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        responseVo.setInfo("成功");

        if (mCurrentFrame < MAX_FRAME) {
            rescheduleToExit(true);
            randomFrameCount();
            mCurrentFrame += mLastFrameCount;
            if (mCurrentFrame >= MAX_FRAME) {
                mCurrentFrame = MAX_FRAME;
                mHandler.post(mBrokenEggEffectRunnable);

                checkIfWin();
            } else {
                mHandler.post(mHitEggEffectRunnable);
            }
        } else {
            responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            responseVo.setInfo("蛋已砸开");
        }
        return responseVo;
    }

    private void checkIfWin() {

    }

    public void exitImmediately() {
        mHandler.post(mExitLotteryRunnable);
    }

    /**
     * 重置定期退出页面计划
     *
     * @param scheduleNewOne 是否重置
     */
    private void rescheduleToExit(boolean scheduleNewOne) {
        LogUtils.e("rescheduleToExit scheduleNewOne=" + scheduleNewOne + " " + this.hashCode());
        mHandler.removeCallbacks(mExitLotteryRunnable);
        if (scheduleNewOne) {
            mHandler.postDelayed(mExitLotteryRunnable, MAX_STAY_DURATION);
        }
    }

    /**
     * 停止投屏
     *
     * @return
     */
    public void stop() {
        LogUtils.e("StopResponseVo will exitProjection " + this.hashCode());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                exitLottery();
            }
        });

        resetGlobalFlag();
    }

    /**
     * 重置全局变量
     */
    private void resetGlobalFlag() {
        GlobalValues.LAST_PROJECT_DEVICE_ID = GlobalValues.CURRENT_PROJECT_DEVICE_ID;
        GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
        GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
        GlobalValues.IS_LOTTERY = false;
        GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
        GlobalValues.CURRENT_PROJECT_ID = null;
    }

    private void exitLottery() {
        LogUtils.e("will exitLottery " + this.hashCode());
        finish();
    }

    private void writeLotteryRecord(String record){
        if (mwriter==null){
            createLotteryRecordFile();
            if (mwriter!=null){
                try {
                    mwriter.write(record);
                    closeWriter();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    private void createLotteryRecordFile(){
        Session session = Session.get(mContext);
        String time = AppUtils.getTime("date");
        String recordFileName = session.getEthernetMac()+"_"+time+".blog";
        String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.lottery);
        try {
            mwriter = new FileWriter(path+recordFileName,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeWriter() {
        if (mwriter != null) {
            try {
                mwriter.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mwriter = null;
        }
    }
}
