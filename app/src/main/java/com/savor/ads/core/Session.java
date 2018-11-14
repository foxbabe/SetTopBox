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

import com.savor.ads.bean.PrizeInfo;
import com.savor.ads.bean.PushRTBItem;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.bean.VersionInfo;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.LogFileUtil;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

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
    private final String brand;
    private final String romVersion;
    private String versionName;
    private int versionCode;
    private String debugType;
    private boolean isDebug;
    private String appName;
    private String macAddress;
    private String token;

    private ServerInfo serverInfo;
    /** 轮播音量 */
    private int volume;
    /** 投屏音量 */
    private int projectVolume;
    /** 点播音量 */
    private int vodVolume;
    /** 电视节目音量 */
    private int tvVolume;
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

    /**下一期要播放的广告时间*/
    private String proNextMediaPubTime;

    /**节目单期号（含节目）*/
    private String proPeriod;
    /**宣传片期号*/
    private String advPeriod;
    /**广告期号*/
    private String adsPeriod;
    /**RTB广告期号*/
    private String rtbadsPeriod;
    /**百度聚屏广告期号*/
    private String polyAdsPeriod;
    /**下载中的节目单期号（含节目）*/
    private String proDownloadPeriod;
    /**下载中的宣传片期号*/
    private String advDownloadPeriod;
    /**下载中的广告期号*/
    private String adsDownloadPeriod;
    private String rtbadsDownloadPeriod;
    private String polyAdsDownloadPeriod;
    /**下一期节目单期号（含节目）*/
    private String proNextPeriod;
    /**下一期广告期号*/
    private String adsNextPeriod;
    /**下一期宣传片期号*/
    private String advNextPeriod;
    /**特色菜期号*/
    private String specialtyPeriod;
    /**下载中特色菜期号*/
    private String downloadingSpecialtyPeriod;


    private ArrayList<VersionInfo> mVodVersion;
    private ArrayList<VersionInfo> mDownloadingVodVersion;

    //点播视频期号
    private String multicastMediaPeriod;
    private String vodPeriod;
    private String vodDownloadPeriod;

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
     * 以太网卡MAC地址
     */
    private String mEthernetMacWithColon;
    /**
     * 无线网卡MAC地址
     */
    private String mWlanMac;

    /**
     * oss区域ID
     */
    private String ossAreaId;

    private boolean mIsConnectedToSP;
    /** 呼玛验证码*/
    private String mAuthCode;
    /** 启动图路径*/
    private String mSplashPath;
    /** 加载图路径*/
    private String mLoadingPath;
    /** 小平台中的所有版本号期号等信息*/
    private ArrayList<VersionInfo> mSPVersionInfo;
    /**电视机尺寸**/
    private int tvSize;
    /** 启动图版本*/
    private String mSplashVersion;
    /** 加载图版本*/
    private String mLoadingVersion;

    /** 抽奖奖项信息*/
    private PrizeInfo mPrizeInfo;
    /** 是否使用虚拟小平台*/
    private boolean mUseVirtualSp;
    /**单机版U盘目录*/
    private String usbPath;
    /**是否是单机版机顶盒*/
    private boolean standalone=false;

    private int admaster_update_time;
	/**上次U盘更新时间*/
    private String lastUDiskUpdateTime;

    private ArrayList<PushRTBItem> mPushRTBItems;
    /**记录下载速度**/
	private String netSpeed;
	/**后台配置时候是显示小程序码的屏**/
    private boolean isShowMiniProgramIcon;
    /**小程序NETTY服务是否存活**/
    private boolean isHeartbeatMiniNetty;
    private String nettyUrl;
    private int nettyPort;
    /**是否开机请求过netty最新负载地址*/
    private boolean isGetNettyBalancing;
    /**
     * 当前apk版本是否支持负载均衡
     *0:不支持
     *1:支持
     */
    private int localSupportNettyBalance=1;
    private boolean isSupportNettyBalance;
	private HashMap<String,Long> downloadFilePosition = new HashMap<>();
	/**是否已经下载小程序码小码**/
	private boolean isDownloadMiniProgramSmallIcon;
	/**是否已经下载小程序码大码**/
	private boolean isDownloadMiniProgramBigIcon;
	/**是否已经下载小程序码呼码**/
	private boolean isDownloadMiniProgramCallIcon;
    private Session(Context context) {

        mContext = context;
        mPreference = new SaveFileData(context, "savor");
        osVersion = Build.VERSION.SDK_INT;
        buildVersion = Build.VERSION.RELEASE;
        model = Build.MODEL;
        brand = Build.BRAND;
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
        volume = mPreference.loadIntKey(P_APP_VOLUME, ConstantValues.DEFAULT_ADS_VOLUME);
        projectVolume = mPreference.loadIntKey(P_APP_PROJECT_VOLUME, ConstantValues.DEFAULT_PROJECT_VOLUME);
        vodVolume = mPreference.loadIntKey(P_APP_VOD_VOLUME, ConstantValues.DEFAULT_VOD_VOLUME);
        tvVolume = mPreference.loadIntKey(P_APP_TV_VOLUME, ConstantValues.DEFAULT_TV_VOLUME);
        boiteName = mPreference.loadStringKey(P_APP_BOITENAME, null);
        boiteId = mPreference.loadStringKey(P_APP_BOITEID, null);
        roomName = mPreference.loadStringKey(P_APP_ROOMNAME, null);
        roomId = mPreference.loadStringKey(P_APP_ROOMID, null);
        boxName = mPreference.loadStringKey(P_APP_BOXNAME, null);
        boxId = mPreference.loadStringKey(P_APP_BOXID, null);
        roomType = mPreference.loadStringKey(P_APP_ROOM_TYPE, null);

        proPeriod = mPreference.loadStringKey(P_APP_PRO_MEDIA_PERIOD,"");
        proDownloadPeriod = mPreference.loadStringKey(P_APP_PRO_DOWNLOAD_MEDIA_PERIOD,"");
        proNextPeriod = mPreference.loadStringKey(P_APP_PRO_NEXT_MEDIA_PERIOD,"");
        proNextMediaPubTime = mPreference.loadStringKey(P_APP_PRO_NEXT_MEDIA_PUBTIME,"");
        advPeriod = mPreference.loadStringKey(P_APP_ADV_MEDIA_PERIOD,"");
        advDownloadPeriod = mPreference.loadStringKey(P_APP_ADV_DOWNLOAD_MEDIA_PERIOD,"");
        advNextPeriod = mPreference.loadStringKey(P_APP_ADV_NEXT_MEDIA_PERIOD,"");
        adsPeriod = mPreference.loadStringKey(P_APP_ADS_MEIDA_PERIOD,"");
        adsDownloadPeriod = mPreference.loadStringKey(P_APP_ADS_DOWNLOAD_MEIDA_PERIOD, "");
        rtbadsPeriod = mPreference.loadStringKey(P_APP_RTBADS_MEIDA_PERIOD,"");
        rtbadsDownloadPeriod = mPreference.loadStringKey(P_APP_RTBADS_DOWNLOAD_MEIDA_PERIOD, "");
        polyAdsPeriod = mPreference.loadStringKey(P_APP_POLYADS_MEDIA_PERIOD,"");
        polyAdsDownloadPeriod = mPreference.loadStringKey(P_APP_POLYADS_DOWNLOAD_MEDIA_PERIOD,"");

        multicastMediaPeriod = mPreference.loadStringKey(P_APP_MULTICASTMEDIAPERIOD, "");
        startTime = mPreference.loadStringKey(P_APP_STARTTIME, null);
        lastStartTime = mPreference.loadStringKey(P_APP_LASTSTARTTIME, null);
        switchTime = mPreference.loadIntKey(P_APP_SWITCHTIME, ConstantValues.DEFAULT_SWITCH_TIME);

        mTvInputSource = mPreference.loadIntKey(P_APP_TV_CURRENT_INPUT, 0);
        mTvCurrentChannelNumber = mTvDefaultChannelNumber = mPreference.loadIntKey(P_APP_TV_DEFAULT_CHANNEL, 0);
        serverInfo = (ServerInfo) StringToObject(mPreference.loadStringKey(P_APP_SERVER_INFO, null));
        mEthernetMac = mPreference.loadStringKey(P_APP_ETHERNET_MAC, null);
        mWlanMac = mPreference.loadStringKey(P_APP_WLAN_MAC, null);
        ossAreaId = mPreference.loadStringKey(P_APP_OSS_AREA_ID,null);
        mAuthCode = mPreference.loadStringKey(P_APP_AUTH_CODE,null);
        mSplashPath = mPreference.loadStringKey(P_APP_SPLASH_PATH, "/Pictures/logo.jpg");
        mLoadingPath = mPreference.loadStringKey(P_APP_LOADING_PATH, "/Pictures/loading.jpg");
        mSplashVersion = mPreference.loadStringKey(P_APP_SPLASH_VERSION, "");
        mLoadingVersion = mPreference.loadStringKey(P_APP_LOADING_VERSION, "");
        mSPVersionInfo = (ArrayList<VersionInfo>) StringToObject(mPreference.loadStringKey(P_APP_SP_VERSION_INFO, ""));
        tvSize = mPreference.loadIntKey(P_APP_TV_SIZE,0);
        // 以下三个方法目前作为新老期号存储方式过渡使用
        setPlayListVersion((ArrayList<VersionInfo>)StringToObject(mPreference.loadStringKey(P_APP_PLAY_LIST_VERSION, "")));
        setDownloadingPlayListVersion((ArrayList<VersionInfo>) StringToObject(mPreference.loadStringKey(P_APP_DOWNLOADING_PLAY_LIST_VERSION, "")));
        setNextPlayListVersion((ArrayList<VersionInfo>) StringToObject(mPreference.loadStringKey(P_APP_NEXT_PLAY_LIST_VERSION, "")));

        setVodVersion((ArrayList<VersionInfo>)StringToObject(mPreference.loadStringKey(P_APP_VOD_VERSION, "")));
        setDownloadingVodVersion((ArrayList<VersionInfo>) StringToObject(mPreference.loadStringKey(P_APP_DOWNLOADING_VOD_VERSION, "")));
        mPrizeInfo = (PrizeInfo) StringToObject(mPreference.loadStringKey(P_APP_PRIZE_INFO, ""));
        mUseVirtualSp = mPreference.loadBooleanKey(P_APP_USE_VIRTUAL_SP, false);
        standalone = mPreference.loadBooleanKey(P_APP_STAND_ALONE,false);
        lastUDiskUpdateTime = mPreference.loadStringKey(P_APP_LAST_UDISK_UPDATE_TIME, "");
        admaster_update_time = mPreference.loadIntKey(P_APP_ADMASTER_UPDATE_TIME,0);
        mPushRTBItems = (ArrayList<PushRTBItem>) StringToObject(mPreference.loadStringKey(P_APP_RTB_PUSH_CONTENT, ""));
        netSpeed = mPreference.loadStringKey(P_APP_DOWNLOAD_NET_SPEED,"");
        isShowMiniProgramIcon = mPreference.loadBooleanKey(P_APP_SHOW_MIMIPROGRAM,false);
        isHeartbeatMiniNetty = mPreference.loadBooleanKey(P_APP_HEARTBEAT_MIMIPROGRAM,false);
        nettyUrl = mPreference.loadStringKey(P_APP_NETTY_URL,null);
        nettyPort = mPreference.loadIntKey(P_APP_NETTY_PORT,0);
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
                || P_APP_BOXID.equals(key)
                || P_APP_ROOM_TYPE.equals(key)
                || P_APP_PRO_MEDIA_PERIOD.equals(key)
                || P_APP_PRO_DOWNLOAD_MEDIA_PERIOD.equals(key)
                || P_APP_PRO_NEXT_MEDIA_PERIOD.equals(key)
                || P_APP_PRO_NEXT_MEDIA_PUBTIME.equals(key)
                || P_APP_ADV_MEDIA_PERIOD.equals(key)
                || P_APP_ADV_DOWNLOAD_MEDIA_PERIOD.equals(key)
                || P_APP_ADV_NEXT_MEDIA_PERIOD.equals(key)
                || P_APP_ADS_MEIDA_PERIOD.equals(key)
                || P_APP_RTBADS_MEIDA_PERIOD.equals(key)
                || P_APP_RTBADS_DOWNLOAD_MEIDA_PERIOD.equals(key)
                || P_APP_ADS_DOWNLOAD_MEIDA_PERIOD.equals(key)
                || P_APP_MULTICASTMEDIAPERIOD.equals(key)
                || P_APP_STARTTIME.equals(key)
                || P_APP_LASTSTARTTIME.equals(key)
                || P_APP_ETHERNET_MAC.equals(key)
                || P_APP_WLAN_MAC.equals(key)
                || P_APP_OSS_AREA_ID.equals(key)
                || P_APP_AUTH_CODE.equals(key)
                || P_APP_SPLASH_PATH.equals(key)
                || P_APP_LOADING_PATH.equals(key)
                || P_APP_SPLASH_VERSION.equals(key)
                || P_APP_LOADING_VERSION.equals(key)
                || P_APP_LAST_UDISK_UPDATE_TIME.equals(key)
                || P_APP_SPECIALTY_PERIOD.equals(key)
                || P_APP_DOWNLOADING_SPECIALTY_PERIOD.equals(key)
                || P_APP_LOADING_VERSION.equals(key)
                || P_APP_DOWNLOAD_NET_SPEED.equals(key)
                || P_APP_NETTY_URL.equals(key)) {

            mPreference.saveStringKey(key, (String) updateItem.second);

        } else if (P_APP_VOLUME.equals(key) ||
                P_APP_PROJECT_VOLUME.equals(key) ||
                P_APP_VOD_VOLUME.equals(key) ||
                P_APP_TV_VOLUME.equals(key) ||
                P_APP_TV_DEFAULT_CHANNEL.equals(key) ||
                P_APP_TV_CURRENT_INPUT.equals(key) ||
                P_APP_SWITCHTIME.equals(key) ||
                P_APP_TV_SIZE.equals(key)||
                P_APP_NETTY_PORT.equals(key)) {
            mPreference.saveIntKey(key, (int) updateItem.second);
        } else if (P_APP_USE_VIRTUAL_SP.equals(key)||
                P_APP_STAND_ALONE.equals(key)) {
            mPreference.saveBooleanKey(key, (boolean) updateItem.second);
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
        return versionName == null ? "" : versionName;
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

    public String getBrand() {
        return brand;
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

    public ArrayList<VersionInfo> getSPVersionInfo() {
        return mSPVersionInfo;
    }

    public void setSPVersionInfo(ArrayList<VersionInfo> SPVersionInfo) {
        mSPVersionInfo = SPVersionInfo;
        writePreference(new Pair<String, Object>(P_APP_SP_VERSION_INFO, mSPVersionInfo));
    }

    public int getTvSize() {
        return tvSize;
    }

    public void setTvSize(int tvSize) {
        this.tvSize = tvSize;
        writePreference(new Pair<String, Object>(P_APP_TV_SIZE, tvSize));
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
        writePreference(new Pair<String, Object>(P_APP_VOLUME, volume));
    }
    public int getProjectVolume() {
        return projectVolume;
    }

    public void setProjectVolume(int volume) {
        this.projectVolume = volume;
        writePreference(new Pair<String, Object>(P_APP_PROJECT_VOLUME, volume));
    }
    public int getVodVolume() {
        return vodVolume;
    }

    public void setVodVolume(int volume) {
        this.vodVolume = volume;
        writePreference(new Pair<String, Object>(P_APP_VOD_VOLUME, volume));
    }
    public int getTvVolume() {
        return tvVolume;
    }

    public void setTvVolume(int volume) {
        this.tvVolume = volume;
        writePreference(new Pair<String, Object>(P_APP_TV_VOLUME, volume));
    }

    public String getBoiteName() {
        return boiteName;
    }

    public void setBoiteName(String boiteName) {
        this.boiteName = boiteName;
        writePreference(new Pair<String, Object>(P_APP_BOITENAME, boiteName));
    }

    public String getBoiteId() {
        return boiteId == null ? "" : boiteId;
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
        return roomId == null ? "" : roomId;
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
        writePreference(new Pair<String, Object>(P_APP_BOXID, this.boxId));
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

    public String getProNextMediaPubTime() {
        return proNextMediaPubTime;
    }

    public void setProNextMediaPubTime(String proNextMediaPubTime) {
        this.proNextMediaPubTime = proNextMediaPubTime;
        writePreference(new Pair<String, Object>(P_APP_PRO_NEXT_MEDIA_PUBTIME,proNextMediaPubTime));
    }

    public void setPlayListVersion(ArrayList<VersionInfo> playListVersion) {
        if (playListVersion != null) {
            setAdsPeriod(AppUtils.findSpecifiedPeriodByType(playListVersion, "ads"));
            setAdvPeriod(AppUtils.findSpecifiedPeriodByType(playListVersion, "adv"));
            setProPeriod(AppUtils.findSpecifiedPeriodByType(playListVersion, "pro"));
            mPreference.removeKey(P_APP_PLAY_LIST_VERSION);
        }
    }

    public void setNextPlayListVersion(ArrayList<VersionInfo> nextPlayListVersion) {
        if (nextPlayListVersion != null) {
            setAdsNextPeriod(AppUtils.findSpecifiedPeriodByType(nextPlayListVersion, "ads"));
            setAdvNextPeriod(AppUtils.findSpecifiedPeriodByType(nextPlayListVersion, "adv"));
            setProNextPeriod(AppUtils.findSpecifiedPeriodByType(nextPlayListVersion, "pro"));
            mPreference.removeKey(P_APP_NEXT_PLAY_LIST_VERSION);
        }
    }

   public void setDownloadingPlayListVersion(ArrayList<VersionInfo> downloadingPlayListVersion) {
       if (downloadingPlayListVersion != null) {
           setAdsDownloadPeriod(AppUtils.findSpecifiedPeriodByType(downloadingPlayListVersion, "ads"));
           setAdvDownloadPeriod(AppUtils.findSpecifiedPeriodByType(downloadingPlayListVersion, "adv"));
           setProDownloadPeriod(AppUtils.findSpecifiedPeriodByType(downloadingPlayListVersion, "pro"));
           mPreference.removeKey(P_APP_DOWNLOADING_PLAY_LIST_VERSION);
       }
   }

    public void setProPeriod(String proPeriod) {
        this.proPeriod = proPeriod;
        writePreference(new Pair<String, Object>(P_APP_PRO_MEDIA_PERIOD,proPeriod));
    }

    public void setAdvPeriod(String advPeriod) {
        this.advPeriod = advPeriod;
        writePreference(new Pair<String, Object>(P_APP_ADV_MEDIA_PERIOD,advPeriod));
    }

    public void setAdsPeriod(String adsPeriod) {
        this.adsPeriod = adsPeriod;
        writePreference(new Pair<String, Object>(P_APP_ADS_MEIDA_PERIOD,adsPeriod));
    }

    public void setRtbAdsPeriod(String adsPeriod) {
        this.rtbadsPeriod = adsPeriod;
        writePreference(new Pair<String, Object>(P_APP_RTBADS_MEIDA_PERIOD,adsPeriod));
    }

    public String getPolyAdsPeriod() {
        return polyAdsPeriod;
    }

    public void setPolyAdsPeriod(String polyAdsPeriod) {
        this.polyAdsPeriod = polyAdsPeriod;
        writePreference(new Pair<String, Object>(P_APP_POLYADS_MEDIA_PERIOD,polyAdsPeriod));
    }

    public String getPolyAdsDownloadPeriod() {
        return polyAdsDownloadPeriod;
    }

    public void setPolyAdsDownloadPeriod(String polyAdsDownloadPeriod) {
        this.polyAdsDownloadPeriod = polyAdsDownloadPeriod;
        writePreference(new Pair<String, Object>(P_APP_POLYADS_DOWNLOAD_MEDIA_PERIOD,polyAdsDownloadPeriod));
    }

    public void setProDownloadPeriod(String proDownloadPeriod) {
        this.proDownloadPeriod = proDownloadPeriod;
        writePreference(new Pair<String, Object>(P_APP_PRO_DOWNLOAD_MEDIA_PERIOD,proDownloadPeriod));
    }

    public void setAdvDownloadPeriod(String advDownloadPeriod) {
        this.advDownloadPeriod = advDownloadPeriod;
        writePreference(new Pair<String, Object>(P_APP_ADV_DOWNLOAD_MEDIA_PERIOD,advDownloadPeriod));
    }

    public void setAdsDownloadPeriod(String adsDownloadPeriod) {
        this.adsDownloadPeriod = adsDownloadPeriod;
        writePreference(new Pair<String, Object>(P_APP_ADS_DOWNLOAD_MEIDA_PERIOD, adsDownloadPeriod));
    }

    public void setRtbadsDownloadPeriod(String rtbadsDownloadPeriod) {
        this.rtbadsDownloadPeriod = rtbadsDownloadPeriod;
        writePreference(new Pair<String, Object>(P_APP_ADS_DOWNLOAD_MEIDA_PERIOD, rtbadsDownloadPeriod));
    }

    public void setProNextPeriod(String proNextPeriod) {
        this.proNextPeriod = proNextPeriod;
        writePreference(new Pair<String, Object>(P_APP_PRO_NEXT_MEDIA_PERIOD,proNextPeriod));
    }

    public void setAdsNextPeriod(String adsNextPeriod) {
        this.adsNextPeriod = adsNextPeriod;
    }

    public void setAdvNextPeriod(String advNextPeriod) {
        this.advNextPeriod = advNextPeriod;
        writePreference(new Pair<String, Object>(P_APP_ADV_NEXT_MEDIA_PERIOD, advNextPeriod));
    }

    public ArrayList<VersionInfo> getVodVersion() {
        return mVodVersion;
    }

    public void setVodVersion(ArrayList<VersionInfo> vodVersion) {
        mVodVersion = vodVersion;
        if (mVodVersion != null) {
            vodPeriod = AppUtils.findSpecifiedPeriodByType(mVodVersion, "vod");
        }
        writePreference(new Pair<String, Object>(P_APP_VOD_VERSION, mVodVersion));
    }

    public ArrayList<VersionInfo> getDownloadingVodVersion() {
        return mDownloadingVodVersion;
    }

    public void setDownloadingVodVersion(ArrayList<VersionInfo> downloadingVodVersion) {
        mDownloadingVodVersion = downloadingVodVersion;
        if (mDownloadingVodVersion != null) {
            vodDownloadPeriod = AppUtils.findSpecifiedPeriodByType(mDownloadingVodVersion, "vod");
        }
        writePreference(new Pair<String, Object>(P_APP_DOWNLOADING_VOD_VERSION, mDownloadingVodVersion));
    }

    public String getMulticastMediaPeriod() {
        return multicastMediaPeriod == null ? "" : multicastMediaPeriod;
    }

    public void setMulticastMediaPeriod(String multicastMediaPeriod) {
        this.multicastMediaPeriod = multicastMediaPeriod;
        writePreference(new Pair<String, Object>(P_APP_MULTICASTMEDIAPERIOD, multicastMediaPeriod));
    }



    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        setLastStartTime(this.startTime);
        this.startTime = startTime;
        writePreference(new Pair<String, Object>(P_APP_STARTTIME, startTime));
        LogFileUtil.writeBootInfo(startTime);
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
            String mac = AppUtils.getEthernetMacAddr();
            if (!TextUtils.isEmpty(mac)) {
                mEthernetMac = mac.replaceAll(":", "");
                writePreference(new Pair<String, Object>(P_APP_ETHERNET_MAC, mEthernetMac));
            }
        }
        return mEthernetMac == null ? "" : mEthernetMac;
    }

    public String getEthernetMacWithColon() {
        if (TextUtils.isEmpty(mEthernetMacWithColon)) {
            mEthernetMacWithColon = AppUtils.getEthernetMacAddr();
            writePreference(new Pair<String, Object>(P_APP_ETHERNET_MAC_WITH_COLON, mEthernetMacWithColon));
        }
        return mEthernetMacWithColon == null ? "" : mEthernetMacWithColon;
    }

    public String getWlanMac() {
        if (TextUtils.isEmpty(mWlanMac)) {
            mWlanMac = AppUtils.getWlanMacAddr();
            writePreference(new Pair<String, Object>(P_APP_WLAN_MAC, mWlanMac));
        }
        return mWlanMac;
    }


    public String getOssAreaId() {
        return ossAreaId;
    }

    public void setOssAreaId(String ossAreaId) {
        this.ossAreaId = ossAreaId;
        writePreference(new Pair<String, Object>(P_APP_OSS_AREA_ID, ossAreaId));
    }

    public boolean isConnectedToSP() {
        return mIsConnectedToSP;
    }

    public void setConnectedToSP(boolean connectedToSP) {
        mIsConnectedToSP = connectedToSP;
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

    public String getSplashPath() {
        return mSplashPath;
    }

    public void setSplashPath(String splashPath) {
        mSplashPath = splashPath;
        writePreference(new Pair<String, Object>(P_APP_SPLASH_PATH, splashPath));
    }

    public String getLoadingPath() {
        return mLoadingPath;
    }

    public void setLoadingPath(String loadingPath) {
        mLoadingPath = loadingPath;
        writePreference(new Pair<String, Object>(P_APP_LOADING_PATH, loadingPath));
    }

    public String getSplashVersion() {
        return mSplashVersion == null ? "" : mSplashVersion;
    }

    public void setSplashVersion(String splashVersion) {
        if (TextUtils.isEmpty(mSplashVersion) || !mSplashVersion.equals(splashVersion)) {
            mSplashVersion = splashVersion;
            writePreference(new Pair<String, Object>(P_APP_SPLASH_VERSION, splashVersion));
        }
    }

    public String getLoadingVersion() {
        return mLoadingVersion == null ? "" : mLoadingVersion;
    }

    public void setLoadingVersion(String loadingVersion) {
        if (TextUtils.isEmpty(mLoadingVersion) || !mLoadingVersion.equals(loadingVersion)) {
            mLoadingVersion = loadingVersion;
            writePreference(new Pair<String, Object>(P_APP_LOADING_VERSION, loadingVersion));
        }
    }


    public String getUsbPath() {
        return usbPath;
    }

    public void setUsbPath(String usbPath) {
        this.usbPath = usbPath;
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void setStandalone(boolean standalone) {
        this.standalone = standalone;
        writePreference(new Pair<String, Object>(P_APP_STAND_ALONE, standalone));
    }

    public int getAdmaster_update_time() {
        return admaster_update_time;
    }

    public void setAdmaster_update_time(int admaster_update_time) {
        this.admaster_update_time = admaster_update_time;
        writePreference(new Pair<String, Object>(P_APP_ADMASTER_UPDATE_TIME,admaster_update_time));
    }

    public String getSpecialtyPeriod() {
        return specialtyPeriod == null ? "" : specialtyPeriod;
    }

    public void setSpecialtyPeriod(String specialtyPeriod) {
        if (TextUtils.isEmpty(this.specialtyPeriod) || !this.specialtyPeriod.equals(specialtyPeriod)) {
            this.specialtyPeriod = specialtyPeriod;
            writePreference(new Pair<String, Object>(P_APP_SPECIALTY_PERIOD, specialtyPeriod));
        }
    }

    public String getDownloadingSpecialtyPeriod() {
        return downloadingSpecialtyPeriod;
    }

    public void setDownloadingSpecialtyPeriod(String downloadingSpecialtyPeriod) {
        if (TextUtils.isEmpty(this.downloadingSpecialtyPeriod) || !this.downloadingSpecialtyPeriod.equals(downloadingSpecialtyPeriod)) {
            this.downloadingSpecialtyPeriod = downloadingSpecialtyPeriod;
            writePreference(new Pair<String, Object>(P_APP_DOWNLOADING_SPECIALTY_PERIOD, downloadingSpecialtyPeriod));
        }

    }

    public String getNetSpeed() {
        return netSpeed;
    }

    public void setNetSpeed(String netSpeed) {
        this.netSpeed = netSpeed;
        writePreference(new Pair<String, Object>(P_APP_DOWNLOAD_NET_SPEED,netSpeed));
    }

    public boolean isShowMiniProgramIcon() {
        return isShowMiniProgramIcon;
    }

    public void setShowMiniProgramIcon(boolean showMiniProgramIcon) {
        isShowMiniProgramIcon = showMiniProgramIcon;
        writePreference(new Pair<String, Object>(P_APP_SHOW_MIMIPROGRAM,showMiniProgramIcon));
    }

    public boolean isHeartbeatMiniNetty() {
        return isHeartbeatMiniNetty;
    }

    public void setHeartbeatMiniNetty(boolean heartbeatMiniNetty) {
        isHeartbeatMiniNetty = heartbeatMiniNetty;
        writePreference(new Pair<String, Object>(P_APP_HEARTBEAT_MIMIPROGRAM,heartbeatMiniNetty));
    }

    public String getNettyUrl() {
        return nettyUrl;
    }

    public void setNettyUrl(String nettyUrl) {
        this.nettyUrl = nettyUrl;
        writePreference(new Pair<String, Object>(P_APP_NETTY_URL,nettyUrl));
    }

    public int getNettyPort() {
        return nettyPort;
    }

    public void setNettyPort(int nettyPort) {
        this.nettyPort = nettyPort;
        writePreference(new Pair<String, Object>(P_APP_NETTY_PORT,nettyPort));
    }

    public boolean isGetNettyBalancing() {
        return isGetNettyBalancing;
    }

    public void setGetNettyBalancing(boolean getNettyBalancing) {
        isGetNettyBalancing = getNettyBalancing;
    }

    public int getLocalSupportNettyBalance() {
        return localSupportNettyBalance;
    }

    public void setLocalSupportNettyBalance(int localSupportNettyBalance) {
        this.localSupportNettyBalance = localSupportNettyBalance;
    }

    public boolean isSupportNettyBalance() {
        return isSupportNettyBalance;
    }

    public void setSupportNettyBalance(boolean supportNettyBalance) {
        isSupportNettyBalance = supportNettyBalance;
    }

    public HashMap<String, Long> getDownloadFilePosition() {
        return downloadFilePosition;
    }

    public void setDownloadFilePosition(HashMap<String, Long> downloadFilePosition) {
        this.downloadFilePosition = downloadFilePosition;
    }

    public static String getTAG() {
        return TAG;
    }

    public boolean isDownloadMiniProgramSmallIcon() {
        return isDownloadMiniProgramSmallIcon;
    }

    public void setDownloadMiniProgramSmallIcon(boolean downloadMiniProgramSmallIcon) {
        isDownloadMiniProgramSmallIcon = downloadMiniProgramSmallIcon;
    }

    public boolean isDownloadMiniProgramBigIcon() {
        return isDownloadMiniProgramBigIcon;
    }

    public void setDownloadMiniProgramBigIcon(boolean downloadMiniProgramBigIcon) {
        isDownloadMiniProgramBigIcon = downloadMiniProgramBigIcon;
    }

    public boolean isDownloadMiniProgramCallIcon() {
        return isDownloadMiniProgramCallIcon;
    }

    public void setDownloadMiniProgramCallIcon(boolean downloadMiniProgramCallIcon) {
        isDownloadMiniProgramCallIcon = downloadMiniProgramCallIcon;
    }

    //轮播播放声音
    public static final String P_APP_VOLUME = "com.savor.ads.volume";
    //投屏播放声音
    public static final String P_APP_PROJECT_VOLUME = "com.savor.ads.project_volume";
    //点播播放声音
    public static final String P_APP_VOD_VOLUME = "com.savor.ads.vod_volume";
    //电视节目播放声音
    public static final String P_APP_TV_VOLUME = "com.savor.ads.tv_volume";
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
    // 机顶盒ID
    public static final String P_APP_BOXID = "com.savor.ads.boxId";
    // 包间类型
    public static final String P_APP_ROOM_TYPE = "com.savor.ads.roomType";
    /**节目单期号(含节目内容)*/
    public static final String P_APP_PRO_MEDIA_PERIOD = "com.savor.pro.mediaPeriod";
    public static final String P_APP_PRO_DOWNLOAD_MEDIA_PERIOD = "com.savor.pro.downloadMediaPeriod";
    public static final String P_APP_PRO_NEXT_MEDIA_PERIOD= "com.savor.pro.nextMediaPeriod";
    public static final String P_APP_PRO_NEXT_MEDIA_PUBTIME = "com.savor.pro.nextPubTime";
    /**宣传片相关期号*/
    public static final String P_APP_ADV_MEDIA_PERIOD = "com.savor.adv.mediaPeriod";
    public static final String P_APP_ADV_DOWNLOAD_MEDIA_PERIOD = "com.savor.adv.downloadMediaPeriod";
    public static final String P_APP_ADV_NEXT_MEDIA_PERIOD = "com.savor.adv.nextMediaPeriod";
    /**广告相关期号*/
    public static final String P_APP_ADS_MEIDA_PERIOD = "com.savor.ads.mediaPeriod";
    public static final String P_APP_ADS_DOWNLOAD_MEIDA_PERIOD = "com.savor.ads.downloadMediaPeriod";
    public static final String P_APP_RTBADS_MEIDA_PERIOD = "com.savor.ads.rtbMediaPeriod";
    public static final String P_APP_RTBADS_DOWNLOAD_MEIDA_PERIOD = "com.savor.ads.rtbDownloadMediaPeriod";
    public static final String P_APP_POLYADS_MEDIA_PERIOD = "com.savor.ads.polyMediaPeriod";
    public static final String P_APP_POLYADS_DOWNLOAD_MEDIA_PERIOD = "com.saovr.ads.polyDownloadMediaPeriod";

    /** 当前点播期号KEY*/
    public static final String P_APP_VOD_VERSION = "com.savor.ads.vod_version";
    /** 下载中点播期号KEY*/
    public static final String P_APP_DOWNLOADING_VOD_VERSION = "com.savor.ads.downloading_vod_version";
    /** 当前特色菜期号KEY*/
    public static final String P_APP_SPECIALTY_PERIOD = "com.savor.ads.specialty_period";
    /** 下载中特色菜期号KEY*/
    public static final String P_APP_DOWNLOADING_SPECIALTY_PERIOD = "com.savor.ads.downloading_specialty_period";
    //点播视频期号
    public static final String P_APP_MULTICASTMEDIAPERIOD = "com.savor.ads.multicastMediaPeriod";
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
    public static final String P_APP_ETHERNET_MAC_WITH_COLON = "com.savor.ads.ethernetMacWithColon";
    // 无线网卡MAC地址KEY
    public static final String P_APP_WLAN_MAC = "com.savor.ads.wlanMac";
    //oss区域ID
    public static final String P_APP_OSS_AREA_ID = "com.savor.ads.oss.areaId";
    //启动图版本key
    public static final String P_APP_SPLASH_VERSION = "com.savor.ads.splashVersion";
    //呼玛验证码key
    public static final String P_APP_AUTH_CODE = "com.savor.ads.authCode";
    //启动图路径key
    public static final String P_APP_SPLASH_PATH = "com.savor.ads.splashPath";
    //加载图路径key
    public static final String P_APP_LOADING_PATH = "com.savor.ads.loadingPath";
    //加载图版本key
    public static final String P_APP_LOADING_VERSION = "com.savor.ads.loadingVersion";
    //小平台中的各种版本信息key
    public static final String P_APP_SP_VERSION_INFO = "com.savor.ads.spVersionInfo";
    public static final String P_APP_TV_SIZE = "com.savor.ads.tvSize";
    // 奖项设置key
    public static final String P_APP_PRIZE_INFO = "com.savor.ads.prizeInfo";
    // 是否使用虚拟小平台key
    public static final String P_APP_USE_VIRTUAL_SP = "com.savor.ads.use_virtual_sp";
    //是否是单机版机顶盒
    public static final String P_APP_STAND_ALONE = "com.savor.ads.stand_alone";
    //上次U盘动作时间
    public static final String P_APP_LAST_UDISK_UPDATE_TIME = "com.savor.ads.last_udisk_update_time";

    //admster配置文件版本
    public static final String P_APP_ADMASTER_UPDATE_TIME = "com.savor.ads.admaster_update_time";

    public static final String P_APP_PLAY_LIST_VERSION = "com.savor.ads.play_list_version";
    public static final String P_APP_DOWNLOADING_PLAY_LIST_VERSION = "com.savor.ads.downloading_play_list_version";
    public static final String P_APP_NEXT_PLAY_LIST_VERSION = "com.savor.ads.next_play_list_version";

    public static final String P_APP_RTB_PUSH_CONTENT = "com.savor.ads.rtb_push_content";

    public static final String P_APP_DOWNLOAD_NET_SPEED = "com.savor.download.net_speed";

    public static final String P_APP_SHOW_MIMIPROGRAM = "com.savor.ads.show.miniprogram";
    public static final String P_APP_HEARTBEAT_MIMIPROGRAM = "com.savor.ads.heartbeat.miniprogram";
    public static final String P_APP_NETTY_URL = "com.savor.ads.netty_url";
    public static final String P_APP_NETTY_PORT = "com.savor.ads.netty_port";

    public String getAdsPeriod() {
        return adsPeriod == null ? "" : adsPeriod;
    }

    public String getRtbadsPeriod() {
        return rtbadsPeriod == null ? "" : rtbadsPeriod;
    }

    public String getAdvPeriod() {
        return advPeriod == null ? "" : advPeriod;
    }

    public String getProPeriod() {
        return proPeriod == null ? "" : proPeriod;
    }

    public String getVodPeriod() {
        return vodPeriod == null ? "" : vodPeriod;
    }

    public String getAdsDownloadPeriod() {
        return adsDownloadPeriod == null ? "" : adsDownloadPeriod;
    }

    public String getRtbadsDownloadPeriod() {
        return rtbadsDownloadPeriod == null ? "" : rtbadsDownloadPeriod;
    }

    public String getAdvDownloadPeriod() {
        return advDownloadPeriod == null ? "" : advDownloadPeriod;
    }

    public String getProDownloadPeriod() {
        return proDownloadPeriod == null ? "" : proDownloadPeriod;
    }

    public String getAdsNextPeriod() {
        return adsNextPeriod == null ? "" : adsNextPeriod;
    }

    public String getAdvNextPeriod() {
        return advNextPeriod == null ? "" : advNextPeriod;
    }

    public String getProNextPeriod() {
        return proNextPeriod == null ? "" : proNextPeriod;
    }

    public String getVodDownloadPeriod() {
        return vodDownloadPeriod == null ? "" : vodDownloadPeriod;
    }

    public PrizeInfo getPrizeInfo() {
        return mPrizeInfo;
    }

    public void setPrizeInfo(PrizeInfo prizeInfo) {
        mPrizeInfo = prizeInfo;
        writePreference(new Pair<String, Object>(P_APP_PRIZE_INFO, mPrizeInfo));
    }

    public boolean isUseVirtualSp() {
        return mUseVirtualSp;
    }

    public void setUseVirtualSp(boolean useVirtualSp) {
        if (useVirtualSp != mUseVirtualSp) {
            mUseVirtualSp = useVirtualSp;
            writePreference(new Pair<String, Object>(P_APP_USE_VIRTUAL_SP, mUseVirtualSp));
        }
    }

    public String getLastUDiskUpdateTime() {
        return lastUDiskUpdateTime;
    }

    public void setLastUDiskUpdateTime(String lastUDiskUpdateTime) {
        this.lastUDiskUpdateTime = lastUDiskUpdateTime;
        writePreference(new Pair<String, Object>(P_APP_LAST_UDISK_UPDATE_TIME, lastUDiskUpdateTime));
    }

    public ArrayList<PushRTBItem> getRTBPushItems() {
        return mPushRTBItems;
    }

    public void setRTBPushItems(ArrayList<PushRTBItem> PushRTBItems) {
        mPushRTBItems = PushRTBItems;
        writePreference(new Pair<String, Object>(P_APP_RTB_PUSH_CONTENT, mPushRTBItems));
    }
}