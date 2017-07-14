package com.savor.ads.core;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.savor.ads.BuildConfig;
import com.savor.ads.bean.AtvProgramInfo;
import com.savor.ads.bean.AtvProgramRequestBean;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
     * CP_前缀标识云平台接口；SP_前缀标识小平台接口
     */
    public static enum Action {
        SP_GET_ADVERT_DATA_FROM_JSON,
        SP_GET_ON_DEMAND_DATA_FROM_JSON,
        SP_GET_TV_MATCH_DATA_FROM_JSON,
        SP_POST_UPLOAD_LOG_JSON,
        SP_GET_UPGRADE_INFO_JSON,
        SP_GET_LOGO_DOWN,
        SP_GET_LOADING_IMG_DOWN,
        SP_GET_UPGRADEDOWN,
        CP_GET_HEARTBEAT_PLAIN,
        SP_POST_UPLOAD_PROGRAM_JSON,
        CP_GET_SP_IP_JSON,
        SP_GET_BOX_INIT_JSON,
        CP_GET_PRIZE_JSON,
        CP_REPORT_LOTTERY_JSON,
        PH_NOTIFY_STOP_JSON,
    }


    /**
     * URL集合
     */
    public static HashMap<Action, String> API_URLS = new HashMap<Action, String>() {
        private static final long serialVersionUID = -8469661978245513712L;

        {
            put(Action.SP_GET_ADVERT_DATA_FROM_JSON,SP_BASE_URL+"small/api/download/vod/config");
            put(Action.SP_GET_ON_DEMAND_DATA_FROM_JSON,SP_BASE_URL+"small/api/download/demand/config");
            put(Action.SP_GET_TV_MATCH_DATA_FROM_JSON,SP_BASE_URL+"small/tvList/api/stb/tv_getCommands");
            put(Action.SP_POST_UPLOAD_LOG_JSON,SP_BASE_URL+"small/log/upload-file");
            put(Action.SP_GET_UPGRADE_INFO_JSON,SP_BASE_URL+"small/api/download/apk/config");
            put(Action.CP_GET_HEARTBEAT_PLAIN, BuildConfig.BASE_URL + "Heartbeat/Report/index");
            put(Action.SP_POST_UPLOAD_PROGRAM_JSON, SP_BASE_URL + "small/tvList/api/stb/tv_commands");
            put(Action.CP_GET_SP_IP_JSON, BuildConfig.BASE_URL + "basedata/ipinfo/getIp");
            put(Action.SP_GET_BOX_INIT_JSON, SP_BASE_URL + "small/api/download/init");
            put(Action.CP_GET_PRIZE_JSON, BuildConfig.BASE_URL + "Award/Award/getAwardInfo");
            put(Action.CP_REPORT_LOTTERY_JSON, BuildConfig.BASE_URL + "Award/Award/recordAwardLog");
            put(Action.PH_NOTIFY_STOP_JSON, PHONE_BASE_URL + "stopProjection");
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

    //处理小平台广告数据
    public static String getAdvertDataFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_ADVERT_DATA_FROM_JSON, handler, params).syncGet();

    }

    //处理小平台点播数据
    public static String getOnDemandDataFromSmallPlatform(Context context, ApiRequestListener handler,String boxMac) throws IOException {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("boxMac",boxMac);
        return new AppServiceOk(context, Action.SP_GET_ON_DEMAND_DATA_FROM_JSON, handler, params).syncGet();

    }
    //处理小平台电视频道数据
    public static void getTVMatchDataFromSmallPlatform(Context context, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        new AppServiceOk(context, Action.SP_GET_TV_MATCH_DATA_FROM_JSON, handler, params).get();

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
            String target= AppUtils.getSDCardPath();
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
        if (Session.get(context).getPlayListVersion() != null && !Session.get(context).getPlayListVersion().isEmpty()) {
            params.put("period", AppUtils.findSpecifiedPeriodByType(Session.get(context).getPlayListVersion(), "ads"));
            params.put("adv_period", AppUtils.findSpecifiedPeriodByType(Session.get(context).getPlayListVersion(), "adv"));
            params.put("pro_period", AppUtils.findSpecifiedPeriodByType(Session.get(context).getPlayListVersion(), "pro"));
        }
        params.put("demand", Session.get(context).getVodPeriod());
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
