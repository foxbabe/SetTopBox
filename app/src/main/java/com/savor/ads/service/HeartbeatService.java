package com.savor.ads.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.savor.ads.activity.BaseActivity;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.savor.small.netty.NettyClient;


/**
 */
public class HeartbeatService extends IntentService {
    /**
     * 心跳周期，5分钟
     */
    private static final int HEARTBEAT_DURATION = 1000 * 60 * 5;
    /**
     * 小平台信息检测周期，1分钟
     */
    private static final int SERVER_INFO_CHECK_DURATION = 1000 * 60 * 1;
    /**
     * 单次循环等待时长。
     * 由于要在关键时间点上做检测，这里须>30sec <1min
     */
    private static final int ONE_CYCLE_TIME = 1000 * 40;

    /**
     * 上一个心跳过去的时长
     */
    private int mHeartbeatElapsedTime = 0;
    /**
     * 上一个小平台信息监测过去的时长
     */
    private int mServerInfoCheckElapsedTime = 0;

    public HeartbeatService() {
        super("HeartbeatService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //  启动时立即心跳一次
        doHeartbeat();

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(mNetworkDetectionRunnable, 1, 5, TimeUnit.MINUTES);

        while (true) {

            // 小平台信息监测周期到达
            if (mServerInfoCheckElapsedTime >= SERVER_INFO_CHECK_DURATION) {
                mServerInfoCheckElapsedTime = 0;

                if (Session.get(this).getServerInfo() == null) {
                    Intent intent1 = new Intent(this, ServerDiscoveryService.class);
                    startService(intent1);

                    httpGetIp();
                }
            }

            // 心跳周期到达，向云平台发送心跳
            if (mHeartbeatElapsedTime >= HEARTBEAT_DURATION) {
                mHeartbeatElapsedTime = 0;

                doHeartbeat();
            }

            String time = AppUtils.getCurTime("HH:mm");
            // 检测时间是否到达凌晨2点整
            if ("02:00".equals(time)) {
                // 去删除存本地的投屏文件
                AppUtils.clearPptTmpFiles(this);
                AppUtils.clearAllCache(this);

                // 刷新播放列表
                Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
                if (activity instanceof BaseActivity) {
                    BaseActivity baseActivity = (BaseActivity) activity;
                    baseActivity.fillPlayList();
                    sendBroadcast(new Intent(ConstantValues.ADS_DOWNLOAD_COMPLETE_ACTION));
                }
            }

            try {
                Thread.sleep(ONE_CYCLE_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mHeartbeatElapsedTime += ONE_CYCLE_TIME;
            mServerInfoCheckElapsedTime += ONE_CYCLE_TIME;
        }
    }

    private void doHeartbeat() {
        AppApi.heartbeat(this, new ApiRequestListener() {
            @Override
            public void onSuccess(AppApi.Action method, Object obj) {

            }

            @Override
            public void onError(AppApi.Action method, Object obj) {

            }

            @Override
            public void onNetworkFailed(AppApi.Action method) {

            }
        });
    }

    private void httpGetIp() {
        LogUtils.w("HeartbeatService 将发HTTP请求去发现小平台信息");
        LogFileUtil.write("HeartbeatService 将发HTTP请求去发现小平台信息");
        AppApi.getSpIp(this, new ApiRequestListener() {
            @Override
            public void onSuccess(AppApi.Action method, Object obj) {
                LogUtils.w("HeartbeatService HTTP接口发现小平台信息");
                LogFileUtil.write("HeartbeatService HTTP接口发现小平台信息");
                if (obj instanceof ServerInfo) {
                    handleServerIp((ServerInfo) obj);
                }
            }

            @Override
            public void onError(AppApi.Action method, Object obj) {
                LogUtils.w("HeartbeatService HTTP接口发现小平台信息失败");
                LogFileUtil.write("HeartbeatService HTTP接口发现小平台信息失败");
            }

            @Override
            public void onNetworkFailed(AppApi.Action method) {
                LogUtils.w("HeartbeatService HTTP接口发现小平台信息失败");
                LogFileUtil.write("HeartbeatService HTTP接口发现小平台信息失败");
            }
        });
    }


    private void handleServerIp(ServerInfo serverInfo) {
        if (serverInfo != null && !TextUtils.isEmpty(serverInfo.getServerIp()) && serverInfo.getNettyPort() > 0 && serverInfo.getCommandPort() > 0 && serverInfo.getDownloadPort() > 0 &&
                (Session.get(this).getServerInfo() == null || Session.get(this).getServerInfo().getSource() != 1)) {
            LogUtils.w("HeartbeatService 将使用HTTP拿到的信息重置小平台信息");
            LogFileUtil.write("HeartbeatService 将使用HTTP拿到的信息重置小平台信息");
            serverInfo.setSource(2);
            if (serverInfo.getServerIp().contains("*")) {
                serverInfo.setServerIp(serverInfo.getServerIp().split("\\*")[0]);
            }
            Session.get(this).setServerInfo(serverInfo);
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

    private Runnable mNetworkDetectionRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.d("NetworkDetectionRunnable is run.");

            double internetLatency = getLatency("www.baidu.com");
            double intranetLatency = -1;
            if (Session.get(HeartbeatService.this).getServerInfo() != null) {
                intranetLatency = getLatency(Session.get(HeartbeatService.this).getServerInfo().getServerIp());
            }

            AppApi.postNetstat(HeartbeatService.this, new ApiRequestListener() {
                @Override
                public void onSuccess(AppApi.Action method, Object obj) {
                    LogUtils.d("postNetstat success");
                }

                @Override
                public void onError(AppApi.Action method, Object obj) {
                    LogUtils.d("postNetstat failed");
                }

                @Override
                public void onNetworkFailed(AppApi.Action method) {
                    LogUtils.d("postNetstat failed");
                }
            }, intranetLatency == -1 ? "" : "" + intranetLatency, internetLatency == -1 ? "" : "" + internetLatency);
        }

        private double getLatency(String address) {
            LogUtils.d("address is " + address);
            double latency = -1;

            Process process = null;
            InputStream is = null;
            BufferedReader reader = null;
            try {
                process = Runtime.getRuntime().exec("ping -c 10 -i 0.2 -s 56 " + address);
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String str = null;
                while ((str = reader.readLine()) != null) {
                    if (str.contains("rtt ")) {
                        LogUtils.d(str);
                        String speedStr = str.split(" = ")[1].split(",")[0];
                        String unitStr = speedStr.split(" ")[1];
                        double min = Double.parseDouble(speedStr.split(" ")[0].split("/")[0]);
                        double avg = Double.parseDouble(speedStr.split(" ")[0].split("/")[1]);
                        double max = Double.parseDouble(speedStr.split(" ")[0].split("/")[2]);
                        double mdev = Double.parseDouble(speedStr.split(" ")[0].split("/")[3]);

                        if ("ms".equals(unitStr)) {
                            latency = avg;
                        } else if ("s".equals(unitStr)) {
                            latency = avg * 1000;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (process != null) {
                    try {
                        process.destroy();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return latency;
        }
    };
}
