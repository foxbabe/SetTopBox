package com.jar.savor.box.vo;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class HeartBeatResponeseVo extends BaseResponse {
    private static final long serialVersionUID = 4335752996695755914L;
    private int currcycletime;

    public HeartBeatResponeseVo() {
    }

    public int getCurrcycletime() {
        return this.currcycletime;
    }

    public void setCurrcycletime(int currcycletime) {
        this.currcycletime = currcycletime;
    }
}
