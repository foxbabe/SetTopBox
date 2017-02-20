package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class PrepareRequestVo implements Serializable {
    private static final long serialVersionUID = 1666585004016142602L;
    private String function;
    private String action;
    private String period;
    private String assettype;
    private String asseturl;
    private String assetid;
    private String assetname;
    private int play;
    /**
     * 投屏设备ID
     */
    private String deviceId;
    /**
     * 投屏设备名
     */
    private String deviceName;
    /**
     * 点播类型：
     * 1：普通点播；
     * 2；酒楼宣传片
     */
    private int vodType;



    public PrepareRequestVo() {
    }

    public String getPeriod() {
        return this.period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAssettype() {
        return this.assettype;
    }

    public void setAssettype(String assettype) {
        this.assettype = assettype;
    }

    public String getAsseturl() {
        return this.asseturl;
    }

    public void setAsseturl(String asseturl) {
        this.asseturl = asseturl;
    }

    public String getAssetid() {
        return this.assetid;
    }

    public void setAssetid(String assetid) {
        this.assetid = assetid;
    }

    public String getAssetname() {
        return this.assetname;
    }

    public void setAssetname(String assetname) {
        this.assetname = assetname;
    }

    public int getPlay() {
        return this.play;
    }

    public void setPlay(int play) {
        this.play = play;
    }

    public String getFunction() {
        return this.function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getVodType() {
        return vodType;
    }

    public void setVodType(int vodType) {
        this.vodType = vodType;
    }
}
