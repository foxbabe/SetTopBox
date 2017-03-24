package com.savor.ads.core;

/*
 * Copyright (C) 2010 mAPPn.Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.savor.ads.bean.ServerInfo;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.SaveFileData;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.TimeZone;

import static com.savor.ads.core.Session.P_APP_VOLUME;

/**
 * @author Administrator
 */
@SuppressLint("WorldReadableFiles")
public class Session {
    private final static String TAG = "Session";
    private Context mContext;
    private SaveFileData mPreference;
    private static Session mInstance;


    private final int osVersion;
    private final String buildVersion;
    private final String model;
    private final String romVersion;
    private String versionName;
    private int versionCode;
    private String debugType;
    private boolean isDebug;
    private String appName;
    private String macAddress;
    private String token;

    private ServerInfo serverInfo;
    //音量
    private int volume;
    //酒楼名称
    private String boiteName;
    //酒楼ID
    private String boiteId;
    //包间名称
    private String roomName;
    //包间ID
    private String roomId;
    private String boxId;
    /** 盒子名*/
    private String boxName;
    /** 包间类型*/
    private String roomType;
    /**广告期号*/
    private String advertMediaPeriod;
    /**下一期要播放的广告的期号*/
    private String nextAdvertMediaPeriod;
    /**下一期要播放的广告时间*/
    private String nextAdvertMediaPubTime;
    //广告下载中期号
    private String advertDownloadingPeriod;
    /** 启动图版本*/
    private String splashVersion;
    //当从电视切换到广告播放以后，最后电视停留的频道号
    private String TVLastChannel;
    //log版本
    private String logVersionCode;
    //点播视频期号
    private String multicastMediaPeriod;
    //点播视频用到的期号
    private String multicastDownloadingPeriod;
    //开机时间
    private String startTime;
    private String lastStartTime;
    //如果当前播放的是电视节目，多长时间切换到广告
    private int switchTime;

    /**
     * 电视当前频道
     */
    private int mTvCurrentChannelNumber;
    /**
     * 电视默认频道
     */
    private int mTvDefaultChannelNumber;
    /**
     * 电视当前输入源
     */
    private int mTvInputSource;
    /**
     * 以太网卡MAC地址
     */
    private String mEthernetMac;
    /**
     * 无线网卡MAC地址
     */
    private String mWlanMac;
    /**
     * oss上传桶名称
     */
    private String oss_bucket;
    /**
     * oss上传路径
     */
    private String oss_file_path;

    private boolean mIsConnectedToSP;
    /** 呼玛验证码*/
    private String mAuthCode;

    private Session(Context context) {

        mContext = context;
        mPreference = new SaveFileData(context, "savor");
        osVersion = Build.VERSION.SDK_INT;
        buildVersion = Build.VERSION.RELEASE;
        model = Build.MODEL;
        romVersion = Build.VERSION.INCREMENTAL;
        try {
//            AppUtils.clearExpiredFile(context, false);
//            AppUtils.clearExpiredCacheFile(context);
            readSettings();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Session get(Context context) {

        if (mInstance == null) {
            mInstance = new Session(context);
        }
        return mInstance;
    }

    private void readSettings() {

        getApplicationInfo();
        volume = mPreference.loadIntKey(P_APP_VOLUME, 50);
        boiteName = mPreference.loadStringKey(P_APP_BOITENAME, null);
        boiteId = mPreference.loadStringKey(P_APP_BOITEID, null);
        roomName = mPreference.loadStringKey(P_APP_ROOMNAME, null);
        roomId = mPreference.loadStringKey(P_APP_ROOMID, null);
        boxName = mPreference.loadStringKey(P_APP_BOXNAME, null);
        roomType = mPreference.loadStringKey(P_APP_ROOM_TYPE, null);
        advertMediaPeriod = mPreference.loadStringKey(P_APP_ADVERTMEDIAPERIOD, "");
        nextAdvertMediaPeriod = mPreference.loadStringKey(P_APP_NEXT_ADVERTMEDIAPERIOD, "");
        nextAdvertMediaPubTime = mPreference.loadStringKey(P_APP_NEXT_ADVERTMEDIA_PUBTIME, null);
        advertDownloadingPeriod = mPreference.loadStringKey(P_APP_ADVERTDOWNLOADINGPERIOD, null);
        TVLastChannel = mPreference.loadStringKey(P_APP_TVLASTCHANNEL, null);
        logVersionCode = mPreference.loadStringKey(P_APP_LOGVERSIONCODE, null);
        multicastMediaPeriod = mPreference.loadStringKey(P_APP_MULTICASTMEDIAPERIOD, "");
        multicastDownloadingPeriod = mPreference.loadStringKey(P_APP_MULTICASTDOWNLOADINGPERIOD, null);
        startTime = mPreference.loadStringKey(P_APP_STARTTIME, null);
        lastStartTime = mPreference.loadStringKey(P_APP_LASTSTARTTIME, null);
        switchTime = mPreference.loadIntKey(P_APP_SWITCHTIME, 30);

        mTvInputSource = mPreference.loadIntKey(P_APP_TV_CURRENT_INPUT, 0);
        mTvCurrentChannelNumber = mTvDefaultChannelNumber = mPreference.loadIntKey(P_APP_TV_DEFAULT_CHANNEL, 0);
        serverInfo = (ServerInfo) StringToObject(mPreference.loadStringKey(P_APP_SERVER_INFO, null));
        mEthernetMac = mPreference.loadStringKey(P_APP_ETHERNET_MAC, null);
        mWlanMac = mPreference.loadStringKey(P_APP_WLAN_MAC, null);
        oss_bucket = mPreference.loadStringKey(P_APP_OSS_BUCKET,null);
        oss_file_path = mPreference.loadStringKey(P_APP_OSS_PATH,null);
        splashVersion = mPreference.loadStringKey(P_APP_SPLASH_VERSION,null);
        mAuthCode = mPreference.loadStringKey(P_APP_AUTH_CODE,null);
        /** 清理App缓存 */
        AppUtils.clearExpiredFile(mContext, false);
    }

    /*
     * 读取App配置信息
     */
    private void getApplicationInfo() {

        final PackageManager pm = mContext.getPackageManager();
        try {
            final PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(),
                    0);
            versionName = pi.versionName;
            versionCode = pi.versionCode;

            final ApplicationInfo ai = pm.getApplicationInfo(
                    mContext.getPackageName(), PackageManager.GET_META_DATA);
            debugType = "1";// ai.metaData.get("app_debug").toString();

            if ("1".equals(debugType)) {
                // developer mode
                isDebug = true;
            } else if ("0".equals(debugType)) {
                // release mode
                isDebug = false;
            }
            LogUtils.allow = isDebug;

            appName = String.valueOf(ai.loadLabel(pm));
            LogUtils.appTagPrefix = appName;

        } catch (NameNotFoundException e) {
            LogUtils.d("met some error when get application info");
        }
    }


    private void writePreference(Pair<String, Object> updateItem) {
        //
        // // the preference key
        final String key = (String) updateItem.first;

        //根据不同的key确定不同的存储方式。
        if (P_APP_BOITENAME.equals(key)
                || P_APP_BOITEID.equals(key)
                || P_APP_ROOMNAME.equals(key)
                || P_APP_ROOMID.equals(key)
                || P_APP_BOXNAME.equals(key)
                || P_APP_ROOM_TYPE.equals(key)
                || P_APP_ADVERTMEDIAPERIOD.equals(key)
                || P_APP_NEXT_ADVERTMEDIAPERIOD.equals(key)
                || P_APP_NEXT_ADVERTMEDIA_PUBTIME.equals(key)
                || P_APP_ADVERTDOWNLOADINGPERIOD.equals(key)
                || P_APP_TVLASTCHANNEL.equals(key)
                || P_APP_LOGVERSIONCODE.equals(key)
                || P_APP_MULTICASTMEDIAPERIOD.equals(key)
                || P_APP_MULTICASTDOWNLOADINGPERIOD.equals(key)
                || P_APP_STARTTIME.equals(key)
                || P_APP_LASTSTARTTIME.equals(key)
                || P_APP_ETHERNET_MAC.equals(key)
                || P_APP_WLAN_MAC.equals(key)
                || P_APP_OSS_PATH.equals(key)
                || P_APP_OSS_BUCKET.equals(key)
                || P_APP_AUTH_CODE.equals(key)) {
            mPreference.saveStringKey(key, (String) updateItem.second);
        } else if (P_APP_VOLUME.equals(key) ||
                P_APP_TV_DEFAULT_CHANNEL.equals(key) ||
                P_APP_TV_CURRENT_INPUT.equals(key) ||
                P_APP_SWITCHTIME.equals(key)) {
            mPreference.saveIntKey(key, (int) updateItem.second);
        } else {
            String string = ObjectToString(updateItem.second);
            mPreference.saveStringKey(key, string);
        }
    }

    private Object getObj(String key) {
        String string = mPreference.loadStringKey(key, "");
        Object object = null;
        if (!TextUtils.isEmpty(string)) {
            try {
                object = StringToObject(string);
            } catch (Exception ex) {
                LogUtils.e("wang" + "异常" + ex.toString());
            }
        }
        return object;
    }

    private void setObj(String key, Object obj) {
        try {
            writePreference(new Pair<String, Object>(key, obj));
        } catch (Exception ex) {
            LogUtils.e("wang" + ex.toString());
        }
    }

    private String ObjectToString(Object obj) {
        String productBase64 = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            productBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            LogUtils.e("错误" + "保存错误" + e.toString());
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return productBase64;
    }

    private Object StringToObject(String str) {
        Object obj = null;
        byte[] base64Bytes;
        ByteArrayInputStream bais = null;
        try {
            String productBase64 = str;
            if (null == productBase64
                    || TextUtils.isEmpty(productBase64.trim())) {
                return null;
            }

            base64Bytes = Base64.decode(productBase64, Base64.DEFAULT);
            bais = new ByteArrayInputStream(base64Bytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            obj = ois.readObject();
            ois.close();
        } catch (Exception e) {
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return obj;
    }


    public String getVersionName() {
        if (TextUtils.isEmpty(versionName)) {
            getApplicationInfo();
        }
        return versionName;
    }

    public int getVersionCode() {
        if (versionCode <= 0) {
            getApplicationInfo();
        }
        return versionCode;
    }


    public String getMacAddr() {
        if (TextUtils.isEmpty(macAddress)) {
            try {
                WifiManager wifi = (WifiManager) mContext
                        .getSystemService(Context.WIFI_SERVICE);
                WifiInfo info = wifi.getConnectionInfo();
                macAddress = info.getMacAddress();
            } catch (Exception ex) {
                LogUtils.e(ex.toString());
            }
        }
        return macAddress;
    }

    /**
     * boxId当前指的就是机顶盒有线的Mac地址
     */
    public static String getWiredMacAddr() {
        String cmd = "busybox ifconfig eth0";
        Process process = null;
        InputStream is = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            is = process.getInputStream();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is));
            String line = reader.readLine();
            return line.substring(line.indexOf("HWaddr") + 6).trim()
                    .replaceAll(":", "");
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }


    /**
     * 返回设备相关信息
     */
    public String getDeviceInfo() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("versionname=");
        buffer.append(versionName);
        buffer.append(";versioncode=");
        buffer.append(versionName);
        buffer.append(";macaddress=");
        buffer.append(getMacAddr());
        buffer.append(";buildversion=");
        buffer.append(buildVersion);
        buffer.append(versionCode);

        TimeZone timeZone = TimeZone.getDefault();
        buffer.append(";systemtimezone=");
        buffer.append(timeZone.getID());

        return buffer.toString();
    }

    // 获取应用名字
    public String getAppName() {
        return appName;
    }

    public int getOsVersion() {
        return osVersion;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public String getModel() {
        return model;
    }

    public String getRomVersion() {
        return romVersion;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getDebugType() {
        return debugType;
    }

    public void setDebugType(String debugType) {
        this.debugType = debugType;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void setDebug(boolean debug) {
        isDebug = debug;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        synchronized (Session.class) {
            this.serverInfo = serverInfo;
            writePreference(new Pair<String, Object>(P_APP_SERVER_INFO, serverInfo));
        }
    }


    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
        writePreference(new Pair<String, Object>(P_APP_VOLUME, volume));
    }

    public String getBoiteName() {
        return boiteName;
    }

    public void setBoiteName(String boiteName) {
        this.boiteName = boiteName;
        writePreference(new Pair<String, Object>(P_APP_BOITENAME, boiteName));
    }

    public String getBoiteId() {
        return boiteId;
    }

    public void setBoiteId(String boiteId) {
        this.boiteId = boiteId;
        writePreference(new Pair<String, Object>(P_APP_BOITEID, boiteId));
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
        writePreference(new Pair<String, Object>(P_APP_ROOMNAME, roomName));
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
        writePreference(new Pair<String, Object>(P_APP_ROOMID, roomId));
    }

    public String getBoxId() {
        return boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    public String getBoxName() {
        return boxName;
    }

    public void setBoxName(String boxName) {
        this.boxName = boxName;
        writePreference(new Pair<String, Object>(P_APP_BOXNAME, this.boxName));
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
        writePreference(new Pair<String, Object>(P_APP_ROOM_TYPE, this.roomType));
    }

    public String getAdvertMediaPeriod() {
        return advertMediaPeriod;
    }

    public void setAdvertMediaPeriod(String advertMediaPeriod) {
        this.advertMediaPeriod = advertMediaPeriod;
        writePreference(new Pair<String, Object>(P_APP_ADVERTMEDIAPERIOD, advertMediaPeriod));
    }

    public String getNextAdvertMediaPeriod() {
        return nextAdvertMediaPeriod;
    }

    public void setNextAdvertMediaPeriod(String nextAdvertMediaPeriod) {
        this.nextAdvertMediaPeriod = nextAdvertMediaPeriod;
        writePreference(new Pair<String, Object>(P_APP_NEXT_ADVERTMEDIAPERIOD, nextAdvertMediaPeriod));
    }

    public String getNextAdvertMediaPubTime() {
        return nextAdvertMediaPubTime;
    }

    public void setNextAdvertMediaPubTime(String nextAdvertMediaPubTime) {
        this.nextAdvertMediaPubTime = nextAdvertMediaPubTime;
        writePreference(new Pair<String, Object>(P_APP_NEXT_ADVERTMEDIA_PUBTIME, nextAdvertMediaPubTime));
    }

    public String getAdvertDownloadingPeriod() {
        return advertDownloadingPeriod;
    }

    public void setAdvertDownloadingPeriod(String advertDownloadingPeriod) {
        this.advertDownloadingPeriod = advertDownloadingPeriod;
        writePreference(new Pair<String, Object>(P_APP_ADVERTDOWNLOADINGPERIOD, advertDownloadingPeriod));
    }

    public String getTVLastChannel() {
        return TVLastChannel;
    }

    public void setTVLastChannel(String TVLastChannel) {
        this.TVLastChannel = TVLastChannel;
        writePreference(new Pair<String, Object>(P_APP_TVLASTCHANNEL, TVLastChannel));
    }

    public String getLogVersionCode() {
        return logVersionCode;
    }

    public void setLogVersionCode(String logVersionCode) {
        this.logVersionCode = logVersionCode;
        writePreference(new Pair<String, Object>(P_APP_LOGVERSIONCODE, logVersionCode));
    }

    public String getMulticastMediaPeriod() {
        return multicastMediaPeriod;
    }

    public void setMulticastMediaPeriod(String multicastMediaPeriod) {
        this.multicastMediaPeriod = multicastMediaPeriod;
        writePreference(new Pair<String, Object>(P_APP_MULTICASTMEDIAPERIOD, multicastMediaPeriod));
    }

    public String getMulticastDownloadingPeriod() {
        return multicastDownloadingPeriod;
    }

    public void setMulticastDownloadingPeriod(String multicastDownloadingPeriod) {
        this.multicastDownloadingPeriod = multicastDownloadingPeriod;
        writePreference(new Pair<String, Object>(P_APP_MULTICASTDOWNLOADINGPERIOD, multicastDownloadingPeriod));
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        setLastStartTime(this.startTime);
        this.startTime = startTime;
        writePreference(new Pair<String, Object>(P_APP_STARTTIME, startTime));
    }

    public String getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(String lastStartTime) {
        this.lastStartTime = lastStartTime;
        writePreference(new Pair<String, Object>(P_APP_LASTSTARTTIME, lastStartTime));
    }

    public int getSwitchTime() {
        return switchTime;
    }

    public void setSwitchTime(int switchTime) {
        this.switchTime = switchTime;
        writePreference(new Pair<String, Object>(P_APP_SWITCHTIME, switchTime));
    }

    public int getTvCurrentChannelNumber() {
        return mTvCurrentChannelNumber;
    }

    public void setTvCurrentChannelNumber(int tvCurrentChannelNumber) {
        mTvCurrentChannelNumber = tvCurrentChannelNumber;
    }

    public int getTvDefaultChannelNumber() {
        return mTvDefaultChannelNumber;
    }

    public void setTvDefaultChannelNumber(int tvDefaultChannelNumber) {
        mTvCurrentChannelNumber = mTvDefaultChannelNumber = tvDefaultChannelNumber;
        writePreference(new Pair<String, Object>(P_APP_TV_DEFAULT_CHANNEL, tvDefaultChannelNumber));
    }

    public int getTvInputSource() {
        return mTvInputSource;
    }

    public void setTvInputSource(int tvInputSource) {
        mTvInputSource = tvInputSource;
        writePreference(new Pair<String, Object>(P_APP_TV_CURRENT_INPUT, tvInputSource));
    }

    public String getEthernetMac() {
        if (TextUtils.isEmpty(mEthernetMac)) {
            mEthernetMac = AppUtils.getEthernetMacAddr();
            writePreference(new Pair<String, Object>(P_APP_ETHERNET_MAC, mEthernetMac));
        }
        return mEthernetMac;
    }

    public String getWlanMac() {
        if (TextUtils.isEmpty(mWlanMac)) {
            mWlanMac = AppUtils.getWlanMacAddr();
            writePreference(new Pair<String, Object>(P_APP_WLAN_MAC, mWlanMac));
        }
        return mWlanMac;
    }

    public String getOss_bucket() {
        return oss_bucket;
    }

    public void setOss_bucket(String oss_bucket) {
        this.oss_bucket = oss_bucket;
        writePreference(new Pair<String, Object>(P_APP_OSS_BUCKET, oss_bucket));
    }

    public String getOss_file_path() {
        return oss_file_path;
    }

    public void setOss_file_path(String oss_file_path) {
        this.oss_file_path = oss_file_path;
        writePreference(new Pair<String, Object>(P_APP_OSS_PATH, oss_file_path));
    }

    public boolean isConnectedToSP() {
        return mIsConnectedToSP;
    }

    public void setConnectedToSP(boolean connectedToSP) {
        mIsConnectedToSP = connectedToSP;
    }

    public String getSplashVersion() {
        return splashVersion;
    }

    public void setSplashVersion(String splashVersion) {
        this.splashVersion = splashVersion;
        writePreference(new Pair<String, Object>(P_APP_SPLASH_VERSION, splashVersion));
    }

    public String getAuthCode() {
        return mAuthCode;
    }

    public void setAuthCode(String authCode) {
        if (!TextUtils.isEmpty(authCode) && !authCode.equals(mAuthCode)) {
            writePreference(new Pair<String, Object>(P_APP_AUTH_CODE, authCode));
        }
        mAuthCode = authCode;
    }

    //电视播放声音
    public static final String P_APP_VOLUME = "com.savor.ads.volume";
    //酒楼名称
    public static final String P_APP_BOITENAME = "com.savor.ads.boiteName";
    //酒楼ID
    public static final String P_APP_BOITEID = "com.savor.ads.boiteId";
    //包间名称
    public static final String P_APP_ROOMNAME = "com.savor.ads.roomName";
    //包间ID
    public static final String P_APP_ROOMID = "com.savor.ads.roomId";
    // 机顶盒名称
    public static final String P_APP_BOXNAME = "com.savor.ads.boxName";
    // 包间类型
    public static final String P_APP_ROOM_TYPE = "com.savor.ads.roomType";
    //广告视频期号
    public static final String P_APP_ADVERTMEDIAPERIOD = "com.savor.ads.advertMediaPeriod";
    public static final String P_APP_NEXT_ADVERTMEDIAPERIOD = "com.savor.ads.nextAdvertMediaPeriod";
    public static final String P_APP_NEXT_ADVERTMEDIA_PUBTIME = "com.savor.ads.nextAdvertMediaPubTime";
    public static final String P_APP_ADVERTDOWNLOADINGPERIOD = "com.savor.ads.advertMediaDownloadingPeriod";
    //当从电视切换到广告后，要记录一下最后停留的电视台频道号
    public static final String P_APP_TVLASTCHANNEL = "com.savor.ads.TVLastChannel";
    //记录日志版本
    public static final String P_APP_LOGVERSIONCODE = "com.savor.ads.logVersionCode";
    //点播视频期号
    public static final String P_APP_MULTICASTMEDIAPERIOD = "com.savor.ads.multicastMediaPeriod";
    public static final String P_APP_MULTICASTDOWNLOADINGPERIOD = "com.savor.ads.multicastDownloadPeriod";
    //开机时间
    public static final String P_APP_STARTTIME = "com.savor.ads.startTime";
    public static final String P_APP_LASTSTARTTIME = "com.savor.ads.laststartTime";
    //切换时间
    public static final String P_APP_SWITCHTIME = "com.savor.ads.switchtime";
    // 电视默认频道KEY
    public static final String P_APP_TV_DEFAULT_CHANNEL = "com.savor.ads.tvDefaultChannel";
    // 电视当前信号源KEY
    public static final String P_APP_TV_CURRENT_INPUT = "com.savor.ads.tvCurrentInput";
    // 小平台信息KEY
    public static final String P_APP_SERVER_INFO = "com.savor.ads.serverInfo";
    // 以太网卡MAC地址KEY
    public static final String P_APP_ETHERNET_MAC = "com.savor.ads.ethernetMac";
    // 无线网卡MAC地址KEY
    public static final String P_APP_WLAN_MAC = "com.savor.ads.wlanMac";
    //oss桶名称
    public static final String P_APP_OSS_BUCKET = "com.savor.ads.oss.bucket";
    //oss上传路径
    public static final String P_APP_OSS_PATH = "com.savor.ads.oss.path";
    //启动图版本key
    public static final String P_APP_SPLASH_VERSION = "com.savor.ads.splashVersion";
    //呼玛验证码key
    public static final String P_APP_AUTH_CODE = "com.savor.ads.authCode";
}