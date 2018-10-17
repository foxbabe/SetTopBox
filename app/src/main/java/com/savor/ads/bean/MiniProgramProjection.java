package com.savor.ads.bean;

import java.io.Serializable;

public class MiniProgramProjection implements Serializable{
    /**小程序投屏动作1:呼玛  2：投屏：3 退出投屏 4:投屏多张图片（包括单张）**/
    private int action;
    /**小程序呼出的码**/
    private int code;
    /**微信标示openid**/
    private String openid;
    /**投屏图片url**/
    private String url;
    /**投屏图片名称**/
    private String filename;
    /**投多张图片时总张数**/
    private int img_nums;
    /**投屏添加文字**/
    private String forscreen_char;
    /**多张投屏序号**/
    private int order;
    /**操作ID**/
    private String forscreen_id;
    /***********************微信小程序游戏中用到的字段*******************************/
    /**游戏活动ID*/
    private long activity_id;
    /**微信用户头像**/
    private String avatarurl;
    /**游戏邀请码地址**/
    private String gamecode;
    /**投图片ID*/
    private String img_id;

    /**投视频ID**/
    private String video_id;
    /**动作操作时间**/
    private String order_time;
    /**微信头像*/
    private String avatarUrl;
    /**微信昵称*/
    private String nickName;

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getImg_nums() {
        return img_nums;
    }

    public void setImg_nums(int img_nums) {
        this.img_nums = img_nums;
    }

    public String getForscreen_char() {
        return forscreen_char;
    }

    public void setForscreen_char(String forscreen_char) {
        this.forscreen_char = forscreen_char;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getForscreen_id() {
        return forscreen_id;
    }

    public void setForscreen_id(String forscreen_id) {
        this.forscreen_id = forscreen_id;
    }

    public long getActivity_id() {
        return activity_id;
    }

    public void setActivity_id(long activity_id) {
        this.activity_id = activity_id;
    }

    public String getAvatarurl() {
        return avatarurl;
    }

    public void setAvatarurl(String avatarurl) {
        this.avatarurl = avatarurl;
    }

    public String getGamecode() {
        return gamecode;
    }

    public void setGamecode(String gamecode) {
        this.gamecode = gamecode;
    }

    public String getImg_id() {
        return img_id;
    }

    public void setImg_id(String img_id) {
        this.img_id = img_id;
    }

    public String getVideo_id() {
        return video_id;
    }

    public void setVideo_id(String video_id) {
        this.video_id = video_id;
    }

    public String getOrder_time() {
        return order_time;
    }

    public void setOrder_time(String order_time) {
        this.order_time = order_time;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}
