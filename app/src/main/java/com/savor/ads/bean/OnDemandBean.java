package com.savor.ads.bean;

import java.io.Serializable;

/**
 * Created by Administrator on 2016/12/13.
 */

public class OnDemandBean implements Serializable{
    private String Catagory;
    //视频名称
    private String title;
    private String taskId;
    private String picTitle;
    private String vodId;
    private String media_type;
    //播放时长
    private String time;
    private String Desc;
    //视频地址
    private String videoUrl;
    private String picUrl;
    //格式
    private String surfix;

    private String picUrlMd5;
    private String md5;
    private String lengthClassify;
    private String areaId;
    private String period;
    public String getCatagory() {
        return Catagory;
    }

    public void setCatagory(String catagory) {
        Catagory = catagory;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPicTitle() {
        return picTitle;
    }

    public void setPicTitle(String picTitle) {
        this.picTitle = picTitle;
    }

    public String getVodId() {
        return vodId;
    }

    public void setVodId(String vodId) {
        this.vodId = vodId;
    }

    public String getMedia_type() {
        return media_type;
    }

    public void setMedia_type(String media_type) {
        this.media_type = media_type;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDesc() {
        return Desc;
    }

    public void setDesc(String desc) {
        Desc = desc;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getPicUrl() {
        picUrl =picUrl.substring(picUrl.lastIndexOf("/") + 1,picUrl.length());
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getSurfix() {
        return surfix;
    }

    public void setSurfix(String surfix) {
        this.surfix = surfix;
    }

    public String getPicUrlMd5() {
        return picUrlMd5;
    }

    public void setPicUrlMd5(String picUrlMd5) {
        this.picUrlMd5 = picUrlMd5;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getLengthClassify() {
        return lengthClassify;
    }

    public void setLengthClassify(String lengthClassify) {
        this.lengthClassify = lengthClassify;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
