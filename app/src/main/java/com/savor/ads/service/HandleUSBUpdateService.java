package com.savor.ads.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.bean.BoiteBean;
import com.savor.ads.bean.BoxInitBean;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.bean.RoomBean;
import com.savor.ads.bean.SetTopBoxBean;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.log.LogUploadService;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.ShowMessage;
import com.savor.ads.utils.TechnicalLogReporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.savor.ads.utils.AppUtils.StorageFile.log;
import static com.savor.ads.utils.AppUtils.StorageFile.loged;

/**
 * Processing the U disk to update the video content logic of the set-top box
 * U disk updates are relatively less complex than network updates,
 * and do not involve multiple interfaces that need only a JSON file to do the corresponding.
 * Created by bichao on 2017/11/29.
 */

public class HandleUSBUpdateService extends Service {

    private Context context;
    private Session session;
    /**节目数据*/
    private SetTopBoxBean setTopBoxBean;

    /**
     * 接口返回的盒子信息
     */
    private BoxInitBean boxInitBean;
    /**酒楼信息*/
    private BoiteBean boiteBean;
    /**包间信息*/
    private RoomBean roomBean;
    private DBHelper dbHelper;
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();


    private boolean isProduceLog = false;

    private boolean isProCompleted = false;  //节目是否下载完毕
    private String mProCompletedPeriod;
    private File cfgFile;
    private List<String> cfgList = new ArrayList<>();
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        this.session = Session.get(this);
        dbHelper = DBHelper.get(context);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("==========into onStartCommand method=========");
        LogFileUtil.write("HandleUSBUpdateService onStartCommand");
        cfgFile = (File) intent.getSerializableExtra("cfgFile");
        new Thread(new Runnable() {
            @Override
            public void run() {
                cfgList = FileUtils.readFile(cfgFile);
                if (cfgList!=null&&cfgList.size()>0){
                    for (String str: cfgList){
                        switch (str){
                            case ConstantValues.USB_FILE_HOTEL_GET_CHANNEL:

                                break;
                            case ConstantValues.USB_FILE_HOTEL_SET_CHANNEL:

                                break;
                            case ConstantValues.USB_FILE_HOTEL_GET_LOG:
                                getLogToUSBDriver(log);
                                break;
                            case ConstantValues.USB_FILE_HOTEL_GET_LOGED:
                                getLogToUSBDriver(loged);
                                break;
                            case ConstantValues.USB_FILE_HOTEL_UPDATE_MEIDA:
                                handleProgramMediaData();
                                break;
                            case ConstantValues.USB_FILE_HOTEL_UPDATE_APK:
                                toUpdateApk();
                                break;
                        }
                    }
                }
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 处理节目单视频
     */
   private void handleProgramMediaData(){
       String jsonPath = session.getUsbPath()
               +File.separator
               + ConstantValues.USB_FILE_HOTEL_PATH
               + File.separator
               + session.getBoiteId()
               + File.separator
               + ConstantValues.USB_FILE_HOTEL_UPDATE_JSON;
       File jsonFile = new File(jsonPath);
       if (!jsonFile.exists()) {
        return;
       }
        String jsonContent = FileUtils.readFileToStr(jsonFile);
        if (!TextUtils.isEmpty(jsonContent)){
            setTopBoxBean = gson.fromJson(jsonContent, new TypeToken<SetTopBoxBean>() {
            }.getType());
        }
        if (setTopBoxBean==null){
            return;
        }
        if (setTopBoxBean.getPeriod().equals(session.getProPeriod())){
            ShowMessage.showToast(context,"当前期号和U盘期号一致");
            return;
        }
        if (setTopBoxBean.getRoom()!=null){
            roomBean = setTopBoxBean.getRoom();
        }
        if (setTopBoxBean.getBoite()!=null){
            boiteBean = setTopBoxBean.getBoite();
        }
        List<MediaLibBean> mediaLibBeans = setTopBoxBean.getPlaylist();
        if (mediaLibBeans!=null&&mediaLibBeans.size()>0){
            dbHelper.deleteAllData(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST);
            String usbMediaRootPath = session.getUsbPath()
                    +File.separator
                    +ConstantValues.USB_FILE_HOTEL_MEDIA_PATH
                    +File.separator;
            String usbAdvRootPath = session.getUsbPath()
                    +File.separator
                    +ConstantValues.USB_FILE_HOTEL_PATH
                    +File.separator
                    +session.getBoiteId()
                    +File.separator
                    +ConstantValues.USB_FILE_HOTEL_UPDATE_ADV
                    +File.separator;
            String localRootPath = AppUtils.getFilePath(context, AppUtils.StorageFile.media);
            int completedCount = 0;     // 下载成功个数
            while (completedCount!=mediaLibBeans.size()){
                for (MediaLibBean bean:mediaLibBeans){
                    String localPath = null;
                    String usbMeidaPath = null;
                    if (bean.getType().equals(ConstantValues.ADV)){
                        localPath = localRootPath+bean.getName();
                        usbMeidaPath = usbAdvRootPath+bean.getName();

                    }else{
                        localPath = localRootPath+bean.getChinese_name()+".mp4";
                        usbMeidaPath = usbMediaRootPath + bean.getChinese_name()+".mp4";
                    }
                    try {
                        boolean isDownloaded = false;
                        if (isDownloadCompleted(localPath,bean.getMd5())){
                            isDownloaded = true;

                        }else{
                            new File(localPath).delete();
                            FileUtils.copyFile(usbMeidaPath,localPath);
                        }
                        if (isDownloaded){
                            PlayListBean play = new PlayListBean();
                            play.setPeriod(bean.getPeriod());
                            play.setDuration(bean.getDuration());
                            play.setMd5(bean.getMd5());
                            play.setVid(bean.getVid());
                            play.setMedia_name(bean.getName());
                            play.setMedia_type(bean.getType());
                            play.setOrder(bean.getOrder());
                            play.setSurfix(bean.getSurfix());
                            play.setLocation_id(bean.getLocation_id());
                            play.setMediaPath(localPath);
                            // 插库成功，completedCount+1
                            if (dbHelper.insertOrUpdateNewPlayListLib(play, -1)) {
                                completedCount++;
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        ShowMessage.showToast(context,usbMeidaPath+"出错");
                    }

                }
            }
            if (completedCount==mediaLibBeans.size()){
                dbHelper.copyTableMethod(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, DBHelper.MediaDBInfo.TableName.PLAYLIST);
                session.setProPeriod(setTopBoxBean.getPeriod());
                session.setAdvPeriod(setTopBoxBean.getPeriod());
                session.setAdsPeriod(setTopBoxBean.getPeriod());
                notifyToPlay();
            }
        }
   }

    /**
     * 文件是否下载完成判定
     *
     * @param path
     * @param md5
     * @return
     * @throws IOException
     */
    private boolean isDownloadCompleted(String path, String md5) throws Exception {
        if (AppUtils.isFileExist(path)) {
            String realMd5 = AppUtils.getMD5Method(new File(path));
            if (!TextUtils.isEmpty(md5) && md5.equals(realMd5)) {
                return true;
            }
            return false;
        } else {
            return false;
        }
    }

    private void notifyToPlay() {
        if (fillPlayList()) {
            LogUtils.d("发送广告下载完成广播");
            sendBroadcast(new Intent(ConstantValues.ADS_DOWNLOAD_COMPLETE_ACCTION));
        }
    }

    /**
     * 生成播放列表
     * @return
     */
    private boolean fillPlayList() {
        LogUtils.d("开始fillPlayList");
        ArrayList<PlayListBean> playList = dbHelper.getOrderedPlayList();

        if (playList != null && !playList.isEmpty()) {
            for (int i = 0; i < playList.size(); i++) {
                PlayListBean bean = playList.get(i);

                File mediaFile = new File(bean.getMediaPath());
                boolean fileCheck = false;
                if (!TextUtils.isEmpty(bean.getMd5()) &&
                        !TextUtils.isEmpty(bean.getMediaPath()) &&
                        mediaFile.exists()) {
                    if (!bean.getMd5().equals(AppUtils.getMD5Method(mediaFile))) {
                        fileCheck = true;

                        TechnicalLogReporter.md5Failed(this, bean.getVid());
                    }
                } else {
                    fileCheck = true;
                }

                if (fileCheck) {
                    LogUtils.e("媒体文件校验失败! vid:" + bean.getVid());
                    // 校验失败时将文件路径置空，下面会删除掉为空的项
                    bean.setMediaPath(null);
                    if (mediaFile.exists()) {
                        mediaFile.delete();
                    }

                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,
                            DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                            new String[]{bean.getPeriod(), bean.getVid(), bean.getMedia_type()});
                    dbHelper.deleteDataByWhere(DBHelper.MediaDBInfo.TableName.PLAYLIST,
                            DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                            new String[]{bean.getPeriod(), bean.getVid(), bean.getMedia_type()});
                }
            }
        }

        if (playList != null && !playList.isEmpty()) {
            ArrayList<PlayListBean> list = new ArrayList<>();
            for (PlayListBean bean : playList) {
                if (!TextUtils.isEmpty(bean.getMediaPath())) {
                    list.add(bean);
                }
            }
            GlobalValues.PLAY_LIST = list;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 将日志拷贝到U盘
     */
    private void getLogToUSBDriver(AppUtils.StorageFile storageFile){
        File[] files = new File(AppUtils.getFilePath(context, storageFile)).listFiles();
        if (files!=null&&files.length>0){
            String usbLogPath = session.getUsbPath()
                    + File.separator
                    + ConstantValues.USB_FILE_LOG_PATH
                    + File.separator;
            for (File file:files){
                if (!file.getName().endsWith(".blog")) {
                    file.delete();
                    continue;
                }
                String name = file.getName();
                String path = file.getPath();
                if (file.isFile()){
                    String[] split = name.split("_");
                    if (split.length!=3){
                        continue;
                    }
                    String time = split[1].substring(0, 10);
                    if (time.equals(AppUtils.getCurTime("yyyyMMddHH"))) {
                        continue;
                    }
                    String archivePath = path + ".zip";
                    File zipFile = new File(archivePath);
                    try {
                        AppUtils.zipFile(file, zipFile, zipFile.getName());
                        if (zipFile.exists()){

                           String usbLogFilePath = usbLogPath + zipFile.getName();
                            FileUtils.copyFile(zipFile.getPath(),usbLogFilePath);
                            if (new File(usbLogFilePath).exists()){
                                zipFile.delete();
                                if (storageFile.equals(AppUtils.StorageFile.log)){
                                    File logedFile = new File(AppUtils.getFilePath(context, AppUtils.StorageFile.loged));
                                    file.renameTo(new File(logedFile + file.getName()));
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }

        }

    }

    /**
     * 更新APK版本
     */
    private void toUpdateApk(){

    }
}
