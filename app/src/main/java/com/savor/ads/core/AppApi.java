package com.savor.ads.core;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.savor.ads.bean.AtvProgramInfo;
import com.savor.ads.bean.AtvProgramRequestBean;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.FileUtils;
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
     * 小平台地址
     */
	public static String SP_BASE_URL = "http://192.168.1.2/";

//    /**
//     * 云平台线上环境
//     **/
//    public static final String BASE_URL = "http://www.savorx.cn/";
//    /**
//     * 云平台测试环境
//     **/
//    public static final String BASE_URL = "http://devp.api.rerdian.com/";
    /**
     * 云平台正式环境
     **/
    public static final String BASE_URL = "https://mb.rerdian.com/";

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

    /**
     * 常用的一些key值 ,签名、时间戳、token、params
     */
    public static final String SIGN = "sign";
    public static final String TIME = "time";
    public static final String TOKEN = "token";
    public static final String PARAMS = "params";

    public static final String APK_DOWNLOAD_FILENAME =  "updateapksamples.apk";
    public static final String ROM_DOWNLOAD_FILENAME =  "update_signed.zip";
    /**
     * Action-自定义行为 注意：自定义后缀必须为以下结束 _FORM:该请求是Form表单请求方式 _JSON:该请求是Json字符串
     * _XML:该请求是XML请求描述文件 _GOODS_DESCRIPTION:图文详情 __NOSIGN:参数不需要进行加密
     */
    public static enum Action {
        TEST_POST_JSON,
        TEST_GET_JSON,
        SP_GET_ADVERT_DATA_FROM_JSON,
        SP_GET_ON_DEMAND_DATA_FROM_JSON,
        SP_GET_TV_MATCH_DATA_FROM_JSON,
//        CP_REPORT_TECHNICAL_LOG_PLAIN,
        SP_POST_UPLOAD_LOG_JSON,
        SP_GET_UPGRADE_INFO_JSON,
        SP_GET_LOGO_DOWN,
        SP_GET_UPGRADEDOWN,
        CP_GET_HEARTBEAT_PLAIN,
        SP_POST_UPLOAD_PROGRAM_JSON,
        CP_GET_SP_IP_JSON,
        SP_GET_BOX_INIT_JSON,
    }


    /**
     * API_URLS:TODO(URL集合)
     */
    public static HashMap<Action, String> API_URLS = new HashMap<Action, String>() {
        private static final long serialVersionUID = -8469661978245513712L;

        {
            put(Action.SP_GET_ADVERT_DATA_FROM_JSON,SP_BASE_URL+"small/api/download/vod/config");
            put(Action.SP_GET_ON_DEMAND_DATA_FROM_JSON,SP_BASE_URL+"small/api/download/demand/config");
            put(Action.SP_GET_TV_MATCH_DATA_FROM_JSON,SP_BASE_URL+"small/tvList/api/stb/tv_getCommands");
            put(Action.SP_POST_UPLOAD_LOG_JSON,SP_BASE_URL+"small/log/upload-file");
            put(Action.SP_GET_UPGRADE_INFO_JSON,SP_BASE_URL+"small/api/download/apk/config");
//            put(Action.CP_REPORT_TECHNICAL_LOG_PLAIN, BASE_URL + "getfaultInterface.html");
            put(Action.CP_GET_HEARTBEAT_PLAIN, "https://sapi.rerdian.com/survival/api/2/survival");
            put(Action.SP_POST_UPLOAD_PROGRAM_JSON, SP_BASE_URL + "small/tvList/api/stb/tv_commands");
            put(Action.CP_GET_SP_IP_JSON, BASE_URL + "mobile/api/getIp");
            put(Action.SP_GET_BOX_INIT_JSON, SP_BASE_URL + "small/api/download/init");
        }
    };

    public static void testPost(Context context, String orderNo, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("loginfield", "15901559579");
        params.put("password", "123456");
        params.put("dr_rg_cd", "86");
        params.put("version_code", 19 + "");
        new AppServiceOk(context, Action.TEST_POST_JSON, handler, params).post(false, false, true, true);

    }

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
     * 上传业务日志
     * @param context
     * @param handler
     * @param fileName
     */
    public static void postUploadLog(Context context, ApiRequestListener handler,String fileName,String archive) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        new AppServiceOk(context, Action.SP_POST_UPLOAD_LOG_JSON, handler, params).uploadFile(fileName,archive);

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
        try{
            final HashMap<String, Object> params = new HashMap<String, Object>();
            new AppServiceOk(context, Action.SP_GET_LOGO_DOWN, handler, params).downLoad(url, filePath);
        }catch(Exception ex){
            LogUtils.d(ex.toString());
        }
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
            String targetApk = null;
            if (type==1){
                targetApk=target+ROM_DOWNLOAD_FILENAME;
            }else{
                targetApk=target+APK_DOWNLOAD_FILENAME;
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
//    /**
//     * 上报技术日志
//     * @param context
//     * @param handler
//     * @param event             事件类型
//     * @param md5FailedVid      MD5校验失败的视频ID
//     * @param newAdsPeriod      更新的广告期号
//     * @param newVodPeriod      当前的点播期号
//     * @param newApkVer         更新的APK version name
//     * @param newRomVer         更新的Rom version
//     */
//    public static void reportTechnicalLog(Context context, ApiRequestListener handler, @NonNull String event,
//                                          String md5FailedVid, String newAdsPeriod, String newVodPeriod,
//                                          String newApkVer, String newRomVer) {
////        List<Object> list = new ArrayList<Object>();
////        list.add(log);
////        String md5 = AppUtils.MD5(("body" + list).getBytes());
////        String msg = list.toString();
//        final HashMap<String, Object> params = new HashMap<String, Object>();
//        // 标识是机顶盒发的
//        params.put("type", "stb");
//        // 盒子MAC地址
//        params.put("mac", Session.get(context).getEthernetMac());
//        // 事件类型
//        params.put("event", event);
//        // 酒楼ID
//        params.put("boite", Session.get(context).getBoiteId());
//        // MD5校验失败的视频ID
//        params.put("fvid", md5FailedVid);
//        if (!TextUtils.isEmpty(newApkVer)) {
//            // 正在使用的apk version name
//            params.put("apkv", Session.get(context).getVersionName());
//        }
//        // 更新的APK version name
//        params.put("apkvnew", newApkVer);
//        if (!TextUtils.isEmpty(newRomVer)) {
//            // 正在使用的rom version
//            params.put("romv", Session.get(context).getRomVersion());
//        }
//        // 更新的Rom version
//        params.put("romvnew", newRomVer);
//        if (!TextUtils.isEmpty(newAdsPeriod)) {
//            // 更新的广告期号
//            params.put("aperiodnew", newAdsPeriod);
//        }
//        // 当前的广告期号
//        params.put("aperiod", Session.get(context).getAdvertMediaPeriod());
//        if (!TextUtils.isEmpty(newVodPeriod)) {
//            // 更新的点播期号
//            params.put("vperiodnew", newVodPeriod);
//        }
//        // 当前的点播期号
//        params.put("vperiod", Session.get(context).getMulticastMediaPeriod());
//        // 时间戳
//        params.put("ts", System.currentTimeMillis());
////        params.put("encrypt", md5);
////        params.put("body", msg);
//        new AppServiceOk(context, Action.CP_REPORT_TECHNICAL_LOG_PLAIN, handler, params).get();
//    }

    /**
     * 心跳接口
     * @param context
     * @param handler
     */
    public static void heartbeat(Context context, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("mac", Session.get(context).getEthernetMac());
        params.put("period", Session.get(context).getAdvertMediaPeriod());
        params.put("demand", Session.get(context).getMulticastMediaPeriod());
        params.put("apk", Session.get(context).getVersionName());
        params.put("war", "");
        params.put("logo ", Session.get(context).getSplashVersion());
        params.put("ip", AppUtils.getLocalIPAddress() + "");
        params.put("hotelId", Session.get(context).getBoiteId() + "");
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

    public static void testDownload(Context context, String url, ApiRequestListener handler) {
        final HashMap<String, Object> params = new HashMap<String, Object>();
//        String target = AppUtils.getPath(context, AppUtils.StorageFile.file);

//        String targetApk = target + "123.apk";
//        File tarFile = new File(targetApk);
//        if (tarFile.exists()) {
//            tarFile.delete();
//        }
//        new AppServiceOk(context, Action.TEST_DOWNLOAD_JSON, handler, params).downLoad(url, targetApk);

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
