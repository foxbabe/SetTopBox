package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */
public class VolumeRequestVo implements Serializable {
    private static final long serialVersionUID = 8485444022237974254L;
    private String function;
    private String deviceId;
    /** 音量操作类型
     * 1：静音
     * 2：取消静音
     * 3：音量减
     * 4：音量加*/
    private int action;

    public VolumeRequestVo() {
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

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }
}
