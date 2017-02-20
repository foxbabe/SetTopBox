package com.savor.ads.bean;

import java.io.Serializable;

/**
 * Created by bichao on 2016/12/12.
 */

public class MediaLibBean implements Serializable {

    //视频ID
    private String vid;
    //视频检验值
    private String md5;
    //视频名称
    private String name;
    //视频类型
    private String type;
    //视频后缀名
    private String surfix;
    //视频时长
    private String duration;

    private String property;
    //期刊号
    private String period;
    //播放顺序排序
    private int order;
    private String taskId;
    private String url;
    private String area_id;


    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSurfix() {
        return surfix;
    }

    public void setSurfix(String surfix) {
        this.surfix = surfix;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getArea_id() {
        return area_id;
    }

    public void setArea_id(String area_id) {
        this.area_id = area_id;
    }
}

