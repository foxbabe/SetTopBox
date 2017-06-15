package com.savor.ads.log;


public class RestaurantLogBean {
    /**
     * 视频UUID
     */
    private String UUid = "";
    /**
     * 盒子ID
     */
    private String boxId = "";
    /**
     * 日志生成的小时
     */
    private String logHour;
    /**
     * 酒楼ID
     */
    private String hotel_id;
    /**
     * 包间ID
     */
    private String room_id;
    /**
     * 日志时间
     */
    private String time = "";
    /**
     * 动作
     */
    private String action = "";
    /**
     * 视频类型
     */
    private String type = "";
    /**
     * 手机ID
     */
    private String mobile_id;
    /**
     * APK版本号
     */
    private String apk_version;
    /**
     * 预留参数一
     */
    private String custom1;
    /**
     * 预留参数二
     */
    private String custom2;
    /**
     * 预留参数三
     */
    private String custom3;
    /**
     * 媒体时长
     */
    private String duration;
    /**
     * 幻灯片间隔
     */
    private int pptInterval;
    /**
     * 幻灯片图片数
     */
    private int pptSize;

    private String innerType;

    public RestaurantLogBean() {
        time = System.currentTimeMillis() + "";
    }

    public String getHotel_id() {
        return hotel_id == null ? "" : hotel_id;
    }

    public void setHotel_id(String hotel_id) {
        this.hotel_id = hotel_id;
    }

    public String getRoom_id() {
        return room_id == null ? "" : room_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }

    public String getUUid() {
        return UUid == null ? "" : UUid;
    }

    public void setUUid(String UUid) {
        this.UUid = UUid;
    }

    public String getTime() {
        return time == null ? "" : time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getAction() {
        return action == null ? "" : action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getType() {
        return type == null ? "" : type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMobile_id() {
        return mobile_id == null ? "" : mobile_id;
    }

    public void setMobile_id(String mobile_id) {
        this.mobile_id = mobile_id;
    }

    public String getApk_version() {
        return apk_version == null ? "" : apk_version;
    }

    public void setApk_version(String apk_version) {
        this.apk_version = apk_version;
    }

    public String getCustom1() {
        return custom1 == null ? "" : custom1;
    }

    public void setCustom1(String custom1) {
        this.custom1 = custom1;
    }

    public String getBoxId() {
        return boxId == null ? "" : boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    public String getLogHour() {
        return logHour == null ? "" : logHour;
    }

    public void setLogHour(String logHour) {
        this.logHour = logHour;
    }

    public String getCustom2() {
        return custom2;
    }

    public void setCustom2(String custom2) {
        this.custom2 = custom2;
    }

    public String getCustom3() {
        return custom3;
    }

    public void setCustom3(String custom3) {
        this.custom3 = custom3;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getPptInterval() {
        return pptInterval;
    }

    public void setPptInterval(int pptInterval) {
        this.pptInterval = pptInterval;
    }

    public int getPptSize() {
        return pptSize;
    }

    public void setPptSize(int pptSize) {
        this.pptSize = pptSize;
    }

    public String getInnerType() {
        return innerType;
    }

    public void setInnerType(String innerType) {
        this.innerType = innerType;
    }
}
