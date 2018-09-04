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
    /**下载状态 0：未下载； 1：已下载； 2：下载中*/
    private int download_state;
    /**聚屏类型：1.百度**/
    private String tpmedia_id;
    /**百度返回md5值**/
    private String tp_md5;
    /**oss资源路径**/
    private String oss_path;
    /**李召返回，暂未使用**/
    private String oss_etag;
    /**0：不显示  1：显示**/
    private int is_sapp_qrcode;

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

    public int getDownload_state() {
        return download_state;
    }

    public void setDownload_state(int download_state) {
        this.download_state = download_state;
    }

    public String getTpmedia_id() {
        return tpmedia_id;
    }

    public void setTpmedia_id(String tpmedia_id) {
        this.tpmedia_id = tpmedia_id;
    }

    public String getTp_md5() {
        return tp_md5;
    }

    public void setTp_md5(String tp_md5) {
        this.tp_md5 = tp_md5;
    }

    public String getOss_path() {
        return oss_path;
    }

    public void setOss_path(String oss_path) {
        this.oss_path = oss_path;
    }

    public String getOss_etag() {
        return oss_etag;
    }

    public void setOss_etag(String oss_etag) {
        this.oss_etag = oss_etag;
    }

    public int getIs_sapp_qrcode() {
        return is_sapp_qrcode;
    }

    public void setIs_sapp_qrcode(int is_sapp_qrcode) {
        this.is_sapp_qrcode = is_sapp_qrcode;
    }
}
