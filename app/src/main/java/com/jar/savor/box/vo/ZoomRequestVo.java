package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class ZoomRequestVo implements Serializable {
    private static final long serialVersionUID = 7701559145627701323L;
    private String function;
    private int sessionid;
    private int absolutezoom;
    private int relativezoom;

    public ZoomRequestVo() {
    }

    public int getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }

    public int getAbsolutezoom() {
        return this.absolutezoom;
    }

    public void setAbsolutezoom(int absolutezoom) {
        this.absolutezoom = absolutezoom;
    }

    public int getRelativezoom() {
        return this.relativezoom;
    }

    public void setRelativezoom(int relativezoom) {
        this.relativezoom = relativezoom;
    }

    public String getFunction() {
        return this.function;
    }

    public void setFunction(String function) {
        this.function = function;
    }
}
