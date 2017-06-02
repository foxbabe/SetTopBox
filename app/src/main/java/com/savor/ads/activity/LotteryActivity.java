package com.savor.ads.activity;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jar.savor.box.vo.HitEggResponseVo;
import com.savor.ads.R;
import com.savor.ads.SavorApplication;
import com.savor.ads.bean.PrizeItem;
import com.savor.ads.log.LotteryLogUtil;
import com.savor.ads.projection.action.ShowEggAction;
import com.savor.ads.projection.action.StopAction;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCodeConstant;
import com.savor.ads.utils.LogUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class LotteryActivity extends BaseActivity {

    public static final String EXTRA_HUNGER = "extra_hunger";
    public static final String EXTRA_ACTION = "extra_action";

    private static final int MAX_STAY_DURATION = 2 * 60 * 1000;
    private static final int WIN_STAY_SECONDS = 60;
    private static final int LOSE_STAY_SECONDS = 30;

    private int mBrokenSoundId;
    private SoundPool mSoundPool;

    private int mCurrentFrame;
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
            playBrokenSound();
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

    private int mCurrentCountDown;
    private Runnable mWinExitCountDownRunnable = new Runnable() {
        @Override
        public void run() {
            if (mWinTimeTv != null) {
                if (mCurrentCountDown > 0) {
                    mWinTimeTv.setText(--mCurrentCountDown + "秒");
                    mHandler.postDelayed(mWinExitCountDownRunnable, 1000);
                } else {
                    resetGlobalFlag();
                    exitLottery();
                }
            }
        }
    };
    private Runnable mLoseExitCountDownRunnable = new Runnable() {
        @Override
        public void run() {
            if (mLoseTimeTv != null) {
                if (mCurrentCountDown > 0) {
                    mLoseTimeTv.setText(mCurrentCountDown-- + "秒");
                    mHandler.postDelayed(mLoseExitCountDownRunnable, 1000);
                } else {
                    resetGlobalFlag();
                    exitLottery();
                }
            }
        }
    };

    private int mLastFrameCount = -1;
    /** 手机传过来的标识，表示是否到达应中奖的次数*/
    private int mHunger;

    private RelativeLayout mRootLayout;
    private ImageView mEggIv;
    private TextView mProjectTipTv;
    private RelativeLayout mWinDialogRl;
    private ImageView mPrizeNameIv;
    private TextView mPrizeTimeTv;
    private TextView mEndTimeTv;
    private RelativeLayout mLoseDialogRl;
    private TextView mWinTimeTv;
    private TextView mLoseTimeTv;
    private ImageView mLostTipIv;

    /**蛋列图组*/
    private int[] EGG_FRAMES = new int[]{
            R.mipmap.egg1,
            R.mipmap.egg2,
            R.mipmap.egg3,
            R.mipmap.egg4,
            R.mipmap.egg5,
            R.mipmap.egg6,
    };

    /**未中奖提示语*/
    private int[] FAIL_TIPS = new int[]{
            R.mipmap.ic_egg_fail_tip1,
            R.mipmap.ic_egg_fail_tip2,
            R.mipmap.ic_egg_fail_tip3,
            R.mipmap.ic_egg_fail_tip4,
    };

    private PrizeItem mPrizeHit;
    private Date mPrizeTime;

    private ShowEggAction mShowEggAction;
    private StopAction mStopAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lottery);

        mHunger = getIntent().getIntExtra(EXTRA_HUNGER, 0);
        mShowEggAction = (ShowEggAction) getIntent().getSerializableExtra(EXTRA_ACTION);
        if (mShowEggAction != null) {
            mShowEggAction.onActionEnd();
        }
        findViews();
        loadSound();
        setViews();

        rescheduleToExit(true);
    }

    private void findViews() {
        mRootLayout = (RelativeLayout) findViewById(R.id.rl_root);
        mEggIv = (ImageView) findViewById(R.id.iv_egg);
        mProjectTipTv = (TextView) findViewById(R.id.tv_project_tip);
        mPrizeNameIv = (ImageView) findViewById(R.id.iv_prize_name);
        mWinDialogRl = (RelativeLayout) findViewById(R.id.rl_win_dialog);
        mPrizeTimeTv = (TextView) findViewById(R.id.tv_prize_time);
        mEndTimeTv = (TextView) findViewById(R.id.tv_prize_end_time);
        mLoseDialogRl = (RelativeLayout) findViewById(R.id.rl_lose_dialog);
        mWinTimeTv = (TextView) findViewById(R.id.tv_win_exit_time);
        mLoseTimeTv = (TextView) findViewById(R.id.tv_lose_exit_time);
        mLostTipIv = (ImageView) findViewById(R.id.iv_fail_tips);
    }

    private void loadSound() {
        mSoundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 0);
        mBrokenSoundId = mSoundPool.load(mContext, R.raw.broken, 1);
    }

    private void setViews() {
        if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_NAME)) {
            mProjectTipTv.setText(GlobalValues.CURRENT_PROJECT_DEVICE_NAME + "正在砸蛋");
            mProjectTipTv.setVisibility(View.VISIBLE);
        } else {
            mProjectTipTv.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mHandler.removeCallbacksAndMessages(null);
        rescheduleToExit(true);

        mHunger = intent.getIntExtra(EXTRA_HUNGER, 0);
        mShowEggAction = (ShowEggAction) intent.getSerializableExtra(EXTRA_ACTION);
        if (mShowEggAction != null) {
            mShowEggAction.onActionEnd();
        }
        mPrizeHit = null;
        mPrizeTime = null;
        mCurrentFrame = 0;
        mLastFrameCount = -1;
        mRootLayout.setBackgroundResource(R.mipmap.bg_egg);
        mEggIv.setImageResource(R.mipmap.egg1);
        mEggIv.setVisibility(View.VISIBLE);
        mWinDialogRl.setVisibility(View.GONE);
        mLoseDialogRl.setVisibility(View.GONE);
    }

    private void playBrokenSound() {
        if (mSoundPool != null && mBrokenSoundId > 0) {
            mSoundPool.play(mBrokenSoundId, 1, 1, 0, 0, 1);
        }
    }

    private void doHitAnimation() {
        if (mCurrentFrame < EGG_FRAMES.length) {
            mEggIv.setImageResource(EGG_FRAMES[mCurrentFrame]);
        }
    }

    private void doBrokenAnimation() {
        mEggIv.setVisibility(View.GONE);
        if (mPrizeHit != null) {
            mRootLayout.setBackgroundResource(R.drawable.anim_egg_bg_win);
            AnimationDrawable animationDrawable = (AnimationDrawable) mRootLayout.getBackground();
            animationDrawable.start();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mWinDialogRl.setVisibility(View.VISIBLE);
                    switch (mPrizeHit.getPrize_level()) {
                        case 1:
                            mPrizeNameIv.setImageResource(R.mipmap.ic_prize1);
                            break;
                        case 2:
                            mPrizeNameIv.setImageResource(R.mipmap.ic_prize2);
                            break;
                        case 3:
                            mPrizeNameIv.setImageResource(R.mipmap.ic_prize3);
                            break;
                        default:
                            break;
                    }
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                    mPrizeTimeTv.setText("中奖时间：  " + simpleDateFormat.format(mPrizeTime));
                    int hour = (mPrizeTime.getHours() + 1) % 24;
                    mEndTimeTv.setText("有效领奖时间至" + hour + ":" + mPrizeTime.getMinutes());

                    mRootLayout.setBackgroundResource(R.mipmap.bg_egg_win_prize);

                    mCurrentCountDown = WIN_STAY_SECONDS;
                    mHandler.post(mWinExitCountDownRunnable);
                }
            }, 400);
        } else {
            mRootLayout.setBackgroundResource(R.drawable.anim_egg_bg_lose);
            AnimationDrawable animationDrawable = (AnimationDrawable) mRootLayout.getBackground();
            animationDrawable.start();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLoseDialogRl.setVisibility(View.VISIBLE);
                    mRootLayout.setBackgroundResource(R.mipmap.bg_egg_broken_lose);

                    Random random = new Random();
                    int index = random.nextInt(FAIL_TIPS.length);
                    mLostTipIv.setImageResource(FAIL_TIPS[index]);

                    mCurrentCountDown = LOSE_STAY_SECONDS;
                    mHandler.post(mLoseExitCountDownRunnable);
                }
            }, 400);
        }
    }

    private void randomFrameCount() {
        Random random = new Random();
        mLastFrameCount = random.nextInt(4);
        if (mLastFrameCount == 0) {
            mLastFrameCount++;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mSoundPool != null) {
            mSoundPool.release();
        }
        mHandler.removeCallbacksAndMessages(null);
        if (mStopAction != null) {
            mStopAction.onActionEnd();
        }
    }

    public HitEggResponseVo hitEgg() {
        HitEggResponseVo responseVo = new HitEggResponseVo();
        responseVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
        responseVo.setInfo("成功");

        if (mCurrentFrame < EGG_FRAMES.length) {
            rescheduleToExit(true);
            randomFrameCount();

            mCurrentFrame += mLastFrameCount;
            if (mCurrentFrame >= EGG_FRAMES.length) {
//                mCurrentFrame = MAX_FRAME;

                mPrizeTime = new Date();
                checkIfWin();
                mHandler.post(mBrokenEggEffectRunnable);

                if (mPrizeHit != null) {
                    LotteryLogUtil.getInstance(mContext).writeLotteryRecord(mPrizeHit.getPrize_id(), mPrizeHit.getPrize_name(), String.valueOf(mPrizeTime.getTime()));
                } else {
                    LotteryLogUtil.getInstance(mContext).writeLotteryRecord(0, "", String.valueOf(mPrizeTime.getTime()));
                }
            } else {
                mHandler.post(mHitEggEffectRunnable);
            }
        }

        responseVo.setProgress(mCurrentFrame);
        if (mCurrentFrame < EGG_FRAMES.length) {
            responseVo.setDone(0);
        } else {
            responseVo.setDone(1);
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//            responseVo.setPrize_time(simpleDateFormat.format(mPrizeTime));
            responseVo.setPrize_time("" + mPrizeTime.getTime());
            if (mPrizeHit != null) {
                responseVo.setWin(1);
                responseVo.setPrize_id(mPrizeHit.getPrize_id());
                responseVo.setPrize_name(mPrizeHit.getPrize_name());
                responseVo.setPrize_level(mPrizeHit.getPrize_level());

                // 剩余奖品数减一
                mPrizeHit.setPrize_num(mPrizeHit.getPrize_num() - 1);
                mSession.setPrizeInfo(mSession.getPrizeInfo());
            } else {
                responseVo.setWin(0);
                responseVo.setPrize_id(0);
                responseVo.setPrize_name("");
            }
        }
        return responseVo;
    }

    private void checkIfWin() {
        if (mHunger == 1) {
            if (mSession.getPrizeInfo() != null) {
                Random random = new Random();
                int denominator = 0;
                ArrayList<PrizeItem> prizeList = new ArrayList<>();
                ArrayList<Integer> mStartPosList = new ArrayList<>();
                List<PrizeItem> prize = mSession.getPrizeInfo().getPrize();
                for (int i = 0; i < prize.size(); i++) {
                    PrizeItem item = prize.get(i);
                    if (item.getPrize_num() > 0) {
                        prizeList.add(item);
                        denominator += item.getPrize_pos();

                        int startPos = item.getPrize_pos();
                        if (mStartPosList.size() > 0) {
                            startPos += mStartPosList.get(mStartPosList.size() - 1);
                        }
                        mStartPosList.add(startPos);
                    }
                }
                if (!mStartPosList.isEmpty()) {
                    mStartPosList.remove(mStartPosList.size() - 1);
                    mStartPosList.add(0, 0);
                }

                if (denominator > 0) {
                    int hit = random.nextInt(denominator);
                    LogUtils.d("计算是否中奖 denominator=" + denominator + " hit=" + hit);
                    for (int i = prizeList.size() - 1; i >= 0; i--) {
                        PrizeItem item = prizeList.get(i);
                        LogUtils.d("计算是否中奖 start position=" + mStartPosList.get(i));
                        if (hit >= mStartPosList.get(i)) {
                            mPrizeHit = item;
                            break;
                        }
                    }
                    if (mPrizeHit != null) {
                        // 中奖了
                        LogUtils.w("----win----");
                    } else {
                        LogUtils.e("----lose----, cause calculate exception");
                    }
                } else {
                    // 未中奖，因为剩余奖项数量为0
                    LogUtils.w("----lose----, cause no prize number");
                }
            } else {
                // 未中奖，因为没有奖项配置信息
                LogUtils.w("----lose----, cause no prize info");
            }
        } else {
            // 未中奖，因为hunger=0
            LogUtils.w("----lose----, cause hunger=0");
        }
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
    public void stop(StopAction stopAction) {
        LogUtils.e("StopResponseVo will exitProjection " + this.hashCode());
        mStopAction = stopAction;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                exitLottery();
            }
        });

        resetGlobalFlag();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            case KeyCodeConstant.KEY_CODE_BACK:
                resetGlobalFlag();
                exitLottery();
                handled = true;
                break;
            // 呼出二维码
            case KeyCodeConstant.KEY_CODE_SHOW_QRCODE:
                ((SavorApplication) getApplication()).showQrCodeWindow(null);
                handled = true;
                break;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    /**
     * 重置全局变量
     */
    private void resetGlobalFlag() {
        GlobalValues.LAST_PROJECT_DEVICE_ID = GlobalValues.CURRENT_PROJECT_DEVICE_ID;
        GlobalValues.LAST_PROJECT_ID = GlobalValues.CURRENT_PROJECT_ID;
        GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
        GlobalValues.CURRENT_PROJECT_DEVICE_IP = null;
        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
        GlobalValues.IS_LOTTERY = false;
        GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
        GlobalValues.CURRENT_PROJECT_ID = null;
    }

    private void exitLottery() {
        LogUtils.e("will exitLottery " + this.hashCode());
        finish();
    }


}
