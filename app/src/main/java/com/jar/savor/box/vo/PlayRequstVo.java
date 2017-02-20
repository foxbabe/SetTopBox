package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class PlayRequstVo implements Serializable {
    private static final long serialVersionUID = 4445870756166886894L;
    private String function;
    private int sessionid;
    private int rate;
    private String deviceId;

    public PlayRequstVo() {
    }

    public int getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }

    public int getRate() {
        return this.rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
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
}
