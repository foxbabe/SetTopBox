package com.savor.ads.bean;

import java.io.Serializable;

/**
 * 单机版盒子使用
 * Created by bichao on 2017/12/12.
 */

public class BoxBean implements Serializable{

    /**盒子名称*/
    private String box_name;
    /**盒子ID*/
    private String box_id;
    /**盒子MAC*/
    private String box_mac;
    /**切换时间*/
    private int switch_time;
    /**电视声音*/
    private int volume;
    /**包间ID*/
    private String room_id;

    public String getBox_name() {
        return box_name;
    }

    public void setBox_name(String box_name) {
        this.box_name = box_name;
    }

    public String getBox_id() {
        return box_id;
    }

    public void setBox_id(String box_id) {
        this.box_id = box_id;
    }

    public String getBox_mac() {
        return box_mac;
    }

    public void setBox_mac(String box_mac) {
        this.box_mac = box_mac;
    }

    public int getSwitch_time() {
        return switch_time;
    }

    public void setSwitch_time(int switch_time) {
        this.switch_time = switch_time;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public String getRoom_id() {
        return room_id;
    }

    public void setRoom_id(String room_id) {
        this.room_id = room_id;
    }
}
