package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class QueryBufferBySessionIdResponseVo implements Serializable {
    private static final long serialVersionUID = 6922151567069094539L;
    private int result;
    private int bufferstatus;

    public QueryBufferBySessionIdResponseVo() {
    }

    public int getResult() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public int getBufferstatus() {
        return this.bufferstatus;
    }

    public void setBufferstatus(int bufferstatus) {
        this.bufferstatus = bufferstatus;
    }
}
