package com.jar.savor.box.vo;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class ZoomResponseVo extends BaseResponse {
    private static final long serialVersionUID = 3929950430835856667L;
    private int zoom;

    public ZoomResponseVo() {
    }

    public int getZoom() {
        return this.zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }
}
