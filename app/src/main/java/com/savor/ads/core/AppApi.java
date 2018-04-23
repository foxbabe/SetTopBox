package com.savor.ads.core;

import android.content.Context;
import android.text.TextUtils;

import com.savor.ads.BuildConfig;
import com.savor.ads.bean.AtvProgramInfo;
import com.savor.ads.bean.MediaDownloadBean;
import com.savor.ads.bean.DownloadDetailRequestBean;
import com.savor.ads.bean.PlaylistDetailRequestBean;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.tvlibrary.AtvChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import tianshu.ui.api.TsUiApiV20171122;

public class AppApi {

    /**
     * 小平台地址（默认值是一个假数据，获取到真实的小平台地址后会被重置）
     */
    public static String SP_BASE_URL = "http://192.168.1.2/";

    private static String PHONE_BASE_URL = "http://192.168.0.1:8080/";

//    /**
//     * 云平台测试环境
//     **/
//    public static final String BASE_URL = "http://devp.testapi.rerdian.com/";
//    /**
//     * 云平台正式环境
//     **/
//    public static final String BASE_URL = "https://mobile.rerdian.com/";

    public static void resetSmallPlatformInterface(Context context) {
        ServerInfo serverInfo = Session.get(context).getServerInfo();
        if (serverInfo != null) {
            for(Action action:API_URLS.keySet()){
                if (action.name().startsWith("SP_")){
                    String url = API_URLS.get(action);
                    url = url.replace(SP_BASE_URL,serverInfo.getDownloadUrl());
                    API_URLS.put(action,url);
                }
            }
            SP_BASE_URL = serverInfo.getDownloadUrl();
        }
    }

    public static void resetPhoneInterface(String newIP) {
        for (Action action : API_URLS.keySet()) {
            if (action.name().startsWith("PH_")) {
                String url = API_URLS.get(action);
                url = url.replace(PHONE_BASE_URL, "http://"+newIP+":8080/");
                API_URLS.put(action, url);
            }
        }
        PHONE_BASE_URL = "http://"+newIP+":8080/";
    }

    public static final String APK_DOWNLOAD_FILENAME =  "updateapksamples.apk";
    public static final String ROM_DOWNLOAD_FILENAME =  "update_signed.zip";
    /**
     * Action-自定义行为 注意：自定义后缀必须为以下结束 _FORM:该请求是Form表单请求方式 _JSON:该请求是Json字符串
     * _XML:该请求是XML请求描述文件
     * CP_前缀标识云平台接口；SP_前缀标识小平台接口；PH_前缀标识移动端接口
     */
    public static enum Action {
        SP_GET_PROGRAM_DATA_FROM_JSON,
        SP_GET_ADV_DATA_FROM_JSON,
        SP_GET_ADS_DATA_FROM_JSON,
        SP_GET_ON_DEMAND_DATA_FROM_JSON,
        SP_GET_TV_MATCH_DATA_FROM_JSON,
        SP_GET_TV_MATCH_DATA_FROM_GIEC_JSON,
        SP_GET_UPGRADE_INFO_JSON,
        SP_GET_LOGO_DOWN,
        SP_GET_LOADING_IMG_DOWN,
        SP_GET_UPGRADEDOWN,
        CP_GET_HEARTBEAT_PLAIN,
        SP_POST_UPLOAD_PROGRAM_JSON,
        SP_POST_UPLOAD_PROGRAM_GIEC_JSON,
        CP_GET_SP_IP_JSON,
        SP_GET_BOX_INIT_JSON,
        CP_GET_PRIZE_JSON,
        CP_REPORT_LOTTERY_JSON,
        PH_NOTIFY_STOP_JSON,
        SP_GET_SPECIALTY_JSON,
        CP_GET_ADMASTER_CONFIG_JSON,
        CP_POST_DEVICE_TOKEN_JSON,
        SP_GET_RTB_ADS_JSON,
        SP_GET_POLY_ADS_JSON,
        SP_POST_NETSTAT_JSON,
        CP_POST_PLAY_LIST_JSON,
        CP_POST_DOWNLOAD_LIST_JSON,
        CP_POST_SDCARD_STATE_JSON,

        AD_BAIDU_ADS,
    }


    /**
     * URL集合
     */
    public static HashMap<Action, String> API_URLS = new HashMap<Action, String>() {
        private static final long serialVersionUID = -8469661978245513712L;

        {
            put(Action.SP_GET_PROGRAM_DATA_FROM_JSON,SP_BASE_URL+"small/api/download/vod/config/v2");
            put(Action.SP_GET_ADV_DATA_FROM_JSON,SP_BASE_URL+"small/api/download/adv/config");
            put(Action.SP_GET_ADS_DATA_FROM_JSON,SP_BASE_URL+"small/api/download/ads/config");
            put(Action.SP_GET_ON_DEMAND_DATA_FROM_JSON,SP_BASE_URL+"small/api/download/demand/config");
            put(Action.SP_GET_TV_MATCH_DATA_FROM_JSON,SP_BASE_URL+"small/tvList/api/stb/tv_getCommands");
            put(Action.SP_GET_TV_MATCH_DATA_FROM_GIEC_JSON,SP_BASE_URL+"small/tvListNew/api/stb/tv_getCommands");
            put(Action.SP_GET_UPGRADE_INFO_JSON,SP_BASE_URL+"small/api/download/apk/config");
            put(Action.CP_GET_HEARTBEAT_PLAIN, BuildConfig.BASE_URL + "Heartbeat/Report/index");
            put(Action.SP_POST_UPLOAD_PROGRAM_JSON, SP_BASE_URL + "small/tvList/api/stb/tv_commands");
            put(Action.SP_POST_UPLOAD_PROGRAM_GIEC_JSON, SP_BASE_URL + "small/tvListNew/api/stb/tv_commands");
            put(Action.CP_GET_SP_IP_JSON, BuildConfig.BASE_URL + "basedata/ipinfo/getIp");
            put(Action.SP_GET_BOX_INIT_JSON, SP_BASE_URL + "small/api/download/init");
            put(Action.CP_GET_PRIZE_JSON, BuildConfig.BASE_URL + "Award/Award/getAwardInfo");
            put(Action.CP_REPORT_LOTTERY_JSON, BuildConfig.BASE_URL + "Award/Award/recordAwardLog");
            put(Action.PH_NOTIFY_STOP_JSON, PHONE_BASE_URL + "stopProjection");
            put(Action.SP_GET_SPECIALTY_JSON, SP_BASE_URL + "small/api/download/recommend/config");
            put(Action.CP_GET_ADMASTER_CONFIG_JSON,BuildConfig.BASE_URL + "Box/Admaster/getConfFile");
            put(Action.CP_POST_DEVICE_TOKEN_JSON, BuildConfig.BASE_URL + "Basedata/Box/reportDeviceToken");
            put(Action.SP_GET_RTB_ADS_JSON, SP_BASE_URL + "small/api/download/rtbads/config");
            put(Action.SP_GET_POLY_ADS_JSON, SP_BASE_URL + "small/api/download/poly/config");
            put(Action.SP_POST_NETSTAT_JSON, SP_BASE_URL + "small/command/report/ping");
            put(Action.CP_POST_PLAY_LIST_JSON, BuildConfig.BASE_URL + "box/Program/reportPlayInfo");
            put(Action.CP_POST_DOWNLOAD_LIST_JSON, BuildConfig.BASE_URL + "box/Program/reportDownloadInfo");
            put(Action.CP_POST_SDCARD_STATE_JSON, BuildConfig.BASE_URL + "Opclient20/BoxMem/boxMemoryInfo");
            put(Action.AD_BAIDU_ADS, BuildConfig.BAIDU_AD_BASE_URL);
        }
    };

    /**
     * 获取盒子初始化信息
     * @param context
     * @param handler
     * @param boxMac
     */
    public static String getBoxInitInfo(Context context, ApiRequestListener handler, String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_BOX_INIT_JSON, handler, params).syncGet();
    }

    /**
     * 处理小平台返回的节目数据
     * @param context
     * @param handler
     * @param boxMac
     * @return
     * @throws IOException
     */
    public static String getProgramDataFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_PROGRAM_DATA_FROM_JSON, handler, params).syncGet();

    }

    /**
     * 获取小平台宣传片文件
     * @param context
     * @param handler
     * @param boxMac
     * @return
     * @throws IOException
     */
    public static String getAdvDataFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_ADV_DATA_FROM_JSON, handler, params).syncGet();

    }

    /**
     *获取小平台广告列表
     * @param context
     * @param handler
     * @param boxMac
     * @return
     * @throws IOException
     */
    public static String getAdsDataFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException{
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_ADS_DATA_FROM_JSON, handler, params).syncGet();
    }

    /**
     * 获取小平台点播数据
     * @param context
     * @param handler
     * @param boxMac
     * @return
     * @throws IOException
     */
    public static String getOnDemandDataFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_ON_DEMAND_DATA_FROM_JSON, handler, params).syncGet();
    }

    /**
     * 获取小平台特色菜数据
     * @param context
     * @param handler
     * @param boxMac
     * @return
     * @throws IOException
     */
    public static String getSpecialtyFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_SPECIALTY_JSON, handler, params).syncGet();
    }

    /**
     * 获取实时竞价广告资源
     * @param context
     * @param handler
     * @param boxMac
     * @return
     * @throws IOException
     */
    public static String getRtbadsFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_RTB_ADS_JSON, handler, params).syncGet();
    }

    /**
     * 获取百度聚屏广告资源
     * @param context
     * @param handler
     * @param boxMac
     * @return
     * @throws IOException
     */
    public static String getPolyAdsFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_POLY_ADS_JSON, handler, params).syncGet();
    }

    /**
     * 获取小平台电视频道数据
     * @param context
     * @param handler
     */
    public static void getTVMatchDataFromSmallPlatform(Context context, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        new AppServiceOk(context, Action.SP_GET_TV_MATCH_DATA_FROM_JSON, handler, params).get();
    }

    /**
     * 获取小平台电视频道数据
     * @param context
     * @param handler
     */
    public static void getGiecTVMatchDataFromSmallPlatform(Context context, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        new AppServiceOk(context, Action.SP_GET_TV_MATCH_DATA_FROM_GIEC_JSON, handler, params).get();
    }

    /**
     *升级接口
     * @param context
     * @param handler
     */
    public static void upgradeInfo(Context context, ApiRequestListener handler,int versionCode) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("versionCode",versionCode);
        new AppServiceOk(context, Action.SP_GET_UPGRADE_INFO_JSON, handler, params).get();

    }

    public static void downloadLOGO(String url,Context context, ApiRequestListener handler,String filePath){
        final HashMap<String, Object> params = new HashMap<String, Object>();
        new AppServiceOk(context, Action.SP_GET_LOGO_DOWN, handler, params).downLoad(url, filePath);
    }

    public static void downloadLoadingImg(String url,Context context, ApiRequestListener handler,String filePath){
        final HashMap<String, Object> params = new HashMap<String, Object>();
        new AppServiceOk(context, Action.SP_GET_LOADING_IMG_DOWN, handler, params).downLoad(url, filePath);
    }

    /**
     * 下载文件
     * @param type 1是ROM2是apk
     * @param context
     * @param handler
     */
    public static void downVersion(String url,Context context, ApiRequestListener handler,int type){
        try{
            String target= AppUtils.getMainMediaPath();//AppUtils.getSDCardPath();
            if (TextUtils.isEmpty(target)) {
                LogFileUtil.write("External SD is not exist, download canceled");
                return;
            }

            String targetApk = null;
            if (type==1){
                targetApk=target + File.separator + ROM_DOWNLOAD_FILENAME;
            }else{
                targetApk=target + File.separator + APK_DOWNLOAD_FILENAME;
            }

            File tarFile =new File(targetApk);
            if(tarFile.exists()){
                tarFile.delete();
            }
            final HashMap<String, Object> params = new HashMap<String, Object>();
            new AppServiceOk(context, Action.SP_GET_UPGRADEDOWN, handler, params).downLoad(url, targetApk);
        }catch(Exception ex){
            LogUtils.d(ex.toString());
        }
    }

    /**
     * 心跳接口
     * @param context
     * @param handler
     */
    public static void heartbeat(Context context, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("clientid", 2);
        params.put("mac", Session.get(context).getEthernetMac());
        params.put("pro_period", Session.get(context).getProPeriod());
        params.put("adv_period", Session.get(context).getAdvPeriod());
        params.put("period", Session.get(context).getAdsPeriod());
        params.put("pro_download_period", Session.get(context).getProDownloadPeriod());
        params.put("adv_download_period", Session.get(context).getAdvDownloadPeriod());
        params.put("ads_download_period", Session.get(context).getAdsDownloadPeriod());
        params.put("demand", Session.get(context).getVodPeriod());
        params.put("vod_download_period", Session.get(context).getVodDownloadPeriod());
        params.put("specialty_period", Session.get(context).getSpecialtyPeriod());
        params.put("rtb_ads_period", Session.get(context).getRtbadsPeriod());
        params.put("apk", Session.get(context).getVersionName());
        params.put("war", "");
        params.put("logo", Session.get(context).getSplashVersion());
        params.put("p_load_version", Session.get(context).getLoadingVersion());
        params.put("ip", AppUtils.getLocalIPAddress());
        params.put("hotelId", Session.get(context).getBoiteId());
        params.put("roomId", Session.get(context).getRoomId());
        params.put("signal", AppUtils.getInputType(Session.get(context).getTvInputSource()));
        new AppServiceOk(context, Action.CP_GET_HEARTBEAT_PLAIN, handler, params).get();
    }

    /**
     * 获取小平台IP
     * @param context
     * @param handler
     */
    public static void getSpIp(Context context, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        new AppServiceOk(context, Action.CP_GET_SP_IP_JSON, handler, params).get();
    }

    public static void uploadProgram(Context context, ApiRequestListener handler, AtvProgramInfo[] programs) {
        if (programs == null || programs.length <= 0)
            return;
        List<AtvProgramInfo> programInfo = Arrays.asList(programs);
        final HashMap<String, Object> params = new HashMap<>();
        params.put("data", programInfo);
        new AppServiceOk(context, Action.SP_POST_UPLOAD_PROGRAM_JSON, handler, params).post();
    }

    public static void uploadProgram(Context context, ApiRequestListener handler, ArrayList<AtvChannel> programs) {
        if (programs == null || programs.size() <= 0)
            return;
        final HashMap<String, Object> params = new HashMap<>();
        params.put("data", programs);
        new AppServiceOk(context, Action.SP_POST_UPLOAD_PROGRAM_GIEC_JSON, handler, params).post();
    }

    /**
     * 获取奖项设置
     * @param context
     * @param handler
     */
    public static void getPrize(Context context, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("mac", Session.get(context).getEthernetMac());
        new AppServiceOk(context, Action.CP_GET_PRIZE_JSON, handler, params).post();
    }

    /**
     * 上报抽奖信息
     * @param context
     * @param handler
     */
    public static void reportLottery(Context context, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("mac", Session.get(context).getEthernetMac());
        params.put("date", AppUtils.getCurTime("yyyy-MM-dd"));
        new AppServiceOk(context, Action.CP_REPORT_LOTTERY_JSON, handler, params).post();
    }

    /**
     * 上报推送DeviceToken
     * @param context
     * @param handler
     */
    public static void reportDeviceToken(Context context, ApiRequestListener handler, String deviceToken) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("box_id", Session.get(context).getBoxId());
        params.put("box_mac", Session.get(context).getEthernetMac());
        params.put("device_token", deviceToken);
        new AppServiceOk(context, Action.CP_POST_DEVICE_TOKEN_JSON, handler, params).post();
    }

    /**
     * 通知手机投屏结束
     * @param context
     * @param handler
     */
    public static void notifyStop(Context context, ApiRequestListener handler, int type, String msg) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("type", type);
        params.put("tipMsg", msg);
        new AppServiceOk(context, Action.PH_NOTIFY_STOP_JSON, handler, params).get();
    }

    /**
     * 获取admaster配置文件
     * @param context
     * @param handler
     */
    public static void getAdMasterConfig(Context context, ApiRequestListener handler){
        final HashMap<String, Object> params = new HashMap<>();
        new AppServiceOk(context, Action.CP_GET_ADMASTER_CONFIG_JSON, handler, params).get();
    }

    /**
     * 上传网络状况
     * @param context
     * @param handler
     * @param intranetLatency 内网延时时间ms
     * @param internetLatency 外网延时时间ms
     */
    public static void postNetstat(Context context, ApiRequestListener handler, String intranetLatency, String internetLatency){
        final HashMap<String, Object> params = new HashMap<>();
        params.put("boxId", Session.get(context).getBoxId());
        params.put("boxMac", Session.get(context).getEthernetMac());
        params.put("innerDelayed", intranetLatency);
        params.put("outerDelayed", internetLatency);
        new AppServiceOk(context, Action.SP_POST_NETSTAT_JSON, handler, params).get();
    }

    /**
     * 上报当前播放列表
     * @param context
     * @param handler
     * @param detail    明细数据
     */
    public static void reportPlaylist(Context context, ApiRequestListener handler, PlaylistDetailRequestBean detail) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("box_mac", Session.get(context).getEthernetMac());
        params.put("resource_info", detail);
        new AppServiceOk(context, Action.CP_POST_PLAY_LIST_JSON, handler, params).post();
    }

    /**
     * 上报下载列表
     * @param context
     * @param handler
     * @param type      1广告；2节目；3宣传片
     * @param detail    明细数据
     */
    public static void reportDownloadList(Context context, ApiRequestListener handler, int type, DownloadDetailRequestBean detail) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("box_mac", Session.get(context).getEthernetMac());
        params.put("type", type);
        params.put("resource_info", detail);
        new AppServiceOk(context, Action.CP_POST_DOWNLOAD_LIST_JSON, handler, params).post();
    }

    /**
     * 上报SD卡异常
     * @param context
     * @param handler
     * @param type      1内存卡损坏；2内存卡已满
     */
    public static void reportSDCardState(Context context, ApiRequestListener handler, int type) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("box_id", Session.get(context).getBoxId());
        params.put("box_mac", Session.get(context).getEthernetMac());
        params.put("type", type);
        new AppServiceOk(context, Action.CP_POST_SDCARD_STATE_JSON, handler, params).post();
    }

    /**
     * 请求百度聚屏广告
     * @param context
     * @param handler
     * @param requestBean
     */
    public static void requestBaiduAds(Context context, ApiRequestListener handler, TsUiApiV20171122.TsApiRequest requestBean) {
        new AppServiceOk(context, Action.AD_BAIDU_ADS, handler).postProto(requestBean);
    }

    // 超时（网络）异常
    public static final String ERROR_TIMEOUT = "3001";
    // 业务异常
    public static final String ERROR_BUSSINESS = "3002";
    // 网络断开
    public static final String ERROR_NETWORK_FAILED = "3003";

    public static final String RESPONSE_CACHE = "3004";

    /**
     * 从这里定义业务的错误码
     */
    public static final int HTTP_RESPONSE_STATE_SUCCESS = 10000;
    /**
     * 数据返回错误
     */
    public static final int HTTP_RESPONSE_STATE_ERROR = 101;
    /**
     * 登录状态码
     */
    public static final int HTTP_RESPONSE_NEED_LOGIN = 310;
    /**
     * 收藏成功
     */
    public static final int HTTP_RESPONSE_ADD_GOODS_COLLECT = 10401;
    /**
     * 收藏成功
     */
    public static final int HTTP_RESPONSE_GOODS_CANCEL_COLLECT = 403;
    /**
     * 从缓存中取数据
     */
    public static final int HTTP_RESPONSE_STATE_CACHE = 99999;
    /**
     * 密码错误或签名错误
     */
    public static final int HTTP_RESPONSE_STATE_ERROR_PWDORSIGN = 400;
    /**
     * 绑定手机号用户无余额
     */
    public static final int HTTP_RESPONSE_CHECKMC_NO_MONEY = 10004;

}
