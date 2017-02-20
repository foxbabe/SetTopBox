package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class RotateRequestVo implements Serializable {
    private static final long serialVersionUID = -2141400868147102506L;
    private String function;
    private int sessionid;
    private int rotatevalue;
    private String deviceId;

    public RotateRequestVo() {
    }

    public int getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }

    public int getRotatevalue() {
        return this.rotatevalue;
    }

    public void setRotatevalue(int rotatevalue) {
        this.rotatevalue = rotatevalue;
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
