package com.savor.ads.utils;

import android.graphics.Bitmap;

import com.savor.ads.bean.PlayListBean;

import java.util.ArrayList;

/**
 * Created by zhanghq on 2016/12/8.
 */

public class ConstantValues {
    /**
     * 显示二维码指令
     */
    public static final String NETTY_SHOW_QRCODE_COMMAND = "call-tdc";

    /**
     * 投屏类型:图片
     */
    public static final String PROJECT_TYPE_PICTURE = "pic";
    /**
     * 投屏类型:视频
     */
    public static final String PROJECT_TYPE_VIDEO = "video";
    /**
     * 投屏类型:PDF
     */
    public static final String PROJECT_TYPE_PDF = "pdf";
    /**
     * 投屏类型:文件
     */
    public static final String PROJECT_TYPE_FILE = "file";
    /**
     * 视频投屏:点播
     */
    public static final String PROJECT_TYPE_VIDEO_VOD = "vod";
    /**
     * 视频投屏:投屏
     */
    public static final String PROJECT_TYPE_VIDEO_2SCREEN = "2screen";

    public static final String CONFIG_TXT = "config.txt";

    public static final String USB_FILE_PATH = "redian";

    /** 广告下载完成广播Action*/
    public static final String ADS_DOWNLOAD_COMPLETE_ACCTION = "com.savor.ads.ads_download_complete";

    /** 二维码内容*/
    public static String QRCODE_CONTENT = "";

    /** 播放列表*/
    public static ArrayList<PlayListBean> PLAY_LIST;
    /** 当前投屏设备ID*/
    public static String CURRENT_PROJECT_DEVICE_ID;

//    /**
//     * 测试环境手机端APP下载页面
//     */
//    public static final String APP_DOWN_LINK = "http://devp.savorx.cn/d";
    /**
     * 正式环境手机端APP下载页面
     */
    public static final String APP_DOWN_LINK = "http://rerdian.com/d";

    /** 当前投屏图片*/
    public volatile static Bitmap PROJECT_BITMAP;
    /** 当前投屏图片ID*/
    public volatile static String PROJECT_IMAGE_ID;
}
