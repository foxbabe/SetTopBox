package com.savor.ads.bean;

import com.savor.tvlibrary.AtvChannel;

import java.util.List;

/**
 * Created by zhanghq on 2016/12/27.
 */

public class AtvProgramRequestBean {

    private List<AtvChannel> data;

    public List<AtvChannel> getData() {
        return data;
    }

    public void setData(List<AtvChannel> data) {
        this.data = data;
    }
}
