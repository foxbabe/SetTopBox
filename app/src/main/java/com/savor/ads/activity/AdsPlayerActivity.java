package com.savor.ads.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.savor.ads.R;
import com.savor.ads.SavorApplication;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.customview.SavorVideoView;
import com.savor.ads.log.LogReportUtil;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCodeConstant;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import java.util.ArrayList;

/**
 * 广告播放页面
 */
public class AdsPlayerActivity extends BaseActivity implements SavorVideoView.PlayStateCallback {

    private static final String TAG = "AdsPlayerActivity";
    private SavorVideoView mSavorVideoView;

    private String mPeriod;
    private ArrayList<PlayListBean> mPlayList;
    private boolean mNeedPlayNewer;
    /** 日志用的播放记录标识*/
    private String mUUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ads_player);

        mSavorVideoView = (SavorVideoView) findViewById(R.id.video_view);
        mSavorVideoView.setIfShowPauseBtn(false);
        mSavorVideoView.setIfShowLoading(false);
        mSavorVideoView.setIfHandlePrepareTimeout(true);
        mSavorVideoView.setPlayStateCallback(this);

        registerDownloadReceiver();
        LogFileUtil.write("AdsPlayerActivity onCreate " + System.currentTimeMillis());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogFileUtil.write("AdsPlayerActivity onNewIntent" + intent.toString());
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mPeriod = mSession.getAdvertMediaPeriod();
        checkAndPlay();
    }

    private void registerDownloadReceiver() {
        IntentFilter intentFilter = new IntentFilter(ConstantValues.ADS_DOWNLOAD_COMPLETE_ACCTION);
        registerReceiver(mDownloadCompleteReceiver, intentFilter);
    }

    private BroadcastReceiver mDownloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.d("收到下载完成广播");
            mNeedPlayNewer = true;
        }
    };

    private void checkAndPlay() {
        LogFileUtil.write("AdsPlayerActivity checkAndPlay");
        // 未发现SD卡时跳到TV
        mPlayList = GlobalValues.PLAY_LIST;
        if (mPlayList == null || mPlayList.isEmpty() || TextUtils.isEmpty(AppUtils.getExternalSDCardPath())) {
            Intent intent = new Intent(this, TvPlayerActivity.class);
            startActivity(intent);
            finish();
        } else {
            doPlay();
        }
//        if ( || TextUtils.isEmpty(mPeriod)) {
//
//            return;
//        }
    }

    private void doPlay() {
        LogFileUtil.write("AdsPlayerActivity doPlay");
        ArrayList<String> urls = new ArrayList<>();
        if (mPlayList != null && mPlayList.size() > 0) {
            for (PlayListBean bean : mPlayList) {
                urls.add(bean.getMediaPath());
            }

            mSavorVideoView.setMediaFiles(urls);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LogFileUtil.write("AdsPlayerActivity onResume " + this.hashCode());

        setVolume(mSession.getVolume());
        mSavorVideoView.onResume();
    }



    @Override
    protected void onStart() {
        LogFileUtil.write("AdsPlayerActivity onStart " + this.hashCode());
        super.onStart();
    }

    @Override
    protected void onRestart() {
        LogFileUtil.write("AdsPlayerActivity onRestart " + this.hashCode());
        super.onRestart();

//        if (!TextUtils.isEmpty(mSession.getAdvertMediaPeriod())) {
//            // Resume时判断是否期号已改变，改变的话去查新的播放表
//            if (mSession.getAdvertMediaPeriod().equals(mPeriod)) {
////                if (!mIsFirstResume) {

//                    mSavorVideoView.onResume();

////                }
//            } else {
//                mPeriod = mSession.getAdvertMediaPeriod();
//                checkAndPlay();
//            }
//        }
    }

    @Override
    protected void onStop() {
        LogFileUtil.write("AdsPlayerActivity onStop " + this.hashCode());
        mSavorVideoView.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
        LogFileUtil.write("AdsPlayerActivity onPause " + this.hashCode());
        mSavorVideoView.onPause();
        super.onPause();
    }

    void handleBack() {
        mSavorVideoView.release();
        finish();

//        if (NettyClient.get() != null) {
//            NettyClient.get().disConnect();
//        }
//        ((SavorApplication) getApplication()).stopScreenProjectionService();
//        System.exit(0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        switch (keyCode) {
            // 后退
            case KeyCodeConstant.KEY_CODE_BACK:
//                handleBack();
                handled = true;
                break;
            // 切换到电视模式
            case KeyCodeConstant.KEY_CODE_CHANGE_MODE:
                switchToTvPlayer();
                handled = true;
                break;
            // 呼出二维码
            case KeyCodeConstant.KEY_CODE_SHOW_QRCODE:
                ((SavorApplication) getApplication()).showQrCodeWindow(null);
                handled = true;
                break;
            // 暂停、继续播放
            case KeyCodeConstant.KEY_CODE_PLAY_PAUSE:
                mSavorVideoView.togglePlay();
                handled = true;
                break;
            // 上一条
            case KeyCodeConstant.KEY_CODE_PREVIOUS_ADS:
                mSavorVideoView.playPrevious();
                handled = true;
                break;
            // 下一条
            case KeyCodeConstant.KEY_CODE_NEXT_ADS:
                mSavorVideoView.playNext();
                handled = true;
                break;
            // 机顶盒信息
            case KeyCodeConstant.KEY_CODE_SHOW_INFO:
                // 对话框弹出后会获得焦点，所以这里不需要处理重复点击重复显示的问题
                showBoxInfo();
                handled = true;
                break;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    /**
     * 切换到电视模式
     */
    private void switchToTvPlayer() {
        String vid = "";
        if (mPlayList != null && mCurrentPlayingIndex >= 0 && mCurrentPlayingIndex < mPlayList.size()) {
            vid = mPlayList.get(mCurrentPlayingIndex).getVid();
        }
        Intent intent = new Intent(this, TvPlayerActivity.class);
        intent.putExtra(TvPlayerActivity.EXTRA_LAST_VID, vid);
        startActivity(intent);
    }

    public void onNewPeriodCome() {
        mNeedPlayNewer = true;
    }

    @Override
    public boolean onMediaComplete(int index, boolean isLast) {
        // 这里只是为了防止到这里的时候mUUID没值，正常mUUID肯定会在onMediaPrepared()中赋值
        if (TextUtils.isEmpty(mUUID)) {
            mUUID = String.valueOf(System.currentTimeMillis());
        }
        if (mPlayList != null) {
            LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "end", mPlayList.get(index).getMedia_type(), mPlayList.get(index).getVid(),
                    "", mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(),
                    "");
        }

        // 新一期下载完成时重新获取播放列表开始播放
        if (isLast && mNeedPlayNewer) {
            mNeedPlayNewer = false;
            mSavorVideoView.stop();
            checkAndPlay();

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onMediaError(int index, boolean isLast) {
        // 新一期下载完成时重新获取播放列表开始播放
        if (isLast && mNeedPlayNewer) {
            mNeedPlayNewer = false;
            mSavorVideoView.stop();
            checkAndPlay();

            return true;
        } else {
            return false;
        }
    }

    private int mCurrentPlayingIndex = -1;
    @Override
    public void onMediaPrepared(int index) {
        // 准备播放新视频时产生一个新的UUID作为日志标识
        mUUID = String.valueOf(System.currentTimeMillis());
        String action = "start";
        if (mCurrentPlayingIndex != index) {
            mCurrentPlayingIndex = index;
            action = "start";
        } else {
            action = "resume";
        }
        LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                String.valueOf(System.currentTimeMillis()), action, mPlayList.get(index).getMedia_type(), mPlayList.get(index).getVid(),
                "", mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(),
                "");
    }

    @Override
    public void onMediaPause(int index) {
        // 这里只是为了防止到这里的时候mUUID没值，正常mUUID肯定会在onMediaPrepared()中赋值
        if (TextUtils.isEmpty(mUUID)) {
            mUUID = String.valueOf(System.currentTimeMillis());
        }
        try {
            LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "pause", mPlayList.get(index).getMedia_type(), mPlayList.get(index).getVid(),
                    "", mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(),
                    "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMediaResume(int index) {
        // 这里只是为了防止到这里的时候mUUID没值，正常mUUID肯定会在onMediaPrepared()中赋值
        if (TextUtils.isEmpty(mUUID)) {
            mUUID = String.valueOf(System.currentTimeMillis());
        }
        LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                String.valueOf(System.currentTimeMillis()), "resume", mPlayList.get(index).getMedia_type(), mPlayList.get(index).getVid(),
                "", mSession.getVersionName(), mSession.getAdvertMediaPeriod(), mSession.getMulticastMediaPeriod(),
                "");
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LogFileUtil.write("AdsPlayerActivity onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        LogFileUtil.write("AdsPlayerActivity onDestroy");
        super.onDestroy();
        unregisterReceiver(mDownloadCompleteReceiver);
    }
}
