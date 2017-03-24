package com.savor.ads.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


/**
 * SSDP发送Service
 */
public class SSDPMulticastService extends IntentService {
    /**
     * SSDP发送周期，2分钟
     */
    private static final int MULTICAST_DURATION = 1000 * 60 * 2;

    private static final int PORT = 11900;
    private static final String IP_TARGET = "238.255.255.250";

    private static final String TYPE_LABEL_PREFIX = "Savor-Type:";
    private static final String IP_LABEL_PREFIX = "Savor-HOST:";
    private static final String CRLF = "\r\n";

    private MulticastSocket mSocketSend;

    public SSDPMulticastService() {
        super("SSDPMulticastService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        while (true) {
            LogUtils.d("开始发送一次SSDP");
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            final WifiManager.MulticastLock multicastLock = wm.createMulticastLock("sendLock");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();

            try {
                mSocketSend = new MulticastSocket();
//                mSocketSend.setLoopbackMode(true);
//                mSocketSend.setTimeToLive(0);
                mSocketSend.joinGroup(InetAddress.getByName(IP_TARGET));

                String msg = TYPE_LABEL_PREFIX + "box" + CRLF +
                        IP_LABEL_PREFIX + AppUtils.getLocalIPAddress() + CRLF;
                DatagramPacket packetSend = new DatagramPacket(msg.getBytes(), msg.length(), InetAddress.getByName(IP_TARGET), PORT);
                mSocketSend.send(packetSend);
            } catch (IOException e) {
                e.printStackTrace();
                LogUtils.e("SSDP发送异常：" + e.getMessage());
            }

            multicastLock.release();
            if (mSocketSend != null && !mSocketSend.isClosed()) {
                mSocketSend.close();
            }
            LogUtils.d("完成发送一次SSDP");

            try {
                Thread.sleep(MULTICAST_DURATION);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("onDestroy");

    }
}
