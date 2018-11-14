package com.savor.ads.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.activity.BaseActivity;
import com.savor.ads.bean.DownloadDetailRequestBean;
import com.savor.ads.bean.MediaDownloadBean;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.MediaPlaylistBean;
import com.savor.ads.bean.NettyBalancingResult;
import com.savor.ads.bean.PlaylistDetailRequestBean;
import com.savor.ads.bean.ProgramBean;
import com.savor.ads.bean.ProgramBeanResult;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.bean.SetBoxTopResult;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.savor.small.netty.NettyClient;


/**
 */
public class HeartbeatService extends IntentService implements ApiRequestListener {
    /**
     * 心跳周期，5分钟
     */
    private static final int HEARTBEAT_DURATION = 1000 * 60 * 5;
    /**
     * 小平台信息检测周期，1分钟
     */
    private static final int SERVER_INFO_CHECK_DURATION = 1000 * 60 * 1;
    /**
     * 单次循环等待时长。
     * 由于要在关键时间点上做检测，这里须>30sec <1min
     */
    private static final int ONE_CYCLE_TIME = 1000 * 40;

    /**
     * 上一个心跳过去的时长
     */
    private int mHeartbeatElapsedTime = 0;
    /**
     * 上一个小平台信息监测过去的时长
     */
    private int mServerInfoCheckElapsedTime = 0;

    private long total_data = TrafficStats.getTotalRxBytes();
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what ==1){
                int real_data = (int)msg.arg1;
                if(real_data>1024){
//                    listSpeed.add(real_data/1024+"kb/s");
                    Session.get(HeartbeatService.this).setNetSpeed(real_data/1024+"kb/s");
                    Log.d("speed",real_data/1024+"kb/s");
                }else{
                    Log.d("speed",real_data+"b/s");
                    Session.get(HeartbeatService.this).setNetSpeed(real_data+"b/s");
//                    listSpeed.add(real_data+"b/s");
                }
            }
        }
    };
    /**几秒刷新一次**/
    private final int count = 5;
    private Session session;
    public HeartbeatService() {
        super("HeartbeatService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // 循环检查网络情况直到可用
        do {
            LogFileUtil.write("HandleMediaDataService will check server info and network");
            if (AppUtils.isNetworkAvailable(this)) {
                break;
            }

            try {
                Thread.sleep(1000 * 2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } while (true);
        session = Session.get(this);
        //  启动时立即心跳一次
        LogFileUtil.write("开机立刻上报心跳和调用是否显示小程序码接口一次");
        doHeartbeat();
        getNettyBalancingInfo();
        doShowMiniProgramQRCode();
        monitorDownloadSpeed();

        if (!Session.get(this).isUseVirtualSp()) {
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            executorService.scheduleAtFixedRate(mNetworkDetectionRunnable, 1, 5, TimeUnit.MINUTES);
        }

        while (true) {

            // 小平台信息监测周期到达
            if (mServerInfoCheckElapsedTime >= SERVER_INFO_CHECK_DURATION) {
                mServerInfoCheckElapsedTime = 0;

                if (Session.get(this).getServerInfo() == null) {
                    Intent intent1 = new Intent(this, ServerDiscoveryService.class);
                    startService(intent1);

                    httpGetIp();
                }
            }

            // 心跳周期到达，向云平台发送心跳
            if (mHeartbeatElapsedTime >= HEARTBEAT_DURATION) {
                mHeartbeatElapsedTime = 0;

                doHeartbeat();
                doShowMiniProgramQRCode();

                getNettyBalancingInfo();
                try {
                    reportMediaDetail();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String time = AppUtils.getCurTime("HH:mm");
            // 检测时间是否到达凌晨2点整
            if ("02:00".equals(time)) {
                // 去删除存本地的投屏文件
                AppUtils.clearPptTmpFiles(this);
                AppUtils.clearAllCache(this);

                // 刷新播放列表
                Activity activity = ActivitiesManager.getInstance().getCurrentActivity();
                if (activity instanceof BaseActivity) {
                    BaseActivity baseActivity = (BaseActivity) activity;
                    baseActivity.fillPlayList();
                    sendBroadcast(new Intent(ConstantValues.UPDATE_PLAYLIST_ACTION));
                }
            }

            try {
                Thread.sleep(ONE_CYCLE_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mHeartbeatElapsedTime += ONE_CYCLE_TIME;
            mServerInfoCheckElapsedTime += ONE_CYCLE_TIME;
        }
    }

    private void doHeartbeat() {
        LogFileUtil.write("开始自动上报心跳");
        AppApi.heartbeat(this, this);
    }

    private void doShowMiniProgramQRCode(){
        LogFileUtil.write("开机立刻调用一次展示小程序接口：当前小程序码状态："+Session.get(this).isShowMiniProgramIcon());

        AppApi.getScreenIsShowQRCode(this,this);


    }

        private void getNettyBalancingInfo(){
            HashMap<String,String> params = new HashMap<>();
            params.put("box_mac",session.getEthernetMac());
            params.put("req_id",System.currentTimeMillis()+"");
            AppApi.getNettyBalancingInfo(this,this,params);
        }

    private void monitorDownloadSpeed(){
        mHandler.postDelayed(mRunnable,0);

    }



    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(mRunnable,count*1000);
            Message msg = mHandler.obtainMessage();
            msg.what = 1;
            msg.arg1 = getNetSpeed();
            mHandler.sendMessage(msg);
        }
    };
    private int getNetSpeed(){
        long traffic_data = TrafficStats.getTotalRxBytes() - total_data;
        total_data = TrafficStats.getTotalRxBytes();
        return (int)traffic_data /count ;
    }


    private void reportMediaDetail() {
        reportCurrent();

        Session session = Session.get(this);
        if (!TextUtils.isEmpty(session.getAdvDownloadPeriod()) && !session.getAdvDownloadPeriod().equals(session.getAdvPeriod())) {
            reportAdvDownload();
        }
        if (!TextUtils.isEmpty(session.getAdsDownloadPeriod()) && !session.getAdsDownloadPeriod().equals(session.getAdsPeriod())) {
            reportAdsDownload();
        }
        if (!TextUtils.isEmpty(session.getProDownloadPeriod()) && !session.getProDownloadPeriod().equals(session.getProPeriod())) {
            reportProDownload();
        }
    }

    private void reportProDownload() {
        reportDownloadDataByType(ConstantValues.PRO_DATA_PATH);
    }

    private void reportAdvDownload() {
        reportDownloadDataByType(ConstantValues.ADV_DATA_PATH);
    }

    private void reportAdsDownload() {
        reportDownloadDataByType(ConstantValues.ADS_DATA_PATH);
    }

    private void reportDownloadDataByType(String filePath) {
        ArrayList<MediaDownloadBean> medias = new ArrayList<>();
        DownloadDetailRequestBean requestBean = new DownloadDetailRequestBean();
        String jsonData = FileUtils.read(filePath);
        if (!TextUtils.isEmpty(jsonData)) {
            ProgramBean programBean = null;
            try {
                if (ConstantValues.ADS_DATA_PATH.equals(filePath) || ConstantValues.ADV_DATA_PATH.equals(filePath)) {
                    // 宣传片和广告
                    ProgramBeanResult programBeanResult = new Gson().fromJson(jsonData, new TypeToken<ProgramBeanResult>() {
                    }.getType());
                    if (programBeanResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS && programBeanResult.getResult() != null) {
                        programBean = programBeanResult.getResult();
                    }
                } else {
                    // 节目单
                    SetBoxTopResult setBoxTopResult = new Gson().fromJson(jsonData, new TypeToken<SetBoxTopResult>() {
                    }.getType());
                    if (setBoxTopResult.getCode() == AppApi.HTTP_RESPONSE_STATE_SUCCESS) {
                        if (setBoxTopResult.getResult() != null && setBoxTopResult.getResult().getPlaybill_list() != null) {
                            //该集合包含三部分数据，1:真实节目，2：宣传片占位符.3:广告占位符
                            for (ProgramBean item : setBoxTopResult.getResult().getPlaybill_list()) {
                                if (ConstantValues.PRO.equals(item.getVersion().getType())) {
                                    programBean = item;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (programBean != null && programBean.getVersion() != null) {
                if (programBean.getMedia_lib() != null && programBean.getMedia_lib().size() > 0) {
                    for (MediaLibBean bean : programBean.getMedia_lib()) {
                        MediaDownloadBean mediaDownloadBean = new MediaDownloadBean();
                        mediaDownloadBean.setMedia_id(bean.getVid());

                        String selection = null;
                        String[] selectionArgs = null;
                        if (ConstantValues.ADS_DATA_PATH.equals(filePath) || ConstantValues.ADV_DATA_PATH.equals(filePath)) {
                            mediaDownloadBean.setOrder(bean.getLocation_id());
                            selection = DBHelper.MediaDBInfo.FieldName.VID
                                    + "=? and "
                                    + DBHelper.MediaDBInfo.FieldName.LOCATION_ID
                                    + "=?";
                            selectionArgs = new String[]{bean.getVid(), bean.getLocation_id() + ""};
                        } else {
                            mediaDownloadBean.setOrder(bean.getOrder() + "");
                            selection = DBHelper.MediaDBInfo.FieldName.VID
                                    + "=? and "
                                    + DBHelper.MediaDBInfo.FieldName.ADS_ORDER
                                    + "=?";
                            selectionArgs = new String[]{bean.getVid(), bean.getOrder() + ""};
                        }

                        List<MediaLibBean> list = null;
                        if (ConstantValues.ADS_DATA_PATH.equals(filePath)) {
                            list = DBHelper.get(this).findNewAdsByWhere(selection, selectionArgs);
                        } else {
                            list = DBHelper.get(this).findNewPlayListByWhere(selection, selectionArgs);
                        }
                        if (list != null && list.size() >= 1) {
                            mediaDownloadBean.setState(1);
                        } else {
                            mediaDownloadBean.setState(0);
                        }
                        medias.add(mediaDownloadBean);
                    }
                }
                requestBean.setList(medias);

                requestBean.setPeriod(programBean.getVersion().getVersion());
                int type = 0;
                switch (programBean.getVersion().getType()) {
                    case ConstantValues.ADS:
                        type = 1;
                        break;
                    case ConstantValues.ADV:
                        type = 3;
                        break;
                    case ConstantValues.PRO:
                        type = 2;
                        break;
                }
                AppApi.reportDownloadList(this, this, type, requestBean);
            }
        }
    }

    private void reportCurrent() {
        ArrayList<MediaLibBean> list = new ArrayList<>();
        AppUtils.fillPlaylist(this, list, 2);
        if (!TextUtils.isEmpty(Session.get(this).getProPeriod())) {
            PlaylistDetailRequestBean playlistDetailRequestBean = new PlaylistDetailRequestBean();
            playlistDetailRequestBean.setMenu_num(Session.get(this).getProPeriod());
            ArrayList<MediaPlaylistBean> playlist = new ArrayList<>();
            for (MediaLibBean media : list) {
                if (!TextUtils.isEmpty(media.getVid())) {
                    MediaPlaylistBean bean = new MediaPlaylistBean();
                    bean.setMedia_id(media.getVid());
                    bean.setType(media.getType());
                    bean.setOrder(media.getOrder());
                    playlist.add(bean);
                }
            }
            playlistDetailRequestBean.setList(playlist);

            AppApi.reportPlaylist(this, this, playlistDetailRequestBean);
        }
    }

    private void httpGetIp() {
        LogUtils.w("HeartbeatService 将发HTTP请求去发现小平台信息");
        LogFileUtil.write("HeartbeatService 将发HTTP请求去发现小平台信息");
        AppApi.getSpIp(this, this);
    }


    private void handleServerIp(ServerInfo serverInfo) {
        if (serverInfo != null && !TextUtils.isEmpty(serverInfo.getServerIp()) && serverInfo.getNettyPort() > 0 && serverInfo.getCommandPort() > 0 && serverInfo.getDownloadPort() > 0 &&
                (Session.get(this).getServerInfo() == null || Session.get(this).getServerInfo().getSource() != 1)) {
            LogUtils.w("HeartbeatService 将使用HTTP拿到的信息重置小平台信息");
            LogFileUtil.write("HeartbeatService 将使用HTTP拿到的信息重置小平台信息");
            serverInfo.setSource(2);
            if (serverInfo.getServerIp().contains("*")) {
                serverInfo.setServerIp(serverInfo.getServerIp().split("\\*")[0]);
            }
            Session.get(this).setServerInfo(serverInfo);
            AppApi.resetSmallPlatformInterface(this);

            // 重设NettyClient ip、端口号
            // NettyClient.get() != null意味着在MainActivity已经初始化Netty并开始连接
            if (NettyClient.get() != null) {
                NettyClient.get().setServer(serverInfo.getNettyPort(), serverInfo.getServerIp());
            } else {
                Intent intent = new Intent(this, MessageService.class);
                startService(intent);
            }
        }
    }

    private Runnable mNetworkDetectionRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtils.d("NetworkDetectionRunnable is run.");

            double internetLatency = getLatency("www.baidu.com");
            double intranetLatency = -1;
            if (Session.get(HeartbeatService.this).getServerInfo() != null) {
                intranetLatency = getLatency(Session.get(HeartbeatService.this).getServerInfo().getServerIp());
            }

            AppApi.postNetstat(HeartbeatService.this, HeartbeatService.this,
                    intranetLatency == -1 ? "" : "" + intranetLatency, internetLatency == -1 ? "" : "" + internetLatency);
        }

        private double getLatency(String address) {
            LogUtils.d("address is " + address);
            double latency = -1;

            Process process = null;
            InputStream is = null;
            BufferedReader reader = null;
            try {
                process = Runtime.getRuntime().exec("ping -c 10 -i 0.2 -s 56 " + address);
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String str = null;
                while ((str = reader.readLine()) != null) {
                    if (str.contains("rtt ")) {
                        LogUtils.d(str);
                        String speedStr = str.split(" = ")[1].split(",")[0];
                        String unitStr = speedStr.split(" ")[1];
                        double min = Double.parseDouble(speedStr.split(" ")[0].split("/")[0]);
                        double avg = Double.parseDouble(speedStr.split(" ")[0].split("/")[1]);
                        double max = Double.parseDouble(speedStr.split(" ")[0].split("/")[2]);
                        double mdev = Double.parseDouble(speedStr.split(" ")[0].split("/")[3]);

                        if ("ms".equals(unitStr)) {
                            latency = avg;
                        } else if ("s".equals(unitStr)) {
                            latency = avg * 1000;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (process != null) {
                    try {
                        process.destroy();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return latency;
        }
    };

    @Override
    public void onSuccess(AppApi.Action method, Object obj) {
        switch (method) {
            case CP_GET_HEARTBEAT_PLAIN:
                LogFileUtil.write("自动上报心跳成功。 " + obj);
                break;
            case SP_POST_NETSTAT_JSON:
                LogUtils.d("postNetstat success");
                break;
            case CP_GET_SP_IP_JSON:
                LogUtils.w("HeartbeatService HTTP接口发现小平台信息");
                LogFileUtil.write("HeartbeatService HTTP接口发现小平台信息");
                if (obj instanceof ServerInfo) {
                    handleServerIp((ServerInfo) obj);
                }
                break;
            case CP_POST_DOWNLOAD_LIST_JSON:
                LogUtils.d("上报下载列表成功");
                break;
            case CP_POST_PLAY_LIST_JSON:
                LogUtils.d("上报播放列表成功");
                break;
            case CP_MINIPROGRAM_FORSCREEN_JSON:
                if (obj instanceof String){
                    try {
                        String info = (String)obj;
                        JSONObject jsonObject = new JSONObject(info);
                        int is_sapp_forscreen =jsonObject.getInt("is_sapp_forscreen");
                        if (is_sapp_forscreen==1){
                            LogFileUtil.write("开始立刻调用小程序码接口返回成功，启动小程序NETTY服务");
                            Log.d("HeartbeatService","showMiniProgramIcon(true)");
                            Session.get(HeartbeatService.this).setShowMiniProgramIcon(true);
                            if (!session.isHeartbeatMiniNetty()){
                                Log.d("HeartbeatService","startMiniProgramNettyService");
                                startMiniProgramNettyService();
                            }
                        }else{
                            Session.get(HeartbeatService.this).setShowMiniProgramIcon(false);
                        }
                        if (jsonObject.has("support_netty_balance")){
                            int support_netty_balance = jsonObject.getInt("support_netty_balance");
                            int local_support_netty_balance = session.getLocalSupportNettyBalance();
                            if (support_netty_balance!=local_support_netty_balance){
                                session.setLocalSupportNettyBalance(support_netty_balance);
                                if (1==support_netty_balance){

                                    session.setSupportNettyBalance(true);
                                }else{
                                    session.setSupportNettyBalance(false);
                                }
                                startMiniProgramNettyService();
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
                break;
            case CP_GET_NETTY_BALANCING_FORM:
                if (obj instanceof NettyBalancingResult){
                    boolean start = false;
                    NettyBalancingResult netty = (NettyBalancingResult)obj;
                    if (!TextUtils.isEmpty(netty.getResult())){
                        String url = netty.getResult().split(":")[0];
                        String port = netty.getResult().split(":")[1];
                        if (TextUtils.isEmpty(session.getNettyUrl())||!url.equals(session.getNettyUrl())){
                            start=true;
                        }
                        if (!TextUtils.isEmpty(url)){
                            session.setNettyUrl(url);
                        }
                        if (!TextUtils.isEmpty(port)){
                            session.setNettyPort(Integer.valueOf(port));
                        }
                        session.setGetNettyBalancing(true);
                        if (start||!session.isHeartbeatMiniNetty()){
                            startMiniProgramNettyService();
                        }
                    }
                }
                break;
        }
    }

    public void startMiniProgramNettyService(){
        LogFileUtil.write("HeartbeatService startMiniProgramNettyService");
        Intent intent = new Intent(this, MiniProgramNettyService.class);
        startService(intent);
    }

    @Override
    public void onError(AppApi.Action method, Object obj) {
        switch (method) {
            case CP_GET_HEARTBEAT_PLAIN:
                LogFileUtil.write("自动上报心跳失败。 " + obj);
                break;
            case SP_POST_NETSTAT_JSON:
                LogUtils.d("postNetstat failed");
                break;
            case CP_GET_SP_IP_JSON:
                LogUtils.w("HeartbeatService HTTP接口发现小平台信息失败");
                LogFileUtil.write("HeartbeatService HTTP接口发现小平台信息失败");
                break;
            case CP_POST_DOWNLOAD_LIST_JSON:
                LogUtils.d("上报下载列表失败");
                break;
            case CP_POST_PLAY_LIST_JSON:
                LogUtils.d("上报播放列表失败");
                break;
            case CP_GET_NETTY_BALANCING_FORM:
                session.setGetNettyBalancing(true);
                break;
        }
    }

    @Override
    public void onNetworkFailed(AppApi.Action method) {
        switch (method) {
            case CP_GET_HEARTBEAT_PLAIN:
                LogFileUtil.write("自动上报心跳失败，网络异常");
                break;
            case SP_POST_NETSTAT_JSON:
                LogUtils.d("postNetstat failed");
                break;
            case CP_GET_SP_IP_JSON:
                LogUtils.w("HeartbeatService HTTP接口发现小平台信息失败");
                LogFileUtil.write("HeartbeatService HTTP接口发现小平台信息失败");
                break;
            case CP_POST_DOWNLOAD_LIST_JSON:
                LogUtils.d("上报下载列表失败，网络异常");
                break;
            case CP_POST_PLAY_LIST_JSON:
                LogUtils.d("上报播放列表失败，网络异常");
                break;
            case CP_GET_NETTY_BALANCING_FORM:
                session.setGetNettyBalancing(true);
                break;
        }
    }
}
