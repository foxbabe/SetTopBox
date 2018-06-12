package com.savor.ads.utils;

import com.savor.ads.BuildConfig;

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
     * 播放特色菜和宣传片指令
     */
    public static final String NETTY_SHOW_WELCOME_THEN_SPECIALTY_COMMAND = "call-word-then-specialty";

    /**
     * 停止投屏
     */
    public static final String NETTY_STOP_PROJECTION_COMMAND = "call-stop-projection";

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
    public static final String PROJECT_TYPE_RSTR_SPECIALTY = "rstr_specialty";
    /**
     * 投屏类型:餐厅端，欢迎词
     */
    public static final String PROJECT_TYPE_RSTR_GREETING = "rstr_greeting";
    /**
     * 投屏类型:餐厅端，欢迎词、特色菜连播
     */
    public static final String PROJECT_TYPE_RSTR_GREETING_THEN_SPECIALTY = "rstr_greeting_then_specialty";
    /**
     * 投屏类型:餐厅端，宣传片
     */
    public static final String PROJECT_TYPE_RSTR_ADV = "rstr_adv";
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

    /**U盘安装酒楼文件夹目录*/
    public static final String USB_FILE_HOTEL_PATH = "savor";
    public static final String USB_FILE_HOTEL_MEDIA_PATH = "media";
    public static final String USB_FILE_HOTEL_MULTICAST_PATH = "multicast";
    /**U盘安装酒楼配置文件*/
    public static final String USB_FILE_HOTEL_UPDATE_CFG = "update.cfg";
    /**U盘安装酒楼配置文件-获取电视列表*/
    public static final String USB_FILE_HOTEL_GET_CHANNEL = "get_channel";
    /**U盘安装酒楼配置文件-上传电视列表*/
    public static final String USB_FILE_HOTEL_SET_CHANNEL = "set_channel";
    /**U盘安装酒楼配置文件-拉取日志文件*/
    public static final String USB_FILE_HOTEL_GET_LOG = "get_log";
    /**U盘安装酒楼配置文件-拉取备份日志文件*/
    public static final String USB_FILE_HOTEL_GET_LOGED = "get_loged";
    /**U盘安装酒楼配置文件-更新视频*/
    public static final String USB_FILE_HOTEL_UPDATE_MEIDA = "update_media";
    /**U盘安装酒楼配置文件-更新版本*/
    public static final String USB_FILE_HOTEL_UPDATE_APK = "update_apk";
    /**U盘安装酒楼配置文件-更新LOGO*/
    public static final String USB_FILE_HOTEL_UPDATE_LOGO = "update_logo";
    /**U盘安装酒楼配置文件-更新视频json文件*/
    public static final String USB_FILE_HOTEL_UPDATE_JSON = "play_list.json";
    /**U盘安装酒楼配置文件-更新宣传片目录*/
    public static final String USB_FILE_HOTEL_UPDATE_ADV= "adv";
    /**U盘安装酒楼配置文件-日志提取目录*/
    public static final String USB_FILE_LOG_PATH = "log";
    public static final String USB_FILE_LOGED_PATH = "loged";
    /**U盘安装-频道信息原数据*/
    public static final String USB_FILE_CHANNEL_RAW_DATA = "channel_raw";
    /**U盘安装-频道信息编辑数据*/
    public static final String USB_FILE_CHANNEL_EDIT_DATA = "channel.csv";
    /**U盘安装-酒楼列表文件*/
    public static final String USB_FILE_HOTEL_LIST_JSON = "hotel.json";
    /**U盘安装酒楼配置文件-单机日志标志*/
    public static final String STANDALONE="standalone";

    /** 更新播放列表Action*/
    public static final String UPDATE_PLAYLIST_ACTION = "com.savor.ads.action_update_playlist";


    public static final int KEY_DOWN_LAG = 2000;

    public static final String SSDP_CONTENT_TYPE = "box";

    /** 默认电视切换时间*/
    public static final int DEFAULT_SWITCH_TIME = 999;
    /** 默认轮播音量*/
    public static final int DEFAULT_ADS_VOLUME = 60;
    /** 默认投屏音量*/
    public static final int DEFAULT_PROJECT_VOLUME = 100;
    /** 默认点播音量*/
    public static final int DEFAULT_VOD_VOLUME = 90;
    /** 默认电视音量*/
    public static final int DEFAULT_TV_VOLUME = 100;
    /**节目单-节目*/
    public static final String PRO = "pro";
    /**节目单-宣传单*/
    public static final String ADV = "adv";
    /**节目单-广告*/
    public static final String ADS = "ads";
    /**节目单-RTB广告*/
    public static final String RTB_ADS = "rtbads";
    /**节目单-poly广告*/
    public static final String POLY_ADS = "poly";
    /**特色菜*/
    public static final String RECOMMEND = "recommend";

    /** 虚拟小平台地址*/
    public static final String VIRTUAL_SP_HOST = "v-small.littlehotspot.com";

    /**外置SD卡至少保留的可用空间*/
    public static final long EXTSD_LEAST_AVAILABLE_SPACE = 1024 * 1024 * 1024;


    /**
     * sdkconfig.xml配置文件服务器存放地址,如果为空的话，默认去加载本地assets目录
     */
    public static final String CONFIG_URL = BuildConfig.BASE_URL+"/Public/admaster/admaster_sdkconfig.xml";

    public static final String APK_INSTALLED_PATH = "/system/priv-app/savormedia/";

    /**节目数据文件位置*/
    public static final String PRO_DATA_PATH = "/sdcard/server_data/pro_data";
    /**宣传片数据文件位置*/
    public static final String ADV_DATA_PATH = "/sdcard/server_data/adv_data";
    /**广告数据文件位置*/
    public static final String ADS_DATA_PATH = "/sdcard/server_data/ads_data";

    /**百度聚屏APP ID*/
    public static final String BAIDU_ADS_APP_ID = "ce124b3c";
    /**百度聚屏代码位ID*/
    public static final String BAIDU_ADSLOT_ID = "5592208";
    /**百度聚屏最大连续重复次数*/
    public static final int MAX_BAIDU_ADS_REPEAT_COUNT = 5;
    /**百度聚屏连续重复达上限后，阻塞接下来的请求的次数*/
    public static final int BAIDU_ADS_BLOCK_COUNT = 10;
    /**实体小平台**/
    public static final String ENTITY="entity";
    /**虚拟小平台**/
    public static final String VIRTUAL="virtual";

    public static final String APK_DOWNLOAD_FILENAME =  "updateapksamples.apk";
    public static final String ROM_DOWNLOAD_FILENAME =  "update_signed.zip";
    /**推送类型定义,1:RTB推送;2:移动网络4g投屏**/
    public static final int PUSH_TYPE_RTB_ADS = 1;
    public static final int PUSH_TYPE_4G_PROJECTION = 2;
}
