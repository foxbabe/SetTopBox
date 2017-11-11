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
import com.savor.ads.database.DBHelper;
import com.savor.ads.log.LogReportUtil;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCodeConstant;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.ShowMessage;
import com.savor.tvlibrary.OutputResolution;
import com.savor.tvlibrary.TVOperatorFactory;

import java.io.File;
import java.util.ArrayList;

/**
 * 广告播放页面
 */
public class AdsPlayerActivity extends BaseActivity implements SavorVideoView.PlayStateCallback {

    private static final String TAG = "AdsPlayerActivity";
    private SavorVideoView mSavorVideoView;

    private ArrayList<PlayListBean> mPlayList;
    private String mListPeriod;
    private boolean mNeedPlayNewer;
    /** 日志用的播放记录标识*/
    private String mUUID;
    private long mActivityResumeTime;

    private static final int DELAY_TIME = 2;

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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

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
        LogFileUtil.write("AdsPlayerActivity checkAndPlay GlobalValues.PLAY_LIST=" + GlobalValues.PLAY_LIST +" AppUtils.getExternalSDCardPath()=" + AppUtils.getSDCardPath());
        // 未发现SD卡时跳到TV
        if (GlobalValues.PLAY_LIST == null || GlobalValues.PLAY_LIST.isEmpty() || TextUtils.isEmpty(AppUtils.getSDCardPath())) {
            Intent intent = new Intent(this, TvPlayerActivity.class);
            startActivity(intent);
            finish();
        } else {
            mPlayList = GlobalValues.PLAY_LIST;
            mListPeriod = mSession.getAdsPeriod();
            doPlay();
        }
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

    private boolean mIsGoneToTv;



    @Override
    protected void onResume() {
        super.onResume();

        LogFileUtil.write("AdsPlayerActivity onResume " + this.hashCode());
        mActivityResumeTime = System.currentTimeMillis();
        if (!mIsGoneToTv) {
            setVolume(mSession.getVolume());
            mSavorVideoView.onResume();
        } else {
            GlobalValues.IS_BOX_BUSY = true;
            ShowMessage.showToast(mContext, "视频节目准备中，即将开始播放");
            mSavorVideoView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setVolume(mSession.getVolume());
                    mSavorVideoView.onResume();
                    mIsGoneToTv = false;
                    GlobalValues.IS_BOX_BUSY = false;
                }
            }, 1000 * DELAY_TIME);
        }
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 禁止进入页面后马上操作
        if (System.currentTimeMillis() - mActivityResumeTime < ConstantValues.KEY_DOWN_LAG + DELAY_TIME * 1000)
            return true;

        boolean handled = false;
        switch (keyCode) {
            // 后退
            case KeyCodeConstant.KEY_CODE_BACK:
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
                //TODO: 临时加上方便测试
            case KeyCodeConstant.KEY_CODE_UP:
                // 对话框弹出后会获得焦点，所以这里不需要处理重复点击重复显示的问题
                showBoxInfo();
                handled = true;
                break;
            case KeyCodeConstant.KEY_CODE_CHANGE_RESOLUTION:
                changeResolution();
                handled = true;
                break;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    int resolutionIndex = 0;
    private void changeResolution() {
        OutputResolution resolution = OutputResolution.values()[(resolutionIndex++) % OutputResolution.values().length];
        TVOperatorFactory.getTVOperator(this, TVOperatorFactory.TVType.GIEC)
                .switchResolution(resolution);
        String msg = "1080P";
        switch (resolution) {
            case RESOLUTION_1080p:
                msg = "1080P";
                break;
            case RESOLUTION_720p:
                msg = "720P";
                break;
            case RESOLUTION_480p:
                msg = "480P";
                break;
        }
        ShowMessage.showToast(getApplicationContext(), msg);
    }

    /**
     * 切换到电视模式
     */
    private void switchToTvPlayer() {
        String vid = "";
        if (mPlayList != null && mCurrentPlayingIndex >= 0 && mCurrentPlayingIndex < mPlayList.size()) {
            vid = mPlayList.get(mCurrentPlayingIndex).getVid();
        }
        mIsGoneToTv = true;
        Intent intent = new Intent(this, TvPlayerActivity.class);
        intent.putExtra(TvPlayerActivity.EXTRA_LAST_VID, vid);
        startActivity(intent);
    }

    private void deleteOldMedia() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                LogUtils.d("删除多余视频");

                // PlayListVersion为空说明没有一个完整的播放列表（初装的时候），这时不做删除操作，以免删掉了手动拷入的视频
                if (mSession.getProPeriod() == null ) {
                    return;
                }

                //排除当前已经完整下载的文件和正在下载的文件，其他删除
                String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.media);
                File[] listFiles = new File(path).listFiles();
                if (listFiles == null || listFiles.length == 0) {
                    return;
                }
                try {
                    DBHelper dbHelper = DBHelper.get(mContext);
                    if (dbHelper.findPlayListByWhere(null, null) == null &&
                            dbHelper.findNewPlayListByWhere(null, null) == null){
                        return;
                    }
                    for (File file : listFiles) {
                        if (file.isFile()) {
                            String selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=?";
                            String[] selectionArgs = new String[]{file.getName()};

                            if (dbHelper.findPlayListByWhere(selection, selectionArgs) == null &&
                                    dbHelper.findNewPlayListByWhere(selection, selectionArgs) == null &&
                                    dbHelper.findAdsByWhere(selection, selectionArgs) == null &&
                                    dbHelper.findNewAdsByWhere(selection, selectionArgs) == null) {
                                file.delete();
                                LogUtils.d("删除文件===================" + file.getName());
                            }
                        } else {
                            FileUtils.deleteFile(file);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public boolean onMediaComplete(int index, boolean isLast) {
        // 这里只是为了防止到这里的时候mUUID没值，正常mUUID肯定会在onMediaPrepared()中赋值
        if (TextUtils.isEmpty(mUUID)) {
            mUUID = String.valueOf(System.currentTimeMillis());
        }
        if (mPlayList != null && !TextUtils.isEmpty(mPlayList.get(index).getVid())) {
            LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "end", mPlayList.get(index).getMedia_type(), mPlayList.get(index).getVid(),
                    "", mSession.getVersionName(), mListPeriod, mSession.getVodPeriod(),
                    "");
        }

        // 新一期下载完成时重新获取播放列表开始播放
        if (isLast && mNeedPlayNewer) {
            mNeedPlayNewer = false;
            if (GlobalValues.PLAY_LIST != null && !GlobalValues.PLAY_LIST.equals(mPlayList)) {
                mSavorVideoView.stop();
                checkAndPlay();
                deleteOldMedia();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean onMediaError(int index, boolean isLast) {
        // 新一期下载完成时重新获取播放列表开始播放
        if (isLast && mNeedPlayNewer) {
            mNeedPlayNewer = false;
            if (GlobalValues.PLAY_LIST != null && !GlobalValues.PLAY_LIST.equals(mPlayList)) {
                mSavorVideoView.stop();
                checkAndPlay();
                deleteOldMedia();
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private int mCurrentPlayingIndex = -1;
    @Override
    public void onMediaPrepared(int index) {
        // 准备播放新视频时产生一个新的UUID作为日志标识
        String action = "start";
        if (mCurrentPlayingIndex != index) {
            mUUID = String.valueOf(System.currentTimeMillis());
            mCurrentPlayingIndex = index;
            action = "start";
        } else {
            // 这里只是为了防止到这里的时候mUUID没值，正常mUUID肯定会在onMediaPrepared()中赋值
            if (TextUtils.isEmpty(mUUID)) {
                mUUID = String.valueOf(System.currentTimeMillis());
            }
            action = "resume";
        }
        if (mPlayList != null && !TextUtils.isEmpty(mPlayList.get(index).getVid())) {
            LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), action, mPlayList.get(index).getMedia_type(), mPlayList.get(index).getVid(),
                    "", mSession.getVersionName(), mListPeriod, mSession.getVodPeriod(),
                    "");
        }
    }

    @Override
    public void onMediaPause(int index) {
        // 这里只是为了防止到这里的时候mUUID没值，正常mUUID肯定会在onMediaPrepared()中赋值
        if (TextUtils.isEmpty(mUUID)) {
            mUUID = String.valueOf(System.currentTimeMillis());
        }
        try {
            if (mPlayList != null && !TextUtils.isEmpty(mPlayList.get(index).getVid())) {
                LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "pause", mPlayList.get(index).getMedia_type(), mPlayList.get(index).getVid(),
                        "", mSession.getVersionName(), mListPeriod, mSession.getVodPeriod(),
                        "");
            }
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
        if (mPlayList != null && !TextUtils.isEmpty(mPlayList.get(index).getVid())) {
            LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), "resume", mPlayList.get(index).getMedia_type(), mPlayList.get(index).getVid(),
                    "", mSession.getVersionName(), mListPeriod, mSession.getVodPeriod(),
                    "");
        }
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
