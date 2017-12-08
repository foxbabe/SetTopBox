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
    /** 他人正在投屏，供抢投方判断弹窗*/
    public static final int SERVER_RESPONSE_CODE_ANOTHER_PROJECT = 4;
    /** 批量点播但资源未全部找到*/
    public static final int SERVER_RESPONSE_CODE_SPECIALTY_INCOMPLETE = 5;


    /**
     * 显示二维码指令
     */
    public static final String NETTY_SHOW_QRCODE_COMMAND = "call-tdc";

    /**
     * 展示特色菜指令
     */
    public static final String NETTY_SHOW_SPECIALTY_COMMAND = "call-specialty";

    /**
     * 展示欢迎词指令
     */
    public static final String NETTY_SHOW_WELCOME_COMMAND = "call-word";

    /**
     * 播放宣传片指令
     */
    public static final String NETTY_SHOW_ADV_COMMAND = "call-adv";

    /**
     * 投屏类型:图片
     */
    public static final String PROJECT_TYPE_PICTURE = "pic";
    /**
     * 投屏类型:餐厅端，幻灯片
     */
    public static final String PROJECT_TYPE_RSTR_PPT = "rstr_ppt";
    /**
     * 投屏类型:餐厅端，视频幻灯片
     */
    public static final String PROJECT_TYPE_RSTR_VIDEO_PPT = "rstr_video_ppt";
    /**
     * 投屏类型:餐厅端，特色菜
     */
    public static final String PROJECT_TYPE_RSTR_SPECIALTY = "rstr_video_specialty";
    /**
     * 投屏类型:餐厅端，欢迎词
     */
    public static final String PROJECT_TYPE_RSTR_GREETING = "rstr_video_greeting";
    /**
     * 投屏类型:餐厅端，宣传片
     */
    public static final String PROJECT_TYPE_RSTR_ADV = "rstr_video_adv";
    /**
     * 投屏类型:视频
     */
    public static final String PROJECT_TYPE_VIDEO = "video";
    /**
     * 点播
     */
    public static final String PROJECT_TYPE_VIDEO_VOD = "vod";

    public static final String CONFIG_TXT = "config.txt";

    public static final String USB_FILE_PATH = "redian";

    /** 广告下载完成广播Action*/
    public static final String ADS_DOWNLOAD_COMPLETE_ACCTION = "com.savor.ads.ads_download_complete";


    public static final int KEY_DOWN_LAG = 2000;

    public static final String SSDP_CONTENT_TYPE = "box";

    /** 默认电视切换时间*/
    public static final int DEFAULT_SWITCH_TIME = 30;
    /** 默认轮播音量*/
    public static final int DEFAULT_ADS_VOLUME = 20;
    /** 默认投屏音量*/
    public static final int DEFAULT_PROJECT_VOLUME = 65;
    /** 默认点播音量*/
    public static final int DEFAULT_VOD_VOLUME = 65;
    /** 默认电视音量*/
    public static final int DEFAULT_TV_VOLUME = 100;
    /**节目单-节目*/
    public static final String PRO = "pro";
    /**节目单-宣传单*/
    public static final String ADV = "adv";
    /**节目单-广告*/
    public static final String ADS = "ads";
    /**特色菜*/
    public static final String RECOMMEND = "recommend";

    /** 虚拟小平台地址*/
    public static final String VIRTUAL_SP_HOST = "v-small.littlehotspot.com";

    /**外置SD卡至少保留的可用空间*/
    public static final long EXTSD_LEAST_AVAILABLE_SPACE = 1024 * 1024 * 1024;
}
