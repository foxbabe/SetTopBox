package com.savor.ads.bean;

/**
 * Created by zhang.haiqiang on 2018/1/10.
 */

public class PushCustomBean<T> {
    private String type;

    private T data;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
