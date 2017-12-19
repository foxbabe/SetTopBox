package com.savor.ads.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.mstar.tv.service.skin.AudioSkin;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.ResponseErrorMessage;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.dialog.BoxInfoDialog;
import com.savor.ads.dialog.FileCopyDialog;
import com.savor.ads.dialog.InputBoiteIdDialog;
import com.savor.ads.dialog.UsbUpdateDialog;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCode;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.ShowMessage;
import com.savor.ads.utils.TechnicalLogReporter;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by zhanghq on 2016/6/23.
 */
public abstract class BaseActivity extends Activity implements InputBoiteIdDialog.Callback {

    protected Activity mContext;
    protected Session mSession;
    public AudioManager mAudioManager = null;

    private BoxInfoDialog mBoxInfoDialog;
    private UsbUpdateDialog mUsbUpdateDialog;
    private InputBoiteIdDialog mInputBoiteIdDialog;
    private FileCopyDialog mFileCopyDialog;

    private Handler mHandler = new Handler();

    protected boolean mIsGoneToSystemSetting;
    private AudioSkin mAudioSkin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        ActivitiesManager.getInstance().pushActivity(this);
        mSession = Session.get(mContext);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        LogFileUtil.write("BaseActivity onCreate this is " + this.toString());

        mAudioSkin = new AudioSkin(this);
        mAudioSkin.connect(null);
    }


    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        registerListener();

    }

    /**
     * 注册广播，判断sd卡以及U盘挂载状态
     */
    public void registerListener() {
        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
//        intentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
//        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
//        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
//        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
//        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
//        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
//        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addDataScheme("file");

        registerReceiver(recevierListener, intentFilter);
    }

    private BroadcastReceiver recevierListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.d("BroadcastReceiver: " + intent.getAction());
            if (intent.getAction() == null) {
                return;
            }

            String path = intent.getData().getPath();
            String usbPath = intent.getDataString().split("file://")[1];
            switch (intent.getAction()) {
                case Intent.ACTION_MEDIA_MOUNTED:
                    if (AppUtils.isMstar()) {
                        if (path.contains("/mnt/extsd")) {
                            handleExtsdMounted();
                        } else if (path.contains("usb")) {
                            mSession.setUsbPath(usbPath);
                            handleUdiskMounted(path);
                        }
                    } else {
                        if (path.contains("storage/") && path.contains("-")) {
                            mSession.setUsbPath(usbPath);
                            handleUdiskMounted(path);
                        }
                    }
                    break;
                case Intent.ACTION_MEDIA_UNMOUNTED:
                case Intent.ACTION_MEDIA_REMOVED:
                case Intent.ACTION_MEDIA_BAD_REMOVAL:
                    if (AppUtils.isMstar()) {
                        if (path.contains("/mnt/extsd")) {//sd 卡拔除
                            handleExtsdRemoved();
                        } else if (path.contains("usb")) {
                            //U盘拔出
                            mSession.setUsbPath(null);
                            handleUdiskRemoved(path);
                        }
                    } else {
                        if (path.contains("storage/") && path.contains("-")) {
                            mSession.setUsbPath(null);
                            handleUdiskRemoved(path);
                        }
                    }
                    break;
            }
        }
    };

    private void handleExtsdMounted() {
        TechnicalLogReporter.sdcardMounted(this);
        checkAndClearCache();

//        fillPlayList();

//        if (GlobalValues.PLAY_LIST != null && !GlobalValues.PLAY_LIST.isEmpty()) {
//            Intent intent = new Intent(this, AdsPlayerActivity.class);
//            startActivity(intent);
//        }
    }

    private void checkAndClearCache() {
        AppUtils.clearAllCache(this);

        String lastStartStr = mSession.getLastStartTime();
        String curTimeStr = AppUtils.getCurTime(AppUtils.DATEFORMAT_YYMMDD);
        String dateStr = null;
        if (!TextUtils.isEmpty(lastStartStr) && lastStartStr.contains(" ")) {
            dateStr = lastStartStr.split(" ")[0];
        }
        LogFileUtil.write("checkAndClearCache curTimeStr=" + curTimeStr + " lastDateStr=" + dateStr);
        if (!curTimeStr.equals(dateStr)) {
            AppUtils.clearPptTmpFiles(this);
        }
    }

    public void fillPlayList() {
        LogUtils.d("开始fillPlayList");
        if (!TextUtils.isEmpty(AppUtils.getMainMediaPath())) {
            DBHelper dbHelper = DBHelper.get(mContext);
            ArrayList<PlayListBean> playList = dbHelper.getOrderedPlayList();

            if (playList != null && !playList.isEmpty()) {
                for (int i = 0; i < playList.size(); i++) {
                    PlayListBean bean = playList.get(i);

                    // 特殊处理ads数据
                    if (bean.getMedia_type().equals(ConstantValues.ADS)) {
                        String selection = DBHelper.MediaDBInfo.FieldName.LOCATION_ID
                                + "=? ";
                        String[] selectionArgs = new String[]{bean.getLocation_id()};
                        List<PlayListBean> list = dbHelper.findAdsByWhere(selection, selectionArgs);
                        if (list != null && !list.isEmpty()) {
                            for (PlayListBean item :
                                    list) {
                                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                Date startDate = null;
                                Date endDate = null;
                                try {
                                    startDate = format.parse(item.getStart_date());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    endDate = format.parse(item.getEnd_date());
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                Date now = new Date();
                                if (startDate != null && endDate != null &&
                                        now.after(startDate) && now.before(endDate)) {
                                    bean.setVid(item.getVid());
                                    bean.setDuration(item.getDuration());
                                    bean.setMd5(item.getMd5());
                                    bean.setMedia_name(item.getMedia_name());
                                    bean.setMediaPath(item.getMediaPath());
                                    break;
                                }
                            }
                        }
                    }

                    File mediaFile = new File(bean.getMediaPath());
                    boolean fileCheck = false;
                    if (!TextUtils.isEmpty(bean.getMd5()) &&
                            !TextUtils.isEmpty(bean.getMediaPath()) &&
                            mediaFile.exists()) {
                        if (!bean.getMd5().equals(AppUtils.getEasyMd5(mediaFile))) {
                            fileCheck = true;

                            TechnicalLogReporter.md5Failed(this, bean.getVid());
                        }
                    } else {
                        fileCheck = true;
                    }

                    if (fileCheck) {
                        if (!TextUtils.isEmpty(bean.getVid())) {
                            LogUtils.e("媒体文件校验失败! vid:" + bean.getVid());
                        }
                        // 校验失败时将文件路径置空，下面会删除掉为空的项
                        bean.setMediaPath(null);
                        if (mediaFile.exists()) {
                            mediaFile.delete();
                        }

                        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,
                                DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                        DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                        DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                                new String[]{bean.getPeriod(), bean.getVid(), bean.getMedia_type()});
                        dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.PLAYLIST,
                                DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                        DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                        DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                                new String[]{bean.getPeriod(), bean.getVid(), bean.getMedia_type()});
                    }
                }

//                dbHelper.close();
            }

            if (playList != null && !playList.isEmpty()) {
                ArrayList<PlayListBean> list = new ArrayList<>();
                for (PlayListBean bean : playList) {
                    if (!TextUtils.isEmpty(bean.getMediaPath())) {
                        list.add(bean);
                    }
                }
                GlobalValues.PLAY_LIST = list;
            } else {
                File mediaDir = new File(AppUtils.getFilePath(this, AppUtils.StorageFile.media));
                if (mediaDir.exists() && mediaDir.isDirectory()) {
                    File[] files = mediaDir.listFiles();
                    ArrayList<PlayListBean> filePlayList = new ArrayList<>();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                PlayListBean bean = new PlayListBean();
                                bean.setMediaPath(file.getPath());
                                filePlayList.add(bean);
                            }
                        }
                    }
                    GlobalValues.PLAY_LIST = filePlayList;
                }
            }
        } else {
            LogFileUtil.writeKeyLogInfo("跳转轮播，未找到SD卡！");
            ShowMessage.showToast(mContext, "未发现SD卡");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        if (recevierListener != null) {
            unregisterReceiver(recevierListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivitiesManager.getInstance().popActivity(this);

        if (mBoxInfoDialog != null && mBoxInfoDialog.isShowing()) {
            mBoxInfoDialog.dismiss();
        }
        if (mAudioSkin != null) {
            mAudioSkin.disconnect();
            mAudioSkin = null;
        }
    }


    private Runnable mHideInfoRunnable = new Runnable() {
        @Override
        public void run() {
            if (mBoxInfoDialog != null && mBoxInfoDialog.isShowing()) {
                mBoxInfoDialog.dismiss();
            }
        }
    };

    /**
     * 显示盒子信息
     */
    protected void showBoxInfo() {
        if (mBoxInfoDialog == null) {
            mBoxInfoDialog = new BoxInfoDialog(this);
            mBoxInfoDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    mHandler.removeCallbacks(mHideInfoRunnable);
                }
            });
        }
        if (!mBoxInfoDialog.isShowing()) {
            mBoxInfoDialog.show();
        }
        mHandler.postDelayed(mHideInfoRunnable, 10 * 1000);
    }

    /**
     * 插入USB以后，读取USB中的内容,目前仅限图片
     *
     * @param pathString
     */
    private void handleUdiskMounted(String pathString) {
        ShowMessage.showToast(mContext, "U盘已插入，正在读取...");
        String imagesPath = pathString + File.separator + ConstantValues.USB_FILE_PATH;
        File file = new File(imagesPath);
        if (!file.exists()) {
            ShowMessage.showToast(mContext, "未在U盘中检测到【redian】文件夹，请检查后重试！");
            return;
        }
        File[] files = file.listFiles();
        //空文件夹的情况
        if (files.length == 0) {
            ShowMessage.showToast(mContext, "检测到【redian】文件夹是空的，请检查后重试！");
            return;
        }
        ArrayList<String> usbImgPathList = getImagePath(files);//获取所有图片路径
        if (usbImgPathList.size() <= 0) {
            ShowMessage.showToast(mContext, "检测到【redian】文件夹中没有图片文件，请检查后重试！");
            return;
        }

        if (!(this instanceof MainActivity)) {
            if (this instanceof UsbImageViewerActivity) {
                ((UsbImageViewerActivity) this).resetImages(usbImgPathList, imagesPath);
            } else {
                Intent intent = new Intent(this, UsbImageViewerActivity.class);
                intent.putExtra(UsbImageViewerActivity.EXTRA_URLS, usbImgPathList);
                intent.putExtra(UsbImageViewerActivity.EXTRA_USB_PATH, imagesPath);
                startActivity(intent);
            }
        }
    }

    //获取所有图片路径
    private ArrayList<String> getImagePath(File[] files) {

        ArrayList<String> usbImgPathList = new ArrayList<>();
        for (File file : files) {
            if (AppUtils.checkIsImageFile(file.getPath())) {
                usbImgPathList.add(file.getPath());
            }
        }
        return usbImgPathList;
    }

    /**
     * USB拔出
     */
    private void handleUdiskRemoved(String pathString) {
        ShowMessage.showToast(mContext, "U盘已拔出");
        String imagesPath = pathString + File.separator + ConstantValues.USB_FILE_PATH;
        if (this instanceof UsbImageViewerActivity && imagesPath.equals(((UsbImageViewerActivity) this).getCurrentUsbPath())) {
            finish();
        }
    }


    private void handleExtsdRemoved() {
        TechnicalLogReporter.sdcardRemoved(this);
        //SD移除时跳到TV页
        if (this instanceof AdsPlayerActivity) {
            if (AppUtils.isMstar()) {
                Intent intent = new Intent(this, TvPlayerActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, TvPlayerGiecActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (keyCode == KeyCode.KEY_CODE_SYSTEM_SETTING) {
            LogFileUtil.write("will gotoSystemSetting");
            gotoSystemSetting();
            handled = true;

        } else if (keyCode == KeyCode.KEY_CODE_SETTING) {
            gotoSetting();
            handled = true;

        } else if (keyCode == KeyCode.KEY_CODE_MANUAL_HEARTBEAT) {
            manualHeartbeat();
            handled = true;

        } else if (keyCode == KeyCode.KEY_CODE_SHOW_APP_INSTALLED) {
            gotoAppBrowser();
            handled = true;

        } else if (keyCode == KeyCode.KEY_CODE_UDISK_UPDATE) {
            handleUsbUpdate();
            handled = true;
        } else if (keyCode == KeyCode.KEY_CODE_UDISK_COPY) {
            handleUsbCopy();
            handled = true;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    private void gotoSystemSetting() {
        mIsGoneToSystemSetting = true;
        Intent intent = new Intent("android.settings.SETTINGS");
        startActivity(intent);
    }

    private void gotoSetting() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    private void gotoAppBrowser() {
        Intent intent = new Intent(this, AppBrowserActivity.class);
        startActivity(intent);
    }

    private void handleUsbUpdate() {
        if (checkAndSetUsbPath()) {
            if (!TextUtils.isEmpty(mSession.getBoiteId())) {
                if (mUsbUpdateDialog == null) {
                    mUsbUpdateDialog = new UsbUpdateDialog(this);
                }
                if (!mUsbUpdateDialog.isShowing()) {
                    mUsbUpdateDialog.show();
                }
            } else {
                if (mInputBoiteIdDialog == null) {
                    mInputBoiteIdDialog = new InputBoiteIdDialog(this, this);
                }

                if (!mInputBoiteIdDialog.isShowing()) {
                    mInputBoiteIdDialog.show();
                }
            }
        } else {
            ShowMessage.showToast(this, "未发现可执行U盘目录");
        }
    }

    private void handleUsbCopy() {
        if (checkAndSetUsbPath()) {
            File mediaFile = new File(mSession.getUsbPath() + "media/");
            File multicastFile = new File(mSession.getUsbPath() + "multicast/");
            if (!mediaFile.exists() && !multicastFile.exists()) {
                ShowMessage.showToast(this, "未发现可执行U盘目录");
            } else {
                if (mFileCopyDialog == null) {
                    mFileCopyDialog = new FileCopyDialog(this);
                }
                if (!mFileCopyDialog.isShowing()) {
                    mFileCopyDialog.show();
                }
            }
        } else {
            ShowMessage.showToast(this, "未发现可执行U盘目录");
        }
    }

    private boolean checkAndSetUsbPath() {
        boolean hasEligibleUdisk = false;
        if (AppUtils.isMstar()) {

        } else {
            String[] possiblePaths = new String[]{"/storage/udisk0/", "/storage/udisk1/", "/storage/udisk2/"};
            for (String path : possiblePaths) {
                if (new File(path + ConstantValues.USB_FILE_HOTEL_PATH).exists() &&
                        new File(path + ConstantValues.USB_FILE_HOTEL_PATH + File.separator +
                                mSession.getBoiteId() + File.separator +
                                ConstantValues.USB_FILE_HOTEL_UPDATE_CFG).exists()) {
                    mSession.setUsbPath(path);
                    hasEligibleUdisk = true;
                    break;
                }
            }
        }
        return hasEligibleUdisk;
    }

    private void manualHeartbeat() {
        ShowMessage.showToast(this, "开始上报心跳");
        AppApi.heartbeat(this, new ApiRequestListener() {
            @Override
            public void onSuccess(AppApi.Action method, Object obj) {
                ShowMessage.showToast(mContext, "上报心跳成功");
            }

            @Override
            public void onError(AppApi.Action method, Object obj) {
                String msg = "";
                if (obj instanceof ResponseErrorMessage) {
                    ResponseErrorMessage errorMessage = (ResponseErrorMessage) obj;
                    msg = errorMessage.getMessage();
                }
                ShowMessage.showToast(mContext, "上报心跳失败 " + msg);
            }

            @Override
            public void onNetworkFailed(AppApi.Action method) {
                ShowMessage.showToast(mContext, "上报心跳失败，网络异常");
            }
        });
    }

    protected void setVolume(int volume) {
        if (mAudioSkin != null) {
            if (volume > 100)
                volume = 100;
            else if (volume < 0)
                volume = 0;
            mAudioSkin.setVolume(volume);
        }
    }

    protected int getVolume() {
        if (mAudioSkin != null) {
            return mAudioSkin.getVolume();
        }
        return -1;
    }

    @Override
    public void onBoiteIdCheckPass() {
        handleUsbUpdate();
    }
}
