package com.savor.ads.bean;

import java.io.Serializable;

/**
 * Created by zhang.haiqiang on 2017/12/4.
 */

public class RstrSpecialty implements Serializable {

    /**
     * vid : 2681
     * name : tcyzr7Xa8i.mp4
     * chinese_name : 如果世界都是圆滚滚的1
     * period : 20171130175712
     * type : recommend
     * md5 : 96faaadeb712322142b2b23988f190a3
     * suffix : mp4
     * url : /download-file/recommend/20171130175712/tcyzr7Xa8i.mp4
     * food_id : 2
     * media_type : 2
     */

    private String vid;
    private String name;
    private String chinese_name;
    private String period;
    private String type;
    private String md5;
    private String suffix;
    private String url;
    private String food_id;
    /** 媒体类型 1视频 2图片 3其它*/
    private String media_type;
    private String media_path;

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
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

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFood_id() {
        return food_id;
    }

    public void setFood_id(String food_id) {
        this.food_id = food_id;
    }

    public String getMedia_type() {
        return media_type;
    }

    public void setMedia_type(String media_type) {
        this.media_type = media_type;
    }

    public String getMedia_path() {
        return media_path;
    }

    public void setMedia_path(String media_path) {
        this.media_path = media_path;
    }
}
