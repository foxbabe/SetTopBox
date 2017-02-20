package com.jar.savor.box.vo;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class PrepareResponseVo extends BaseResponse {
    private static final long serialVersionUID = 7154218806343160626L;
    private int sessionid;

    public PrepareResponseVo() {
    }

    public int getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }
}
