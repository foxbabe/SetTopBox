package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class SeekRequestVo implements Serializable {
    private static final long serialVersionUID = -5077821202602243217L;
    private String function;
    private int sessionid;
    private int absolutepos;
    private int relativepos;
    private String deviceId;

    public SeekRequestVo() {
    }

    public int getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }

    public int getAbsolutepos() {
        return this.absolutepos;
    }

    public void setAbsolutepos(int absolutepos) {
        this.absolutepos = absolutepos;
    }

    public int getRelativepos() {
        return this.relativepos;
    }

    public void setRelativepos(int relativepos) {
        this.relativepos = relativepos;
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
