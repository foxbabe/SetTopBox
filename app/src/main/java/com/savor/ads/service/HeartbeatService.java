package com.savor.ads.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;

import com.savor.ads.bean.ServerInfo;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.TechnicalLogReporter;

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

            // 检测时间是否到达凌晨2点整，去删除存本地的投屏文件
            String time = AppUtils.getCurTime("hh:MM");
            if ("02:00".equals(time)) {
                AppUtils.clearAllCache(this);
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
}
