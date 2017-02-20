package com.savor.ads.bean;

/**
 * Created by zhanghq on 2017/2/7.
 */

public class BoxInitBean {
    /**电视切换时间*/
    private int switch_time;
    /**音量大小*/
    private int volume;
    /**酒楼ID*/
    private String hotel_id;
    /**包间ID*/
    private String room_id;
    /**酒楼名称*/
    private String hotel_name;
    /**包间名称*/
    private String room_name;
    /**地区ID*/
    private String areaId;
    /**logo地址*/
    private String logo_url;
    /**logo检验MD5*/
    private String logo_md5;
    /**OSS桶ID*/
    private String ossBucketName;
    /**盒子名称*/
    private String box_name;

    public void setSwitch_time(int switch_time) {
        this.switch_time = switch_time;
    }

    public int getSwitch_time() {
        return switch_time;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public void setHotel_id(String hotel_id) {
        this.hotel_id = hotel_id;
    }

    public String getHotel_id() {
        return hotel_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public String getRoom_id() {
        return room_id;
    }

    public void setHotel_name(String hotel_name) {
        this.hotel_name = hotel_name;
    }

    public String getHotel_name() {
        return hotel_name;
    }

    public void setRoom_name(String room_name) {
        this.room_name = room_name;
    }

    public String getRoom_name() {
        return room_name;
    }

    public void setAreaId(String areaId) {
        this.areaId = areaId;
    }

    public String getAreaId() {
        return areaId;
    }

    public void setLogo_url(String logo_url) {
        this.logo_url = logo_url;
    }

    public String getLogo_url() {
        return logo_url;
    }

    public void setLogo_md5(String logo_md5) {
        this.logo_md5 = logo_md5;
    }

    public String getLogo_md5() {
        return logo_md5;
    }

    public void setOssBucketName(String ossBucketName) {
        this.ossBucketName = ossBucketName;
    }

    public String getOssBucketName() {
        return ossBucketName;
    }

    public String getBox_name() {
        return box_name;
    }

    public void setBox_name(String box_name) {
        this.box_name = box_name;
    }
}
