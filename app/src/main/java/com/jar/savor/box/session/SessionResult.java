package com.jar.savor.box.session;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class SessionResult {
    private int status;
    private String msg;

    public SessionResult() {
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
