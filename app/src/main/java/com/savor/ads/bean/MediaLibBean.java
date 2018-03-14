package com.savor.ads.bean;

import java.io.Serializable;

/**
 * Created by bichao on 2016/12/12.
 */

public class MediaLibBean implements Serializable {
    private int id;
    //视频ID
    private String vid;
    //视频检验值
    private String md5;
    //视频文件名称
    private String name;
    //视频内容名称
    private String chinese_name;
    //视频类型
    private String type;
    //视频后缀名
    private String surfix;
    //视频时长
    private String duration;
    //期刊号
    private String period;
    //播放顺序排序
    private int order;
    private String taskId;
    private String url;
    private String area_id;

    private String mediaPath;

    private String location_id;
    private String start_date;
    private String end_date;

    /** ADMaster 曝光*/
    private String admaster_sin;
    /**下载状态 0：未下载； 1：已下载； 2：下载中*/
    private int download_state;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public String getChinese_name() {
        return chinese_name;
    }

    public void setChinese_name(String chinese_name) {
        this.chinese_name = chinese_name;
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

    public String getLocation_id() {
        return location_id;
    }

    public void setLocation_id(String location_id) {
        this.location_id = location_id;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public String getAdmaster_sin() {
        return admaster_sin;
    }

    public void setAdmaster_sin(String admaster_sin) {
        this.admaster_sin = admaster_sin;
    }

    public int getDownload_state() {
        return download_state;
    }

    public void setDownload_state(int download_state) {
        this.download_state = download_state;
    }
}

