package com.savor.ads.utils;

import android.graphics.Bitmap;
import android.util.Pair;

import com.savor.ads.bean.BaiduAdLocalBean;
import com.savor.ads.bean.MediaLibBean;

import java.util.ArrayList;

/**
 * Created by zhanghq on 2017/3/20.
 */

public class GlobalValues <T extends MediaLibBean> {

    private static GlobalValues instance;

    public static GlobalValues getInstance() {
        if (instance == null) {
            instance = new GlobalValues();
        }
        return instance;
    }

    /** 二维码内容*/
    public static String QRCODE_CONTENT = "";
    /** 输入验证码*/
    public static String AUTH_CODE = "";

    /** 播放列表*/
    public ArrayList<T> PLAY_LIST;
    /** 请求到的百度聚屏广告集合，填充节目单时会用到*/
    public static ArrayList<BaiduAdLocalBean> ADS_PLAY_LIST;
    /** 拿到百度聚屏广告后此刻的节目order，填充节目单时会用到*/
    public static int CURRENT_MEDIA_ORDER = 0;

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

    /** 未在本地找到的百度聚屏广告KEY（md5）*/
    public static String NOT_FOUND_BAIDU_ADS_KEY;
    /**
     * 百度聚屏广告连续重复次数
     * first: 广告md5
     * second: 连续次数
     */
    public static Pair<String, Integer> CURRENT_ADS_REPEAT_PAIR;
    /**
     * 当前跳过的聚屏广告请求次数
     */
    public static int CURRENT_ADS_BLOCKED_COUNT = 0;

    /**当前投屏人的微信ID**/
    public static String CURRENT_OPEN_ID;
    /**本次投屏操作的唯一标示ID**/
    public static String CURRRNT_PROJECT_ID;
    /**本次投屏文字**/
    public static String PROJECTION_WORDS;
    /**当前投屏人的投的照片的集合**/
    public static ArrayList<String> PROJECT_IMAGES=new ArrayList<>();
}
