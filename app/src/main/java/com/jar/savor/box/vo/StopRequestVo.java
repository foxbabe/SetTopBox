package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */
public class StopRequestVo implements Serializable {
    private static final long serialVersionUID = -7224605505218630259L;
    private String function;
    private int sessionid;
    private int reason;
    private String deviceId;

    public StopRequestVo() {
    }

    public int getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }

    public int getReason() {
        return this.reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
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
