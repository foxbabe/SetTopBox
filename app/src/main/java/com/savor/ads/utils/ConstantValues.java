package com.savor.ads.utils;

/**
 * Created by zhanghq on 2016/12/8.
 */

public class ConstantValues {
    /** 手机端操作响应码*/
    /** 失败*/
    public static final int SERVER_RESPONSE_CODE_FAILED = -1;
    /** 成功*/
    public static final int SERVER_RESPONSE_CODE_SUCCESS = 0;
    /** 视频播放完毕*/
    public static final int SERVER_RESPONSE_CODE_VIDEO_COMPLETE = 1;
    /** 大小图不匹配*/
    public static final int SERVER_RESPONSE_CODE_IMAGE_ID_CHECK_FAILED = 2;
    /** 投屏ID不匹配*/
    public static final int SERVER_RESPONSE_CODE_PROJECT_ID_CHECK_FAILED = 3;


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


    /** 下载的启动图*/
    public static final String LOGO_FILE_PATH = "/Pictures/logo.jpg";
    /** 下载的投屏加载图*/
    public static final String LOADING_IMG_FILE_PATH = "/Pictures/loading.jpg";
}
