package com.savor.ads.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.ImageView;

import com.savor.ads.R;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.database.DBHelper;
import com.savor.ads.log.LogProduceService;
import com.savor.ads.log.LogUploadService;
import com.savor.ads.service.HandleMediaDataService;
import com.savor.ads.service.HeartbeatService;
import com.savor.ads.service.MessageService;
import com.savor.ads.service.MiniProgramNettyService;
import com.savor.ads.service.SSDPMulticastService;
import com.savor.ads.service.ServerDiscoveryService;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlideImageLoader;
import com.savor.ads.utils.KeyCode;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.TimeCalibrateHelper;

import java.util.ArrayList;

import cn.savor.small.netty.NettyClient;

/**
 * Created by Administrator on 2016/12/6.
 */

public class MainActivity extends BaseActivity {
    Handler mHandler = new Handler();
    private ImageView main_imgIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDisplay();

        mSession.setStartTime(AppUtils.getCurTime());

        // 清楚Glide图片缓存
        GlideImageLoader.clearCache(mContext, true, true);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gotoAdsActivity();
            }
        }, 5000);
    }

    private void gotoAdsActivity() {
        ArrayList<MediaLibBean> tempList = DBHelper.get(this).getTempProList();
        if (tempList != null && tempList.size() > 30) {
            String selection = DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=? AND " +
                    DBHelper.MediaDBInfo.FieldName.PERIOD + "!=? AND " +
                    DBHelper.MediaDBInfo.FieldName.PERIOD + "!=?";
            String[] args = new String[]{ConstantValues.PRO, mSession.getProPeriod(), mSession.getProDownloadPeriod()};
            DBHelper.get(this).deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, selection, args);
        }

        fillPlayList();

        AppUtils.deleteOldMedia(mContext);

        Intent intent = new Intent(mContext, AdsPlayerActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // 启动心跳服务
        startHeartbeatService();

        if (mSession.getServerInfo() != null) {
//            startNettyService();
            AppApi.resetSmallPlatformInterface(this);

            // source=3表示是在设置界面手动设置的
            if (mSession.getServerInfo().getSource() != 3) {
                //  启动service以发现小平台
                startSsdpService();
                // 去云平台获取小平台地址
                getSpIpFromServer();
            }
        } else {
            //  启动service以发现小平台
            startSsdpService();
            // 去云平台获取小平台地址
            getSpIpFromServer();
        }

        startDownloadMediaDataService();

        startProduceLogService();
        startUploadLogService();

        startMulticastSendService();
    }

    /**
     * 去云平台获取小平台地址
     */
    private void getSpIpFromServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!AppUtils.isNetworkAvailable(mContext)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LogUtils.w("将发HTTP请求去发现小平台信息");
                LogFileUtil.write("MainActivity 将发HTTP请求去发现小平台信息");
                AppApi.getSpIp(mContext, new ApiRequestListener() {
                    @Override
                    public void onSuccess(AppApi.Action method, Object obj) {
                        if (obj instanceof ServerInfo) {
                            LogUtils.w("HTTP接口发现小平台信息");
                            LogFileUtil.write("MainActivity HTTP接口发现小平台信息");
                            handleServerIp((ServerInfo) obj);
                        }
                    }

                    @Override
                    public void onError(AppApi.Action method, Object obj) {
                        LogUtils.w("HTTP接口发现小平台信息失败");
                        LogFileUtil.write("MainActivity HTTP接口发现小平台信息失败");
                    }

                    @Override
                    public void onNetworkFailed(AppApi.Action method) {
                        LogUtils.w("HTTP接口发现小平台信息失败");
                        LogFileUtil.write("MainActivity HTTP接口发现小平台信息失败");
                    }
                });
            }
        }).start();
    }

    private void handleServerIp(ServerInfo serverInfo) {
        if (serverInfo != null && !TextUtils.isEmpty(serverInfo.getServerIp()) && serverInfo.getNettyPort() > 0 && serverInfo.getCommandPort() > 0 && serverInfo.getDownloadPort() > 0 &&
                (mSession.getServerInfo() == null || mSession.getServerInfo().getSource() != 1)) {
            LogUtils.w("将使用HTTP拿到的信息重置小平台信息");
            LogFileUtil.write("MainActivity 将使用HTTP拿到的信息重置小平台信息");
            serverInfo.setSource(2);
            if (serverInfo.getServerIp().contains("*")) {
                serverInfo.setServerIp(serverInfo.getServerIp().split("\\*")[0]);
            }
            mSession.setServerInfo(serverInfo);
            AppApi.resetSmallPlatformInterface(this);

            // 重设NettyClient ip、端口号
            // NettyClient.get() != null意味着在MainActivity已经初始化Netty并开始连接
            if (NettyClient.get() != null) {
                NettyClient.get().setServer(serverInfo.getNettyPort(), serverInfo.getServerIp());
            } else {
                Intent intent = new Intent(this, MessageService.class);
                startService(intent);
            }
        }
    }

    private void startHeartbeatService() {
        LogFileUtil.write("MainActivity will startHeartbeatService");
        Intent intent = new Intent(this, HeartbeatService.class);
        startService(intent);
    }

    private void startSsdpService() {
        LogFileUtil.write("MainActivity will startSsdpService");
        Intent intent = new Intent(this, ServerDiscoveryService.class);
        startService(intent);
    }

    private void startMulticastSendService() {
        LogFileUtil.write("MainActivity will startMulticatSendService");
        Intent intent = new Intent(this, SSDPMulticastService.class);
        startService(intent);
    }

    private void startNettyService() {
        LogFileUtil.write("MainActivity will startNettyService");
        Intent intent = new Intent(this, MessageService.class);
        startService(intent);
    }




    /**
     * 启动下载媒体文件服务
     */
    private void startDownloadMediaDataService() {
        LogUtils.v("========start download media service======");
        LogFileUtil.write("MainActivity will startDownloadMediaDataService");
        Intent intent = new Intent(this, HandleMediaDataService.class);
        startService(intent);
    }

    /**
     * 启动生产日志线程
     */
    private void startProduceLogService() {
        LogFileUtil.write("MainActivity will start LogProduceService");
        LogProduceService logProduceService = new LogProduceService(mContext);
        logProduceService.run();
    }

    /**
     * 启动上传log
     */
    private void startUploadLogService() {
        LogFileUtil.write("MainActivity will start LogUploadService");
        LogUploadService logUploadService = new LogUploadService(mContext);
        logUploadService.start();
    }

    void initDisplay() {
        main_imgIv = (ImageView) findViewById(R.id.main_img);
//        GlideImageLoader.loadImage(this,
//                Environment.getExternalStorageDirectory().getAbsolutePath() + mSession.getSplashPath(),
//                main_imgIv, 0, R.mipmap.bg_splash);
        Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getAbsolutePath() + mSession.getSplashPath());
        if (bitmap != null) {
            main_imgIv.setImageBitmap(bitmap);
        } else {
            main_imgIv.setImageResource(R.mipmap.bg_splash);
        }

        if (mAudioManager != null) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 30, 0);
        }
        TimeCalibrateHelper timeCalibrateHelper = new TimeCalibrateHelper();
        timeCalibrateHelper.startCalibrateTime();

    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyCode.KEY_CODE_SHOW_INFO) {
            showBoxInfo();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
