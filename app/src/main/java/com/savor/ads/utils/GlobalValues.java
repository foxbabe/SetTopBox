package com.savor.ads.utils;

import android.graphics.Bitmap;

import com.savor.ads.bean.MediaLibBean;

import java.util.ArrayList;

/**
 * Created by zhanghq on 2017/3/20.
 */

public class GlobalValues {

    /** 二维码内容*/
    public static String QRCODE_CONTENT = "";
    /** 输入验证码*/
    public static String AUTH_CODE = "";

    /** 播放列表*/
    public static ArrayList<MediaLibBean> PLAY_LIST;

    /** 当前投屏设备ID*/
    public volatile static String CURRENT_PROJECT_DEVICE_ID;
    /** 当前投屏设备IP*/
    public volatile static String CURRENT_PROJECT_DEVICE_IP;
    /** 上次投屏设备ID*/
    public volatile static String LAST_PROJECT_DEVICE_ID;
    /** 当前投屏设备名称*/
    public volatile static String CURRENT_PROJECT_DEVICE_NAME;
    /** 当前投屏图片*/
    public volatile static Bitmap CURRENT_PROJECT_BITMAP;
    /** 当前投屏图片ID*/
    public volatile static String CURRENT_PROJECT_IMAGE_ID;
    /** 当前投屏动作ID*/
    public volatile static String CURRENT_PROJECT_ID;
    /** 上次投屏ID*/
    public volatile static String LAST_PROJECT_ID;
    /** 是否是抽奖*/
    public volatile static boolean IS_LOTTERY;
    /** 是否是餐厅端投屏*/
    public volatile static boolean IS_RSTR_PROJECTION;

    /** 标识盒子是否正在忙碌中，，忙碌中则不处理投屏类请求*/
    public static volatile boolean IS_BOX_BUSY = false;

    /** 标识友盟推送所需SO拷贝是否成功*/
    public static boolean IS_UPUSH_SO_COPY_SUCCESS = false;
    /** 标识友盟推送注册是否成功*/
    public static boolean IS_UPUSH_REGISTER_SUCCESS = false;
}
