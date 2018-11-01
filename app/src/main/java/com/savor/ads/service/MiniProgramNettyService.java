package com.savor.ads.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.BuildConfig;
import com.savor.ads.SavorApplication;
import com.savor.ads.activity.AdsPlayerActivity;
import com.savor.ads.activity.MainActivity;
import com.savor.ads.activity.MonkeyGameActivity;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.MiniProgramProjection;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.dialog.AtlasDialog;
import com.savor.ads.dialog.CircularProgressDialog;
import com.savor.ads.oss.OSSUtils;
import com.savor.ads.projection.ProjectionManager;
import com.savor.ads.projection.action.VodAction;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.MiniProgramQrCodeWindowManager;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import cn.savor.small.netty.MiniProNettyClient;
import cn.savor.small.netty.MiniProNettyClient.MiniNettyMsgCallback;
import io.netty.bootstrap.Bootstrap;

/**
 * 启动小程序Netty服务
 * Created by zhanghq on 2018/07/09.
 */
public class MiniProgramNettyService extends IntentService implements MiniNettyMsgCallback{
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    private Context context;
    private Session session;
    AtlasDialog atlasDialog =null;
    CircularProgressDialog circularProgressDialog = null;
    private int INTERVAL_TIME=1000*8;
    private int downloadIndex;
    private int currentIndex;
    private String words;
    private String avatarUrl;
    private String nickName;
    private int img_nums=0;
    //是否进程在下载中
    private boolean isDownloadRunnable= false;
    //是否正在播放ppt
    private boolean isPPTRunnable = false;
    //通过while的形式去下载netty传过来的文件
    boolean download=true;
    Handler handler=new Handler(Looper.getMainLooper());
    public MiniProgramNettyService() {
        super("");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        session = Session.get(context);
        atlasDialog = new AtlasDialog(context);
        circularProgressDialog = new CircularProgressDialog(context);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LogUtils.d("MiniProgramNettyService onHandleIntent");
        LogFileUtil.write("MiniProgramNettyService onHandleIntent");
        try {
            LogUtils.d("启动小程序NettyService");
            LogFileUtil.write("启动小程序NettyService");

            fetchMessage();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("MiniProgramNettyService onDestroy");
        LogFileUtil.write("MiniProgramNettyService onDestroy");
    }

    private void fetchMessage() throws InterruptedException {
        try{
            LogUtils.d("MiniProgramNettyService fetchMessage");

            MiniProNettyClient.init(ConstantValues.MINI_PROGRAM_NETTY_PORT,ConstantValues.MINI_PROGRAM_NETTY_URL,this,getApplicationContext());
            MiniProNettyClient.get().connect(MiniProNettyClient.get().configureBootstrap(new Bootstrap()));

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onReceiveMiniServerMsg(String msg, String content) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        /***
         * action字段
         * 1:呼玛  2：投屏：3 退出投屏 4:投屏多张图片（包括单张）5:点播机顶盒内存在的视频 9:手机小程序呼出大码
         * 101：发起游戏 102:开始游戏 103:加入游戏 104:退出游戏 105:原班人马，在玩一次
         * 999:下载固定资源测试下载速度
         * 31:加减音量，change_type 1:减音量 2:加音量
         */

        if (ConstantValues.NETTY_MINI_PROGRAM_COMMAND.equals(msg)){
            if (!TextUtils.isEmpty(content)){
                try {
                    JSONObject jsonObject = new JSONObject(content);
                    int action = jsonObject.getInt("action");
                    if (action != 101 && action != 102 && action != 103 && action != 105
                            && ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
                        MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
                        activity.exitGame();
                    }
                    if (action != 4) {
                        if (handler != null) {
                            handler.removeCallbacks(mProjectShowImageRunnable);
                            handler.removeCallbacks(downloadFileRunnable);
                        }
                    }
                    //如果是小程序投屏的话，就将app投屏状态情况
                    GlobalValues.CURRENT_PROJECT_BITMAP = null;
                    MiniProgramProjection miniProgramProjection = gson.fromJson(content, new TypeToken<MiniProgramProjection>() {
                    }.getType());
                    if (miniProgramProjection!=null){
                        avatarUrl = miniProgramProjection.getAvatarUrl();
                        nickName = miniProgramProjection.getNickName();
                    }
                    switch (action) {

                        case 1:
                            showQrCode(miniProgramProjection.getCode());
                            break;
                        case 2:
                            projectionSingleImgOrVideo(miniProgramProjection);
                            break;
                        case 3:
                            exitProjection();
                            break;
                        case 4:
                            projectionMoreImg(miniProgramProjection);
                            break;
                        case 5:
                            onDemandSetTopBoxVideo(miniProgramProjection.getFilename(),miniProgramProjection.getUrl());
                            break;
                        case 9:
                            callBigQrCodeVideo();
                            break;
                        case 101:
                            launchMonkeyGame(action,miniProgramProjection);
                            break;
                        case 102:
                        case 105:
                            startMonkeyGame(action,miniProgramProjection);
                            break;
                        case 103:
                            addMonkeyGame(action,miniProgramProjection);
                            break;
                        case 104:
                            exitMonkeyGame();
                            break;
                        case 999:
                            testNetDownload(miniProgramProjection);
                            break;
                        case 31:
                            if (jsonObject.has("change_type")){
                                int adjust = jsonObject.getInt("change_type");
                                adjustVoice(adjust);
                            }
                            break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    public void onMiniConnected() {
        //TODO:当建立NETTY连接以后请求接口获取小程序地址
        LogUtils.i("CurrentActivity.................." + ActivitiesManager.getInstance().getCurrentActivity());
        session.setHeartbeatMiniNetty(true);

    }

    private void projectShowImage(int currentIndex,String words,String avatarUrl,String nickName){
        if (GlobalValues.PROJECT_IMAGES!=null&&GlobalValues.PROJECT_IMAGES.size()>0){
            boolean flag = true;
            if (GlobalValues.PROJECT_IMAGES.size()>currentIndex){
                String uri = GlobalValues.PROJECT_IMAGES.get(currentIndex);
                ProjectOperationListener.getInstance(context).showImage(1,uri,true,words,avatarUrl,nickName);
            }else{
                flag = false;
            }
            if (handler!=null){
                if (flag){
                    handler.postDelayed(mProjectShowImageRunnable,INTERVAL_TIME);
                }else {
                    handler.removeCallbacks(mProjectShowImageRunnable);
                }
            }


        }

    }

    private Runnable  mProjectShowImageRunnable = new Runnable(){

        @Override
        public void run() {
            currentIndex ++;
            projectShowImage(currentIndex,words,avatarUrl,nickName);
        }
    };

    private Runnable mProjectExitDownloadRunnable = new Runnable() {
        @Override
        public void run() {
            download = false;
        }
    };

    @Override
    public void onMiniReconnect() {
            Intent intent = new Intent(this,MiniProgramNettyService.class);
            startService(intent);
    }

    @Override
    public void onMiniCloseIcon() {
        Activity activity = ActivitiesManager.getInstance().getSpecialActivity(AdsPlayerActivity.class);
        if (activity!=null && activity instanceof AdsPlayerActivity){
            ((SavorApplication) getApplication()).hideMiniProgramQrCodeWindow();
        }
    }


    ApiRequestListener apiRequestListener = new ApiRequestListener() {
        @Override
        public void onSuccess(AppApi.Action method, Object obj) {
            switch (method){

            }
        }

        @Override
        public void onError(AppApi.Action method, Object obj) {

        }

        @Override
        public void onNetworkFailed(AppApi.Action method) {

        }
    };
    /**
     * 上传小程序投屏参数到云
     * */
    private void postMiniProgramGameParam(int action,MiniProgramProjection programProjection){
        if (programProjection==null){
            return;
        }
        HashMap<String,Object> params = new HashMap<>();
        switch (action){
            case 101:
                params.put("action","1");
                params.put("activity_id",programProjection.getActivity_id());
                params.put("order_time",System.currentTimeMillis());
                postProjectionGamesLog(params);
                break;
            case 102:
                params.put("action","3");
                params.put("activity_id",programProjection.getActivity_id());
                params.put("order_time",System.currentTimeMillis());
                postProjectionGamesLog(params);
                break;

            case 103:
                params.put("action","2");
                params.put("activity_id",programProjection.getActivity_id());
                params.put("openid",programProjection.getOpenid());
                params.put("order_time",System.currentTimeMillis());
                postProjectionGamesLog(params);
                break;
        }
    }



    /**
     * 小程序投屏日志统计接口，不在区分资源类型
     * @param params
     */
    private void postProjectionResourceLog(HashMap<String,Object> params){
        AppApi.postProjectionResourceParam(context,apiRequestListener,params);
    }


    /**
     * 互动游戏数据上传
     * */
    private void postProjectionGamesLog(HashMap<String,Object> params){
        AppApi.postProjectionGamesParam(context,apiRequestListener,params);
    }

    public interface DownloadProgressListener{
        void getDownloadProgress(long currentSize, long totalSize);
    }

    /**
     * 展示二维码
     * @param code
     */
    private void showQrCode(int code){
        if (!(ActivitiesManager.getInstance().getCurrentActivity() instanceof MainActivity)) {
            if (getApplication() instanceof SavorApplication) {
                ((SavorApplication) getApplication()).showQrCodeWindow(code + "");
            }
        }
    }

    /**
     * 点击单张图片或者投视频
     * @param miniProgramProjection
     */
    private void projectionSingleImgOrVideo(MiniProgramProjection miniProgramProjection){
        if (miniProgramProjection == null||TextUtils.isEmpty(miniProgramProjection.getUrl())) {
            return;
        }
        boolean isDownloaded = false;
        int resourceType = miniProgramProjection.getResource_type();
        String fileName = miniProgramProjection.getFilename();
        String url = miniProgramProjection.getUrl();
        String openid = miniProgramProjection.getOpenid();
        String forscreen_id = miniProgramProjection.getForscreen_id();
        long startTime = System.currentTimeMillis();
        LogUtils.d("-|-|开始下载时间"+startTime);
        HashMap<String, Object> params = new HashMap<>();
        params.put("box_mac", session.getEthernetMac());
        params.put("forscreen_id", forscreen_id);
        if (1 == resourceType) {
            params.put("resource_id", miniProgramProjection.getImg_id());
        } else {
            params.put("resource_id", miniProgramProjection.getVideo_id());
        }
        params.put("openid", openid);
        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.lottery) + fileName;
        File file = new File(path);
        if (file.exists()) {
            isDownloaded = true;
            params.put("is_exist", 1);
        } else {
            params.put("is_exist", 0);
            OSSUtils ossUtils = new OSSUtils(context,
                    BuildConfig.OSS_BUCKET_NAME,
                    url,
                    file);
            ossUtils.setDownloadProgressListener(new DownloadProgressListener() {
                @Override
                public void getDownloadProgress(final long currentSize, final long totalSize) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (atlasDialog != null && atlasDialog.isShowing()) {
                                atlasDialog.projectTipAnimateOut();
                                atlasDialog.dismiss();
                            }
                            if (circularProgressDialog != null && !circularProgressDialog.isShowing()) {
                                circularProgressDialog.show();

                            }
                            circularProgressDialog.initContent();
                            circularProgressDialog.projectTipAnimateIn();
                            BigDecimal b = new BigDecimal(currentSize * 1.0 / totalSize);
                            Log.d("circularProgress", "原始除法得到的值" + currentSize * 1.0 / totalSize);
                            float f1 = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
                            Log.d("circularProgress", "保留两位小数得到的值" + f1);
                            if (f1 >= 0.01f) {
                                String value = String.valueOf(f1 * 100);
                                int progress = Integer.valueOf(value.split("\\.")[0]);
                                Log.d("circularProgress", "乘以100并且转成整数得到的值" + progress);
                                circularProgressDialog.initnum(progress);
                            }

                        }
                    });
                }
            });

            isDownloaded = ossUtils.syncDownload();
            if (resourceType == 2) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (circularProgressDialog != null) {
                            circularProgressDialog.projectTipAnimateOut();
                            circularProgressDialog.dismiss();
                        }
                    }
                });
            }
        }
        if (isDownloaded) {
            long endTime = System.currentTimeMillis();
            long downloadTime =  endTime- startTime;
            LogUtils.d("-|-|结束下载时间"+endTime);
            LogUtils.d("-|-|下载所用时间"+downloadTime);
            params.put("used_time", downloadTime);
            postProjectionResourceLog(params);
            MobclickAgent.onEvent(context, "screenProjctionDownloadSuccess" + file.getName());
            if (1 == resourceType) {
                if (!TextUtils.isEmpty(GlobalValues.PROJECTION_WORDS)) {
                    ProjectOperationListener.getInstance(context).showImage(1, path, true, GlobalValues.PROJECTION_WORDS, avatarUrl, nickName);
                } else {
                    ProjectOperationListener.getInstance(context).showImage(1, path, true, avatarUrl, nickName);
                }
            } else if (2 == resourceType) {
                ProjectOperationListener.getInstance(context).showVideo(path, 0, true, avatarUrl, nickName);
            }
        } else {
            MobclickAgent.onEvent(context, "screenProjctionDownloadError" + file.getName());
        }
    }

    /**
     * 退出投屏
     */
    private void exitProjection(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (atlasDialog != null && atlasDialog.isShowing()) {
                    atlasDialog.projectTipAnimateOut();
                    atlasDialog.dismiss();
                }
                if (circularProgressDialog != null && circularProgressDialog.isShowing()) {
                    circularProgressDialog.projectTipAnimateOut();
                    circularProgressDialog.dismiss();
                }
            }
        });

        ProjectOperationListener.getInstance(context).stop(GlobalValues.CURRENT_PROJECT_ID);
    }

    /**
     * 投多张图片
     * @param miniProgramProjection
     */
    private void projectionMoreImg(final MiniProgramProjection miniProgramProjection){
        if (miniProgramProjection == null || TextUtils.isEmpty(miniProgramProjection.getUrl())) {
            return;
        }
        words = miniProgramProjection.getForscreen_char();
        img_nums = miniProgramProjection.getImg_nums();
        final String openid = miniProgramProjection.getOpenid();
        final String forscreen_id = miniProgramProjection.getForscreen_id();

        if (img_nums> 0) {

            handler.post(new Runnable() {
                public void run() {
                    if (circularProgressDialog != null && circularProgressDialog.isShowing()) {
                        circularProgressDialog.projectTipAnimateOut();
                        circularProgressDialog.dismiss();

                    }
                    if (!atlasDialog.isShowing()) {
                        atlasDialog.show();
                    }
                    atlasDialog.initContent();
                    atlasDialog.initnum(0);
                    atlasDialog.projectTipAnimateIn();

                }
            });
            if (!TextUtils.isEmpty(GlobalValues.CURRENT_OPEN_ID)
                    && openid.equals(GlobalValues.CURRENT_OPEN_ID)
                    && !TextUtils.isEmpty(GlobalValues.CURRRNT_PROJECT_ID)
                    && forscreen_id.equals(GlobalValues.CURRRNT_PROJECT_ID)) {
                GlobalValues.PROJECTION_WORDS = words;
                isDownloadRunnable = true;
                isPPTRunnable = true;
            } else {
                GlobalValues.PROJECT_IMAGES.clear();
                GlobalValues.PROJECT_LIST.clear();
                GlobalValues.CURRENT_OPEN_ID = openid;
                GlobalValues.CURRRNT_PROJECT_ID = forscreen_id;
                GlobalValues.PROJECTION_WORDS = words;
                if (handler != null) {
                    handler.removeCallbacks(mProjectShowImageRunnable);
                }
                isDownloadRunnable = false;
                isPPTRunnable = false;
            }

            LogUtils.d("当前PROJECT_IMAGES=" + GlobalValues.PROJECT_IMAGES.size());

            //----------------------------------------------------//
            GlobalValues.PROJECT_LIST.add(miniProgramProjection);
            if (!isDownloadRunnable){
                handler.removeCallbacks(downloadFileRunnable);
                downloadIndex =0;
                downloadFile(downloadIndex);
            }
            if(!isPPTRunnable&&img_nums > 1) {
                if (GlobalValues.PROJECT_IMAGES != null && GlobalValues.PROJECT_IMAGES.size()>0) {
                    currentIndex = 0;
                    projectShowImage(currentIndex, words, avatarUrl, nickName);
                }
            } else if (img_nums == 1) {
                if (GlobalValues.PROJECT_IMAGES.size()>0){
                    String path=GlobalValues.PROJECT_IMAGES.get(0);
                    ProjectOperationListener.getInstance(context).showImage(1, path, true, words, avatarUrl, nickName);
                }
            }
        }

    }

    /**
     * 点击机顶盒内视频
     * @param fileName
     * @param url
     */
    private void onDemandSetTopBoxVideo(String fileName,String url){
        if (TextUtils.isEmpty(fileName)||TextUtils.isEmpty(url)){
            return;
        }

        String selection = DBHelper.MediaDBInfo.FieldName.MEDIANAME + "=? ";
        String[] selectionArgs = new String[]{fileName};
        List<MediaLibBean> listPlayList = DBHelper.get(context).findNewPlayListByWhere(selection, selectionArgs);
        List<MediaLibBean> listMutlicast = DBHelper.get(context).findMutlicastMediaLibByWhere(selection, selectionArgs);
        if (listPlayList != null && listPlayList.size() > 0) {
            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + fileName;
            File file = new File(path);
            if (file.exists()) {
                ProjectOperationListener.getInstance(context).showVod(fileName, "", 0, false, true);
            }
        } else if (listMutlicast != null && listMutlicast.size() > 0) {
            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast) + fileName;
            File file = new File(path);
            if (file.exists()) {
                ProjectOperationListener.getInstance(context).showVod(fileName, "", 0, false, true);
            }
        } else {
            ProjectOperationListener.getInstance(context).showVideo(url, 0, true);
        }
    }

    /**
     *通过小程序呼出展示大码的视频
     */
    private void callBigQrCodeVideo(){
        String selection = DBHelper.MediaDBInfo.FieldName.VID + "=? ";
        String[] selectionArgs = new String[]{"17614"};
        List<MediaLibBean> listPlayList = DBHelper.get(context).findNewPlayListByWhere(selection, selectionArgs);
        List<MediaLibBean> listMutlicast = DBHelper.get(context).findMutlicastMediaLibByWhere(selection, selectionArgs);
        if (listPlayList != null && listPlayList.size() > 0) {
            MediaLibBean bean = listPlayList.get(0);
            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.media) + bean.getName();
            File file = new File(path);
            if (file.exists()) {
                VodAction vodAction = new VodAction(context, "17614", path, 0, false, true);
                ProjectionManager.getInstance().enqueueAction(vodAction);
            }
        } else if (listMutlicast != null && listMutlicast.size() > 0) {
            MediaLibBean bean = listMutlicast.get(0);
            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.multicast) + bean.getName();
            File file = new File(path);
            if (file.exists()) {
                VodAction vodAction = new VodAction(context, "17614", path, 0, false, true);
                ProjectionManager.getInstance().enqueueAction(vodAction);
            }
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MiniProgramQrCodeWindowManager.get(context).setCurrentPlayMediaId("17614");
                ((SavorApplication) getApplication()).showMiniProgramQrCodeWindow(ConstantValues.MINI_PROGRAM_CALL_TYPE);
            }
        }, 1000);
    }

    /**
     * 发起小游戏
     * @param action
     * @param miniProgramProjection
     */
    private void launchMonkeyGame(int action,MiniProgramProjection miniProgramProjection){
        if (handler != null) {
            handler.removeCallbacks(mProjectShowImageRunnable);
        }
        Intent intent = new Intent(context, MonkeyGameActivity.class);
        intent.putExtra("miniProgramProjection", miniProgramProjection);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        postMiniProgramGameParam(action, miniProgramProjection);
    }

    /**
     * 开始互动小游戏
     * @param action
     * @param miniProgramProjection
     */
    private void startMonkeyGame(int action,MiniProgramProjection miniProgramProjection){
        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
            activity.startGame();
            if (action != 105) {
                postMiniProgramGameParam(action, miniProgramProjection);
            }
        }
    }

    /**
     * 加入互动小游戏
     * @param action
     * @param miniProgramProjection
     */
    private void addMonkeyGame(int action,MiniProgramProjection miniProgramProjection){
        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
            activity.addWeixinAvatarToGame(miniProgramProjection);
            postMiniProgramGameParam(action, miniProgramProjection);
        }
    }

    /**
     * 退出互动小游戏
     */
    private void exitMonkeyGame(){
        if (ActivitiesManager.getInstance().getCurrentActivity() instanceof MonkeyGameActivity) {
            MonkeyGameActivity activity = (MonkeyGameActivity) ActivitiesManager.getInstance().getCurrentActivity();
            activity.exitGame();
        }
    }

    /**
     * 测试网速专用
     * @param miniProgramProjection
     */
    private void testNetDownload(MiniProgramProjection miniProgramProjection){
        if (miniProgramProjection == null) {
            return;
        }
        HashMap params = new HashMap<>();
        params.put("box_mac", session.getEthernetMac());
        params.put("forscreen_id", miniProgramProjection.getForscreen_id());
        params.put("resource_id", miniProgramProjection.getVideo_id());
        params.put("openid", miniProgramProjection.getOpenid());
        params.put("is_exist", 0);

        String fileName = miniProgramProjection.getFilename();
        String url = miniProgramProjection.getUrl();
        if (TextUtils.isEmpty(url)||TextUtils.isEmpty(fileName)) {
            return;
        }
        String path = AppUtils.getFilePath(context, AppUtils.StorageFile.lottery) + fileName;
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        long startTime = System.currentTimeMillis();
        OSSUtils ossUtils = new OSSUtils(context,
                BuildConfig.OSS_BUCKET_NAME,
                url,
                file);

        if (ossUtils.syncDownload()) {
            params.put("used_time", startTime - System.currentTimeMillis());
            postProjectionResourceLog(params);
        }
    }

    /**
     * 下载文件
     * @return
     */
    private Runnable downloadFileRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.d("下载测试----进入线程");

            downloadIndex ++;
            if (GlobalValues.PROJECT_LIST.size()<=downloadIndex){
                downloadIndex--;
                return;
            }
            LogUtils.d("下载测试----GlobalValues.PROJECT_LIST.size()="+GlobalValues.PROJECT_LIST.size());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    downloadFile(downloadIndex);
                }
            }).start();
        }
    };



    private void downloadFile(int index){
        boolean isDownloaded;
        if (GlobalValues.PROJECT_LIST.size()>index){
            MiniProgramProjection projection = GlobalValues.PROJECT_LIST.get(index);
            final HashMap params = new HashMap<>();
            params.put("box_mac", session.getEthernetMac());
            params.put("forscreen_id", projection.getForscreen_id());
            params.put("openid", projection.getOpenid());
            params.put("resource_id", projection.getImg_id());
            long startTime = System.currentTimeMillis();
            String path = AppUtils.getFilePath(context, AppUtils.StorageFile.lottery) + projection.getFilename();
            File file = new File(path);
            if (file.exists()) {
                params.put("is_exist", 1);
                isDownloaded = true;
            } else {
                params.put("is_exist", 0);
                OSSUtils ossUtils = new OSSUtils(context,
                        BuildConfig.OSS_BUCKET_NAME,
                        projection.getUrl(),
                        file);
                isDownloaded = ossUtils.syncDownload();
            }
            if (isDownloaded) {
                long endTime = System.currentTimeMillis() - startTime;
                params.put("used_time", endTime);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        postProjectionResourceLog(params);
                    }
                });
                if (!GlobalValues.PROJECT_IMAGES.contains(path)){
                    GlobalValues.PROJECT_IMAGES.add(path);
                }
                LogUtils.d("下载完的视频地址："+projection.getUrl());
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    atlasDialog.initnum(GlobalValues.PROJECT_IMAGES.size());
                }
            });
            if (GlobalValues.PROJECT_IMAGES.size()==img_nums){
                handler.removeCallbacks(downloadFileRunnable);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        download = false;
                        atlasDialog.projectTipAnimateOut();
                        atlasDialog.dismiss();
                    }
                },1000);
            }else{
                if (img_nums>1){
                    handler.postDelayed(downloadFileRunnable,500);
                }else{
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            download = false;
                            atlasDialog.projectTipAnimateOut();
                            atlasDialog.dismiss();
                        }
                    });
                }
            }
        }
    }

    /**
     *
     * @param value 1:减音量 2：加音量
     */
    private void adjustVoice(int value){
        switch (value){
            case 1:
                ProjectOperationListener.getInstance(context).volume(3,GlobalValues.CURRENT_PROJECT_ID);
                break;
            case 2:
                ProjectOperationListener.getInstance(context).volume(4,GlobalValues.CURRENT_PROJECT_ID);
            break;
        }

    }
}
