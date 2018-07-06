package com.savor.ads.activity;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;

import com.admaster.sdk.api.AdmasterSdk;
import com.google.protobuf.ByteString;
import com.jar.savor.box.ServiceUtil;
import com.jar.savor.box.services.RemoteService;
import com.savor.ads.R;
import com.savor.ads.SavorApplication;
import com.savor.ads.bean.AdMasterResult;
import com.savor.ads.bean.BaiduAdLocalBean;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.customview.SavorVideoView;
import com.savor.ads.database.DBHelper;
import com.savor.ads.dialog.PlayListDialog;
import com.savor.ads.log.LogReportUtil;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.BaiduAdsResponseCode;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.DensityUtil;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCode;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.RetryHandler;
import com.savor.ads.utils.ShowMessage;
import com.savor.tvlibrary.OutputResolution;
import com.savor.tvlibrary.TVOperatorFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import tianshu.ui.api.TsUiApiV20171122;

/**
 * 广告播放页面
 */
public class AdsPlayerActivity<T extends MediaLibBean> extends BaseActivity implements SavorVideoView.PlayStateCallback, ApiRequestListener, PlayListDialog.Callback {

    private static final String TAG = "AdsPlayerActivity";
    private SavorVideoView mSavorVideoView;

    private ArrayList<T> mPlayList;
    private String mListPeriod;
    private boolean mNeedUpdatePlaylist;
    /**
     * 日志用的播放记录标识
     */
    private String mUUID;
    private long mActivityResumeTime;

    private static final int DELAY_TIME = 2;
    private AdMasterResult adMasterResult = null;

    private PlayListDialog mPlayListDialog;

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
        // 启动投屏类操作处理的Service
        startScreenProjectionService();
//        LogFileUtil.write("AdsPlayerActivity onCreate " + System.currentTimeMillis());
        // SDK初始化
        AdmasterSdk.init(this, ConstantValues.CONFIG_URL);
        AdmasterSdk.setLogState(true);

//        AppApi.getAdMasterConfig(this,this);

    }


    private ServiceConnection mConnection;

    private void startScreenProjectionService() {
        mConnection = ServiceUtil.registerService(ProjectOperationListener.getInstance(this));
//        bindService(new Intent(ServiceUtil.ACTION_REMOTE_SERVICE), connection, Service.BIND_AUTO_CREATE);
        bindService(new Intent(this, RemoteService.class), mConnection, Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        checkAndPlay(-1);
    }

    private void registerDownloadReceiver() {
        IntentFilter intentFilter = new IntentFilter(ConstantValues.UPDATE_PLAYLIST_ACTION);
        registerReceiver(mDownloadCompleteReceiver, intentFilter);
    }

    private BroadcastReceiver mDownloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConstantValues.UPDATE_PLAYLIST_ACTION.equals(intent.getAction())) {
                LogUtils.d("收到更新播放列表广播");
                mNeedUpdatePlaylist = true;
            }
        }
    };

    private void checkAndPlay(int lastMediaOrder) {
//        LogFileUtil.write("AdsPlayerActivity checkAndPlay GlobalValues.PLAY_LIST=" + GlobalValues.PLAY_LIST + " AppUtils.getMainMediaPath()=" + AppUtils.getMainMediaPath());
        // 未发现SD卡时跳到TV
        if (GlobalValues.getInstance().PLAY_LIST == null || GlobalValues.getInstance().PLAY_LIST.isEmpty() || TextUtils.isEmpty(AppUtils.getMainMediaPath())) {
            if (AppUtils.isMstar()) {
                Intent intent = new Intent(this, TvPlayerActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, TvPlayerGiecActivity.class);
                startActivity(intent);
            }
            AppApi.reportSDCardState(mContext, null, 1);
            finish();
        } else {
            mPlayList = GlobalValues.getInstance().PLAY_LIST;
            mListPeriod = mSession.getAdsPeriod();
            doPlay(lastMediaOrder);
        }
    }

    private void doPlay(int lastMediaOrder) {
//        LogFileUtil.write("AdsPlayerActivity doPlay");
        ArrayList<String> urls = new ArrayList<>();
        if (mPlayList != null && mPlayList.size() > 0) {
            int index = 0;
            for (int i = 0; i < mPlayList.size(); i++) {
                MediaLibBean bean = mPlayList.get(i);
                if (bean.getOrder() > lastMediaOrder) {
                    index = i;
                    break;
                }
            }
            for (int i = 0; i < mPlayList.size(); i++) {
                MediaLibBean bean = mPlayList.get(i);
                urls.add(bean.getMediaPath());
            }
            mSavorVideoView.setMediaFiles(urls, index, 0);
        }
    }

    /**
     * Play the current video while detecting the next video that needs to be played if the video is poly,
     * and if so,
     * call the interface to determine if there is a resource that can be played on a poly video.
     *
     * @param index
     */
    private void toCheckIfPolyAds(int index) {
        if (mPlayList != null) {
            int next = (index + 1) % mPlayList.size();
            if (next < mPlayList.size()) {
                MediaLibBean bean = mPlayList.get(next);
                // 当下一个位置是聚屏类、且未被填充媒体内容、且当前未被“未发现百度广告”阻止时，请求百度聚屏广告
                if (ConstantValues.POLY_ADS.equals(bean.getType()) &&
                        TextUtils.isEmpty(bean.getName()) &&
                        TextUtils.isEmpty(GlobalValues.NOT_FOUND_BAIDU_ADS_KEY)) {
                    if (GlobalValues.CURRENT_ADS_REPEAT_PAIR == null ||
                            GlobalValues.CURRENT_ADS_REPEAT_PAIR.second < ConstantValues.MAX_BAIDU_ADS_REPEAT_COUNT) {
                        requestBaiduAds();
                    } else {
                        if (GlobalValues.CURRENT_ADS_BLOCKED_COUNT < ConstantValues.BAIDU_ADS_BLOCK_COUNT) {
                            GlobalValues.CURRENT_ADS_BLOCKED_COUNT++;
                        } else {
                            GlobalValues.CURRENT_ADS_BLOCKED_COUNT = 0;
                            GlobalValues.CURRENT_ADS_REPEAT_PAIR = null;
                            requestBaiduAds();
                        }
                    }
                }
            }
        }
    }

    /**
     * 请求百度聚屏广告
     */
    private void requestBaiduAds() {
        String release = mSession.getBuildVersion();
        String[] ver = release.split("[.]");
        int verMajor = 0, verMinor = 0, verMicro = 0;
        for (int i = 0; i < ver.length; i++) {
            int temp = 0;
            try {
                temp = Integer.parseInt(ver[i]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            switch (i) {
                case 0:
                    verMajor = temp;
                    break;
                case 1:
                    verMinor = temp;
                    break;
                case 2:
                    verMicro = temp;
                    break;
            }
        }

        Point point = DensityUtil.getScreenRealSize(this);

        ByteString uuid = null, appId = null, adslotId = null, mac = null, model = null, brand = null, ip = null;
        try {
            uuid = ByteString.copyFrom(UUID.randomUUID().toString().replace("-", ""), "utf-8");
            appId = ByteString.copyFrom(ConstantValues.BAIDU_ADS_APP_ID, "utf-8");
            if (mSession.getTvSize()>0&&mSession.getTvSize()<=40){
                adslotId = ByteString.copyFrom(ConstantValues.BAIDU_ADSLOT_ID1, "utf-8");
            }else if (mSession.getTvSize()>40&&mSession.getTvSize()<=45){
                adslotId = ByteString.copyFrom(ConstantValues.BAIDU_ADSLOT_ID2, "utf-8");
            }else if (mSession.getTvSize()>45&&mSession.getTvSize()<=50){
                adslotId = ByteString.copyFrom(ConstantValues.BAIDU_ADSLOT_ID3, "utf-8");
            }else if (mSession.getTvSize()>50&&mSession.getTvSize()<=55){
                adslotId = ByteString.copyFrom(ConstantValues.BAIDU_ADSLOT_ID4, "utf-8");
            }else if (mSession.getTvSize()>55){
                adslotId = ByteString.copyFrom(ConstantValues.BAIDU_ADSLOT_ID5, "utf-8");
            }
            mac = ByteString.copyFrom(mSession.getEthernetMacWithColon(), "utf-8");
            model = ByteString.copyFrom(mSession.getModel(), "utf-8");
            brand = ByteString.copyFrom(mSession.getBrand(), "utf-8");
            ip = ByteString.copyFrom(AppUtils.getLocalIPAddress(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (adslotId==null){
            return;
        }
        TsUiApiV20171122.TsApiRequest request = TsUiApiV20171122.TsApiRequest.newBuilder()
                .setRequestId(uuid)
                .setApiVersion(TsUiApiV20171122.Version.newBuilder()
                        .setMajor(6).setMinor(0).setMicro(0))
                .setAppId(appId)
                .setSlot(TsUiApiV20171122.SlotInfo.newBuilder()
                        .setAdslotId(adslotId))
                .setDevice(TsUiApiV20171122.Device.newBuilder()
                        .setUdid(TsUiApiV20171122.UdId.newBuilder()
                                .setIdType(TsUiApiV20171122.UdIdType.MAC)
                                .setId(mac))
                        .setModel(model)
                        .setOsType(TsUiApiV20171122.OsType.ANDROID)
                        .setOsVersion(TsUiApiV20171122.Version.newBuilder().setMajor(verMajor).setMinor(verMinor).setMicro(verMicro))
                        .setScreenSize(TsUiApiV20171122.Size.newBuilder().setWidth(point.x).setHeight(point.y))
                        .setVendor(brand))
                .setNetwork(TsUiApiV20171122.Network.newBuilder()
                        .setConnectionType(TsUiApiV20171122.Network.ConnectionType.ETHERNET)
                        .setOperatorType(TsUiApiV20171122.Network.OperatorType.ISP_UNKNOWN)
                        .setIpv4(ip))
                .build();
        AppApi.requestBaiduAds(this, this, request);
    }

    private boolean mIsGoneToTv;


    @Override
    protected void onResume() {
        super.onResume();

//        LogFileUtil.write("AdsPlayerActivity onResume " + this.hashCode());
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
//        LogFileUtil.write("AdsPlayerActivity onStart " + this.hashCode());
        super.onStart();
    }

    @Override
    protected void onRestart() {
//        LogFileUtil.write("AdsPlayerActivity onRestart " + this.hashCode());
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
//        LogFileUtil.write("AdsPlayerActivity onStop " + this.hashCode());
        mSavorVideoView.onStop();
        super.onStop();
    }

    @Override
    protected void onPause() {
//        LogFileUtil.write("AdsPlayerActivity onPause " + this.hashCode());
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
        // 禁止进入页面后马上操作
        if (System.currentTimeMillis() - mActivityResumeTime < ConstantValues.KEY_DOWN_LAG + DELAY_TIME * 1000)
            return true;

        boolean handled = false;
        if (keyCode == KeyEvent.KEYCODE_BACK) {//                handleBack();
            handled = true;

            // 切换到电视模式
        } else if (keyCode == KeyCode.KEY_CODE_CHANGE_MODE) {
            switchToTvPlayer();
            handled = true;

            // 呼出二维码
        } else if (keyCode == KeyCode.KEY_CODE_SHOW_QRCODE) {
            ((SavorApplication) getApplication()).showQrCodeWindow(null);
            handled = true;

            // 暂停、继续播放
        } else if (keyCode == KeyCode.KEY_CODE_PLAY_PAUSE) {
            mSavorVideoView.togglePlay();
            handled = true;

            // 上一条
        } else if (keyCode == KeyCode.KEY_CODE_PREVIOUS_ADS) {
            mSavorVideoView.playPrevious();
            handled = true;

            // 下一条
        } else if (keyCode == KeyCode.KEY_CODE_NEXT_ADS) {
            mSavorVideoView.playNext();
            handled = true;

            // 机顶盒信息
        } else if (keyCode == KeyCode.KEY_CODE_SHOW_INFO) {// 对话框弹出后会获得焦点，所以这里不需要处理重复点击重复显示的问题
            showBoxInfo();
            handled = true;

        } else if (keyCode == KeyCode.KEY_CODE_CHANGE_RESOLUTION) {
            if (!AppUtils.isMstar()) {
                changeResolution();
                handled = true;
            }

        } else if (keyCode == KeyCode.KEY_CODE_SHOW_PLAYLIST) {
            showPlaylist();
            handled = true;

        }
        return handled || super.onKeyDown(keyCode, event);
    }

    private void showPlaylist() {
        if (mPlayListDialog == null) {
            mPlayListDialog = new PlayListDialog(this, this);
        }
//        if (mPlayList != null) {
        if (!mPlayListDialog.isShowing()) {
            mPlayListDialog.showPlaylist(mPlayList);
        }
//        } else {
//            ShowMessage.showToast(mContext, "播放列表为空");
//        }
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
            case RESOLUTION_576p:
                msg = "576P";
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
        if (AppUtils.isMstar()) {
            Intent intent = new Intent(this, TvPlayerActivity.class);
            intent.putExtra(TvPlayerActivity.EXTRA_LAST_VID, vid);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, TvPlayerGiecActivity.class);
            intent.putExtra(TvPlayerActivity.EXTRA_LAST_VID, vid);
            startActivity(intent);
        }
    }

    @Override
    public boolean onMediaComplete(int index, boolean isLast) {
        // 这里只是为了防止到这里的时候mUUID没值，正常mUUID肯定会在onMediaPrepared()中赋值
        if (TextUtils.isEmpty(mUUID)) {
            mUUID = String.valueOf(System.currentTimeMillis());
        }
        if (mPlayList != null) {
            MediaLibBean item = mPlayList.get(index);
            if (!TextUtils.isEmpty(item.getVid())) {
                LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                        String.valueOf(System.currentTimeMillis()), "end", item.getType(), item.getVid(),
                        "", mSession.getVersionName(), mListPeriod, mSession.getVodPeriod(),
                        "");
            }

            if (ConstantValues.POLY_ADS.equals(item.getType()) && item instanceof BaiduAdLocalBean) {
                // 回调展现完成
                noticeAdsMonitor((BaiduAdLocalBean) item);

                mPlayList.get(index).setName(null);
                mPlayList.get(index).setMediaPath(null);
                mPlayList.get(index).setChinese_name("已过期");
            }
        }

        if (mNeedUpdatePlaylist) {
            // 重新获取播放列表开始播放
            LogUtils.d("更新播放列表后继续播放");
            mNeedUpdatePlaylist = false;
            if (GlobalValues.getInstance().PLAY_LIST != null && !GlobalValues.getInstance().PLAY_LIST.equals(mPlayList)) {
                int currentOrder = mPlayList.get(index).getOrder();
                mSavorVideoView.stop();
                checkAndPlay(currentOrder);
                AppUtils.deleteOldMedia(mContext);
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
        if (mNeedUpdatePlaylist) {
            LogUtils.d("更新播放列表后继续播放");
            // 重新获取播放列表开始播放
            mNeedUpdatePlaylist = false;
            if (GlobalValues.getInstance().PLAY_LIST != null && !GlobalValues.getInstance().PLAY_LIST.equals(mPlayList)) {
                int currentOrder = mPlayList.get(index).getOrder();
                mSavorVideoView.stop();
                checkAndPlay(currentOrder);
                AppUtils.deleteOldMedia(mContext);
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
    public boolean onMediaPrepared(int index) {
        if (mPlayList != null && !TextUtils.isEmpty(mPlayList.get(index).getVid())) {
            MediaLibBean libBean = mPlayList.get(index);
            if (!TextUtils.isEmpty(libBean.getEnd_date())) {
                // 检测截止时间是否已到，到达的话跳到下一个
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date endDate = null;
                try {
                    endDate = format.parse(libBean.getEnd_date());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Date now = new Date();
                if (endDate != null && endDate.before(now)) {
                    mSavorVideoView.playNext();
                    return true;
                }
            }

            String action = "";
            if (mCurrentPlayingIndex != index) {
                // 准备播放新视频时产生一个新的UUID作为日志标识
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
            LogReportUtil.get(this).sendAdsLog(mUUID, mSession.getBoiteId(), mSession.getRoomId(),
                    String.valueOf(System.currentTimeMillis()), action, libBean.getType(), libBean.getVid(),
                    "", mSession.getVersionName(), mListPeriod, mSession.getVodPeriod(), "");

            if (ConstantValues.RTB_ADS.equals(libBean.getType()) && !TextUtils.isEmpty(libBean.getAdmaster_sin())) {
                AdmasterSdk.onExpose(libBean.getAdmaster_sin());
            }
        }

        toCheckIfPolyAds(index);

        return false;
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
                        String.valueOf(System.currentTimeMillis()), "pause", mPlayList.get(index).getType(), mPlayList.get(index).getVid(),
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
                    String.valueOf(System.currentTimeMillis()), "resume", mPlayList.get(index).getType(), mPlayList.get(index).getVid(),
                    "", mSession.getVersionName(), mListPeriod, mSession.getVodPeriod(),
                    "");
        }
    }

    /**
     * 回调曝光链接
     * @param bean
     */
    private void noticeAdsMonitor(BaiduAdLocalBean bean) {
        if (bean.getWinNoticeUrlList() != null && !bean.getWinNoticeUrlList().isEmpty()) {
            for (ByteString bString :
                    bean.getWinNoticeUrlList()) {
                String url = bString.toStringUtf8();
                RetryHandler.enqueue(url, bean.getExpireTime());
            }
        }
        if (bean.getThirdMonitorUrlList() != null && !bean.getThirdMonitorUrlList().isEmpty()) {
            for (ByteString bString :
                    bean.getThirdMonitorUrlList()) {
                String url = bString.toStringUtf8();
                RetryHandler.enqueue(url, bean.getExpireTime());
            }
        }
    }

    @Override
    protected void onDestroy() {
//        LogFileUtil.write("AdsPlayerActivity onDestroy");
        super.onDestroy();
        unregisterReceiver(mDownloadCompleteReceiver);
        AdmasterSdk.terminateSDK();
    }

    @Override
    public void onSuccess(AppApi.Action method, Object obj) {
        switch (method) {
            case CP_GET_ADMASTER_CONFIG_JSON:
                if (obj instanceof AdMasterResult) {
                    adMasterResult = (AdMasterResult) obj;
                    handleAdmaster();
                }
                break;
            case SP_GET_LOADING_IMG_DOWN:
                if (obj instanceof File) {
                    File f = (File) obj;
                    byte[] fRead = new byte[0];
                    String md5Value = null;
                    try {
                        fRead = org.apache.commons.io.FileUtils.readFileToByteArray(f);
                        md5Value = AppUtils.getMD5(fRead);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //比较本地文件版本是否与服务器文件一致，如果一致则启动安装
                    if (md5Value != null && md5Value.equals(adMasterResult.getMd5())) {
                        try {
                            mContext.deleteFile("admaster_sdkconfig.xml");
                            String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.cache) + "admaster_sdkconfig.xml";
                            File tarFile = new File(path);
//                            AssetManager assetManager = this.getAssets();
//                            assetManager.

//                            FileUtils.copyFile(path, Environment.getExternalStorageDirectory().getAbsolutePath() + newPath);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case AD_BAIDU_ADS:
                if (obj instanceof TsUiApiV20171122.TsApiResponse) {
                    TsUiApiV20171122.TsApiResponse tsApiResponse = (TsUiApiV20171122.TsApiResponse) obj;
                    if (BaiduAdsResponseCode.SUCCESS == tsApiResponse.getErrorCode()) {
                        handleBaiduAdData(tsApiResponse);
                    } else {
                        LogUtils.e("百度聚屏请求错误，错误码为：" + tsApiResponse.getErrorCode());
                        LogFileUtil.write("百度聚屏请求错误，错误码为：" + tsApiResponse.getErrorCode());
                    }
                }
                break;
        }
    }

    private void handleBaiduAdData(TsUiApiV20171122.TsApiResponse tsApiResponse) {
        if (tsApiResponse != null && tsApiResponse.getAdsList() != null && !tsApiResponse.getAdsList().isEmpty()) {
            ArrayList<BaiduAdLocalBean> baiduAdList = new ArrayList<>();
            for (TsUiApiV20171122.Ad ad :
                    tsApiResponse.getAdsList()) {
                if (ad.getMaterialMetasList() != null) {
                    for (TsUiApiV20171122.MaterialMeta material :
                            ad.getMaterialMetasList()) {
                        switch (material.getMaterialType()) {
                            case VIDEO:
                                String md5 = material.getMaterialMd5().toStringUtf8();
                                LogUtils.d("百度聚屏请求，获取到物料md5：" + md5);
                                LogFileUtil.write("百度聚屏请求，获取到物料md5：" + md5);
                                if (!TextUtils.isEmpty(md5)) {
                                    String selection = DBHelper.MediaDBInfo.FieldName.TP_MD5 + "=? ";
                                    String[] selectionArgs = new String[]{md5};
                                    List<MediaLibBean> list = DBHelper.get(this).findRtbadsMediaLibByWhere(selection, selectionArgs);
                                    if (list != null && !list.isEmpty()) {
                                        BaiduAdLocalBean bean = new BaiduAdLocalBean(list.get(0));
                                        bean.setMediaRemotePath(material.getVideoUrl());
                                        bean.setWinNoticeUrlList(ad.getWinNoticeUrlList());
                                        if (ad.getThirdMonitorUrlList()!=null&&ad.getThirdMonitorUrlList().size()>0){
                                            bean.setThirdMonitorUrlList(ad.getThirdMonitorUrlList());
                                        }
                                        bean.setExpireTime(tsApiResponse.getExpirationTime());

                                        baiduAdList.add(bean);

                                        if (GlobalValues.CURRENT_ADS_REPEAT_PAIR != null) {
                                            if (md5.equals(GlobalValues.CURRENT_ADS_REPEAT_PAIR.first)) {
                                                GlobalValues.CURRENT_ADS_REPEAT_PAIR = new Pair<>(md5, GlobalValues.CURRENT_ADS_REPEAT_PAIR.second + 1);
                                            } else {
                                                GlobalValues.CURRENT_ADS_REPEAT_PAIR = new Pair<>(md5, 1);
                                            }
                                        } else {
                                            GlobalValues.CURRENT_ADS_REPEAT_PAIR = new Pair<>(md5, 1);
                                        }
                                    } else {
                                        // 返回的物料在本地没找到，记录下来物料md5，下载完之后恢复请求
                                        GlobalValues.NOT_FOUND_BAIDU_ADS_KEY = md5;
                                        LogUtils.d("百度聚屏请求，物料未在本地发现：" + md5);
                                        LogFileUtil.write("百度聚屏请求，物料未在本地发现：" + md5);
                                    }
                                }

                                break;
                            case IMAGE:
                                // TODO: 处理图片类广告
                                break;
                        }
                    }
                }
            }

            if (!baiduAdList.isEmpty() && mPlayList != null) {
                GlobalValues.ADS_PLAY_LIST = baiduAdList;
                GlobalValues.CURRENT_MEDIA_ORDER = mPlayList.get(mCurrentPlayingIndex).getOrder();
                if (AppUtils.fillPlaylist(this, null, 1)) {
                    mNeedUpdatePlaylist = true;
                }
            }
        }
    }

    private void handleAdmaster() {
        if (adMasterResult == null) {
            return;
        }
        int admaster_update_time = mSession.getAdmaster_update_time();
        if (admaster_update_time != 0 && admaster_update_time != adMasterResult.getUpdate_time()) {
            String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.cache) + "admaster_sdkconfig.xml";
            File tarFile = new File(path);
            if (tarFile.exists()) {
                tarFile.delete();
            }
            if (!TextUtils.isEmpty(adMasterResult.getFile())) {
                AppApi.downloadLoadingImg(adMasterResult.getFile(), mContext, this, path);
            }
        }

    }

    @Override
    public void onError(AppApi.Action method, Object obj) {
        switch (method) {
            case AD_BAIDU_ADS:
                LogFileUtil.write("百度聚屏请求失败");
                break;
        }
    }

    @Override
    public void onNetworkFailed(AppApi.Action method) {
        switch (method) {
            case AD_BAIDU_ADS:
                LogFileUtil.write("百度聚屏请求失败，网络异常");
                break;
        }
    }

    @Override
    public void onMediaItemSelect(int index) {
        LogUtils.d("onMediaItemSelect index is " + index);
        if (mPlayList != null && index < mPlayList.size()) {
            if (mSavorVideoView != null) {
                ArrayList<String> urls = new ArrayList<>();
                for (int i = 0; i < mPlayList.size(); i++) {
                    MediaLibBean bean = mPlayList.get(i);
                    urls.add(bean.getMediaPath());
                }

                mSavorVideoView.setMediaFiles(urls, index, 0);
            }
        }
    }
}
