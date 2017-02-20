package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class QueryRequestVo implements Serializable {
    private static final long serialVersionUID = 8330152453864561446L;
    private String function;
    private String what;
    private String deviceId;

    public QueryRequestVo() {
    }

    public String getWhat() {
        return this.what;
    }

    public void setWhat(String what) {
        this.what = what;
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
