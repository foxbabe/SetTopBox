package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class HeartBeatRequestVo implements Serializable {
    private static final long serialVersionUID = -2260143909097633299L;
    private String function;
    private int sessionid;
    private int cycletime;

    public HeartBeatRequestVo() {
    }

    public String getFunction() {
        return this.function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public int getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }

    public int getCycletime() {
        return this.cycletime;
    }

    public void setCycletime(int cycletime) {
        this.cycletime = cycletime;
    }
}
