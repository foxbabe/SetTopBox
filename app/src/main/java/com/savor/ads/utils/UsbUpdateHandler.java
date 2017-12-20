package com.savor.ads.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.bean.AtvProgramInfo;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.bean.SetTopBoxBean;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.utils.tv.TvOperate;
import com.savor.tvlibrary.AtvChannel;
import com.savor.tvlibrary.TVOperatorFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.savor.ads.utils.AppUtils.StorageFile.log;
import static com.savor.ads.utils.AppUtils.StorageFile.loged;

/**
 * Created by zhang.haiqiang on 2017/12/17.
 */

public class UsbUpdateHandler {
    Context mContext;
    ProgressCallback mCallback;
    List<String> cfgList;
    /**
     * 节目数据
     */
    private SetTopBoxBean setTopBoxBean;
    private final Session mSession;

    public UsbUpdateHandler(Context context, List<String> cfgList, ProgressCallback callback) {
        mContext = context;
        mCallback = callback;
        this.cfgList = cfgList;

        mSession = Session.get(context);
    }

    public interface ProgressCallback {
        void onStart(int index);

        void onActionComplete(int index, boolean success, String msg);

        void onAllComplete();

        void onActionProgress(int index,String msg);
    }

    public void execute() {
        if (cfgList != null && cfgList.size() > 0) {
            boolean haveCfg = initConfig();
            if (!haveCfg){
                return;
            }
            for (int i = 0; i < cfgList.size(); i++) {
                boolean isKnownAction = true;
                String str = cfgList.get(i);

                boolean isSuccess = false;
                String msg = null;
                switch (str) {
                    case ConstantValues.USB_FILE_HOTEL_GET_CHANNEL:
                        if (mCallback != null) {
                            mCallback.onStart(i);
                        }
                        isKnownAction = true;
                        isSuccess = readChannelList();
                        break;
                    case ConstantValues.USB_FILE_HOTEL_SET_CHANNEL:
                        if (mCallback != null) {
                            mCallback.onStart(i);
                        }
                        isKnownAction = true;
                        isSuccess = writeChannelList();
                        break;
                    case ConstantValues.USB_FILE_HOTEL_GET_LOG:
                        if (mCallback != null) {
                            mCallback.onStart(i);
                        }
                        isKnownAction = true;
                        isSuccess = getLogToUSBDriver(log,i);
                        if (isSuccess){
                            msg = "单机版日志文件提取完成";
                        }else{
                            msg = "单机版日志文件提取失败";
                        }
                        break;
                    case ConstantValues.USB_FILE_HOTEL_GET_LOGED:
                        if (mCallback != null) {
                            mCallback.onStart(i);
                        }
                        isKnownAction = true;
                        isSuccess = getLogToUSBDriver(loged,i);
                        if (isSuccess){
                            msg = "单机版历史日志文件提取完成";
                        }else{
                            msg = "单机版历史日志文件提取失败";
                        }
                        break;
                    case ConstantValues.USB_FILE_HOTEL_UPDATE_MEIDA:
                        if (mCallback != null) {
                            mCallback.onStart(i);
                        }
                        isKnownAction = true;
                        isSuccess = handleProgramMediaData(i);
                        if(isSuccess&&setTopBoxBean!=null&&setTopBoxBean.getPlay_list()!=null){
                            msg = "总共:"+setTopBoxBean.getPlay_list().size()+"个视频，已全部更新完成";
                        }else{
                            msg = "视频更新失败，请重试!!";
                        }
                        break;
                    case ConstantValues.USB_FILE_HOTEL_UPDATE_APK:
                        if (mCallback != null) {
                            mCallback.onStart(i);
                        }
                        isKnownAction = true;
                        isSuccess = updateApk();
                        if (!isSuccess){
                            msg = "应用更新失败！！！";
                        }
                        break;
                    case ConstantValues.USB_FILE_HOTEL_UPDATE_LOGO:
                        if (mCallback != null) {
                            mCallback.onStart(i);
                        }
                        isKnownAction = true;
                        isSuccess = updateLogo();
                        if (isSuccess){
                            msg = "LGGO更新成功,重启生效！！！";
                        }else{
                            msg = "LGGO更新失败,请联系热点张海强！！！";
                        }
                        break;
                    default:
                        isKnownAction = false;
                        break;
                }

                if (isKnownAction && mCallback != null) {
                    mCallback.onActionComplete(i, isSuccess, msg);
                }
            }

            if (mCallback != null) {
                mCallback.onAllComplete();
            }
        }
    }

    private boolean initConfig(){
        String jsonPath = mSession.getUsbPath() + File.separator +
                ConstantValues.USB_FILE_HOTEL_PATH + File.separator +
                mSession.getBoiteId() + File.separator +
                ConstantValues.USB_FILE_HOTEL_UPDATE_JSON;
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            LogUtils.w("update media but play_list file not exist");
            LogFileUtil.write("update media but play_list file not exist");
            return false;
        }
        String jsonContent = FileUtils.readFileToStr(jsonFile);
        if (!TextUtils.isEmpty(jsonContent)) {
            setTopBoxBean = new Gson().fromJson(jsonContent, new TypeToken<SetTopBoxBean>() {
            }.getType());
        }
        if (setTopBoxBean!=null){
            return true;
        }else{
            return false;
        }
    }

    private boolean readChannelList() {
        boolean isSuccess = true;
        LogUtils.d("start readChannelList");
        try {
            File csvFile = new File(mSession.getUsbPath() +
                    ConstantValues.USB_FILE_HOTEL_PATH + File.separator +
                    mSession.getBoiteId() + File.separator +
                    ConstantValues.USB_FILE_CHANNEL_EDIT_DATA);
            if (csvFile.exists()) {
                csvFile.delete();
            }
            FileWriter fileWriter = new FileWriter(csvFile, true);
            String channelJson;
            if (AppUtils.isMstar()) {
                AtvProgramInfo[] programs = new TvOperate().getAllProgramInfo();
                // 服务器改成返回ChennalNum从1开始，这里统一加1后再上传
                if (programs != null) {
                    LogUtils.d("Got channels count " + programs.length);
                    for (int i = 0; i < programs.length; i++) {
                        AtvProgramInfo program = programs[i];
                        program.setChennalNum(program.getChennalNum() + 1);

                        fileWriter.write(program.getChennalNum() + "," + program.getChannelName() + "," + program.getChennalNum());
                        fileWriter.flush();
                        if (i < programs.length - 1) {
                            fileWriter.write("\r\n");
                            fileWriter.flush();
                        }
                    }
                }

                channelJson = new Gson().toJson(programs);
            } else {
                ArrayList<AtvChannel> programs = TVOperatorFactory.getTVOperator(mContext, TVOperatorFactory.TVType.GIEC).getSysChannels();
                if (programs != null) {
                    LogUtils.d("Got channels count " + programs.size());
                    for (int i = 0; i < programs.size(); i++) {
                        AtvChannel program = programs.get(i);

                        fileWriter.write(program.getChannelNum() + "," + program.getChannelName() + "," + program.getChannelNum());
                        fileWriter.flush();
                        if (i < programs.size() - 1) {
                            fileWriter.write("\r\n");
                            fileWriter.flush();
                        }
                    }
                }

                channelJson = new Gson().toJson(programs);
            }
            FileUtils.write(mSession.getUsbPath() +
                    ConstantValues.USB_FILE_HOTEL_PATH + File.separator +
                    mSession.getBoiteId() + File.separator +
                    ConstantValues.USB_FILE_CHANNEL_RAW_DATA, channelJson);

            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            isSuccess = false;
        }
        return isSuccess;
    }

    private boolean writeChannelList() {
        LogUtils.d("start writeChannelList");
        boolean isSuccess = true;
        File csvFile = new File(mSession.getUsbPath() +
                ConstantValues.USB_FILE_HOTEL_PATH + File.separator +
                mSession.getBoiteId() + File.separator +
                ConstantValues.USB_FILE_CHANNEL_EDIT_DATA);
        File rawFile = new File(mSession.getUsbPath() +
                ConstantValues.USB_FILE_HOTEL_PATH + File.separator +
                mSession.getBoiteId() + File.separator +
                ConstantValues.USB_FILE_CHANNEL_RAW_DATA);
        if (!csvFile.exists()) {
            LogFileUtil.write("Write channel but csv file not exits");
            LogUtils.w("Write channel but csv file not exits");
            return false;
        }
        if (!rawFile.exists()) {
            LogFileUtil.write("Write channel but channel raw file not exits");
            LogUtils.w("Write channel but channel raw file not exits");
            return false;
        }

        String rawStr = FileUtils.read(rawFile.getPath());
        String csvStr = FileUtils.read(csvFile.getPath());
        if (TextUtils.isEmpty(rawStr) || TextUtils.isEmpty(rawStr.trim())) {
            LogFileUtil.write("Write channel but channel raw file is empty");
            LogUtils.w("Write channel but channel raw file is empty");
            return false;
        }
        if (TextUtils.isEmpty(csvStr) || TextUtils.isEmpty(csvStr.trim())) {
            LogFileUtil.write("Write channel but channel csv file is empty");
            LogUtils.w("Write channel but channel csv file is empty");
            return false;
        }

        try {
            if (AppUtils.isMstar()) {
                ArrayList<AtvProgramInfo> programInfos = new Gson().fromJson(rawStr, new TypeToken<ArrayList<AtvProgramInfo>>() {
                }.getType());
                String[] lines = csvStr.split("\r\n");
                if (lines.length > 0) {
                    AtvProgramInfo[] newList = new AtvProgramInfo[lines.length];
                    for (String line : lines) {
                        if (!TextUtils.isEmpty(line)) {
                            String[] columns = line.split(",");
                            if (columns.length == 3) {
                                int originalIndex = Integer.parseInt(columns[0]);
                                int newIndex = Integer.parseInt(columns[2]);
                                programInfos.get(originalIndex - 1).setChannelName(columns[1]);
                                programInfos.get(originalIndex - 1).setChennalNum(newIndex);
                                newList[newIndex - 1] = programInfos.get(originalIndex - 1);
                            }
                        }
                    }
                    new TvOperate().updateProgram(mContext, newList);
                }
            } else {
                ArrayList<AtvChannel> programInfos = new Gson().fromJson(rawStr, new TypeToken<ArrayList<AtvChannel>>() {
                }.getType());
                String[] lines = csvStr.split("\r\n");
                if (lines.length > 0) {
                    AtvChannel[] newList = new AtvChannel[lines.length];
                    for (String line : lines) {
                        if (!TextUtils.isEmpty(line)) {
                            String[] columns = line.split(",");
                            if (columns.length == 3) {
                                int originalIndex = Integer.parseInt(columns[0]);
                                int newIndex = Integer.parseInt(columns[2]);
                                programInfos.get(originalIndex - 1).setChannelName(columns[1]);
                                programInfos.get(originalIndex - 1).setDisplayName(columns[1]);
                                programInfos.get(originalIndex - 1).setChannelNum(newIndex);
                                programInfos.get(originalIndex - 1).setDisplayNumber(columns[2]);
                                newList[newIndex - 1] = programInfos.get(originalIndex - 1);
                            }
                        }
                    }
                    TVOperatorFactory.getTVOperator(mContext, TVOperatorFactory.TVType.GIEC).setAtvChannels(new ArrayList<AtvChannel>(Arrays.asList(newList)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
        }

        return isSuccess;
    }

    /**
     * 处理节目单视频
     * index:更新配置文件中的第几项
     */
    private boolean handleProgramMediaData(int index) {
        boolean isSuccess = true;

        if (setTopBoxBean == null) {
            LogUtils.w("update media but play_list file json format error");
            LogFileUtil.write("update media but play_list file json format error");
            return false;
        }
//        if (setTopBoxBean.getPeriod().equals(mSession.getProPeriod())){
//            ShowMessage.showToast(mContext,"当前期号和U盘期号一致");
//            return;
//        }
        //TODO:包间信息
//        if (setTopBoxBean.getRoom()!=null){
//            roomBean = setTopBoxBean.getRoom();
//        }
//            if (setTopBoxBean.getBoite() != null) {
//                boiteBean = setTopBoxBean.getBoite();
//            }
        List<MediaLibBean> mediaLibBeans = setTopBoxBean.getPlay_list();
        if (mediaLibBeans != null && mediaLibBeans.size() > 0) {
            String usbMediaRootPath = mSession.getUsbPath()
                    + File.separator
                    + ConstantValues.USB_FILE_HOTEL_MEDIA_PATH
                    + File.separator;
            String usbAdvRootPath = mSession.getUsbPath()
                    + File.separator
                    + ConstantValues.USB_FILE_HOTEL_PATH
                    + File.separator
                    + mSession.getBoiteId()
                    + File.separator
                    + ConstantValues.USB_FILE_HOTEL_UPDATE_ADV
                    + File.separator;
            String localRootPath = AppUtils.getFilePath(mContext, AppUtils.StorageFile.media);
            int completedCount = 0;     // 下载成功个数
            while (completedCount != mediaLibBeans.size()) {
                DBHelper.get(mContext).deleteAllData(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST);
                for (int i =0;i<mediaLibBeans.size();i++) {
                    MediaLibBean bean = mediaLibBeans.get(i);
                    String localPath = null;
                    String usbMeidaPath = null;
                    if (bean.getType().equals(ConstantValues.ADV)) {
                        localPath = localRootPath + bean.getName();
                        usbMeidaPath = usbAdvRootPath + bean.getName();

                    } else {
                        localPath = localRootPath + bean.getChinese_name() + "." + bean.getSurfix();
                        usbMeidaPath = usbMediaRootPath + bean.getChinese_name() + "." + bean.getSurfix();
                    }
                    try {
                        mCallback.onActionProgress(index,"总共:"+mediaLibBeans.size()+"个视频，正在更新第"+(i+1)+"个");
                        boolean isDownloaded = false;
                        if (isDownloadCompleted(localPath, bean.getMd5())) {
                            isDownloaded = true;
                        } else {
                            new File(localPath).delete();
                            FileUtils.copyFile(usbMeidaPath, localPath);
                        }
                        if (!isDownloaded&&isDownloadCompleted(localPath, bean.getMd5())){
                            isDownloaded = true;
                        }
                        if (isDownloaded) {
                            PlayListBean play = new PlayListBean();
                            play.setPeriod(setTopBoxBean.getPeriod());
                            play.setDuration(bean.getDuration());
                            play.setMd5(bean.getMd5());
                            play.setVid(bean.getVid());
                            if (bean.getType().equals(ConstantValues.ADV)) {
                                play.setMedia_name(bean.getName());
                            }else{
                                play.setMedia_name(bean.getChinese_name()+"."+bean.getSurfix());
                            }
                            play.setMedia_type(bean.getType());
                            play.setOrder(bean.getOrder());
                            play.setSurfix(bean.getSurfix());
                            play.setLocation_id(bean.getLocation_id());
                            play.setMediaPath(localPath);
                            // 插库成功，completedCount+1
                            if (DBHelper.get(mContext).insertOrUpdateNewPlayListLib(play, -1)) {
                                completedCount++;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ShowMessage.showToast(mContext, usbMeidaPath + "出错");
                    }

                }
            }
            if (completedCount == mediaLibBeans.size()) {
                DBHelper.get(mContext).copyTableMethod(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST, DBHelper.MediaDBInfo.TableName.PLAYLIST);
                mSession.setProPeriod(setTopBoxBean.getPeriod());
                mSession.setAdvPeriod(setTopBoxBean.getPeriod());
                mSession.setAdsPeriod(setTopBoxBean.getPeriod());
                notifyToPlay();
            } else {
                isSuccess = false;
            }
        }

        return isSuccess;
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
            String realMd5 = AppUtils.getEasyMd5(new File(path));
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
            mContext.sendBroadcast(new Intent(ConstantValues.ADS_DOWNLOAD_COMPLETE_ACCTION));
        }
    }

    /**
     * 生成播放列表
     *
     * @return
     */
    private boolean fillPlayList() {
        LogUtils.d("开始fillPlayList");
        ArrayList<PlayListBean> playList = DBHelper.get(mContext).getOrderedPlayList();

        if (playList != null && !playList.isEmpty()) {
            for (int i = 0; i < playList.size(); i++) {
                PlayListBean bean = playList.get(i);

                File mediaFile = new File(bean.getMediaPath());
                boolean fileCheck = false;
                if (!TextUtils.isEmpty(bean.getMd5()) &&
                        !TextUtils.isEmpty(bean.getMediaPath()) &&
                        mediaFile.exists()) {
                    if (!bean.getMd5().equals(AppUtils.getEasyMd5(mediaFile))) {
                        fileCheck = true;

                        TechnicalLogReporter.md5Failed(mContext, bean.getVid());
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

                    DBHelper.get(mContext).deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,
                            DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                            new String[]{bean.getPeriod(), bean.getVid(), bean.getMedia_type()});
                    DBHelper.get(mContext).deleteDataByWhere(DBHelper.MediaDBInfo.TableName.PLAYLIST,
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
    private boolean getLogToUSBDriver(AppUtils.StorageFile storageFile,int index) {
        boolean isSuccess = true;
        List<File> fileList = new ArrayList<>();
        File[] files = new File(AppUtils.getFilePath(mContext, storageFile)).listFiles();
        if (files != null && files.length > 0) {
            for (File file:files){
                String name = file.getName();
                String path = file.getPath();
                if (!name.endsWith(".blog")) {
                    file.delete();
                    continue;
                }
                String[] split = name.split("_");
                if (split.length != 3) {
                    continue;
                }
                String time = split[1].substring(0, 10);
                if (time.equals(AppUtils.getCurTime("yyyyMMddHH"))) {
                    continue;
                }
                if (file.isFile()
                        &&file.getName().split("_").length==3
                        &&file.getName().contains(ConstantValues.STANDALONE)){
                    fileList.add(file);
                }
            }
        }

        if (fileList!=null&&fileList.size()>0){
            String usbLogPath = mSession.getUsbPath()
                    + File.separator
                    + ConstantValues.USB_FILE_LOG_PATH
                    + File.separator;
            for (int i =0;i<fileList.size();i++) {
                File file = fileList.get(i);
                mCallback.onActionProgress(index,"总共:"+fileList.size()+"个日志，正在提取第"+(i+1)+"个");

                String name = file.getName();
                String path = file.getPath();
                if (file.isFile()) {

                    String archivePath = path + ".zip";
                    File zipFile = new File(archivePath);
                    try {
                        AppUtils.zipFile(file, zipFile, zipFile.getName());
                        if (zipFile.exists()) {

                            String usbLogFilePath = usbLogPath + zipFile.getName();
                            FileUtils.copyFile(zipFile.getPath(), usbLogFilePath);
                            if (new File(usbLogFilePath).exists()) {
                                zipFile.delete();
                                if (storageFile.equals(AppUtils.StorageFile.log)) {
                                    String logedPath = AppUtils.getFilePath(mContext, AppUtils.StorageFile.loged);
                                    file.renameTo(new File(logedPath + file.getName()));
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        isSuccess = false;
                    }

                }
            }
        }



        return isSuccess;
    }

    /**
     * 更新APK版本
     */
    private boolean updateApk() {
        boolean isSuccess = true;
        File apkFile = new File(mSession.getUsbPath() +File.separator+
                ConstantValues.USB_FILE_HOTEL_PATH + File.separator +
                setTopBoxBean.getVersion().getApk_name());
        if (!apkFile.exists()) {
            LogFileUtil.write("Update apk but apk file not exits");
            LogUtils.w("Update apk but apk file not exits");
            return false;
        }

        String md5Str = setTopBoxBean.getVersion().getApkMd5();
        if (apkFile.length() <= 0) {
            LogFileUtil.write("Update apk but apk file is empty");
            LogUtils.w("Update apk but apk file is empty");
            return false;
        }
        if (TextUtils.isEmpty(md5Str) || TextUtils.isEmpty(md5Str.trim())) {
            LogFileUtil.write("Update apk but md5 file is empty");
            LogUtils.w("Update apk but md5 file is empty");
            return false;
        }

        try {
            if (md5Str.equals(AppUtils.getMD5(org.apache.commons.io.FileUtils.readFileToByteArray(apkFile)))) {
                if (AppUtils.isMstar()) {
                    isSuccess = UpdateUtil.updateApk(apkFile);
                } else {
                    isSuccess = UpdateUtil.updateApk4Giec(apkFile);
                }
            } else {
                LogFileUtil.write("Update apk but apk md5 value is not match");
                LogUtils.w("Update apk but apk md5 value is not match");
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            isSuccess = false;
        }
        return isSuccess;
    }

    /**
     * 更新酒楼LOGO
     * @return
     */
    private boolean updateLogo(){
        boolean isSuccess = true;
        File logoFile = new File(mSession.getUsbPath() +File.separator+
                ConstantValues.USB_FILE_HOTEL_PATH + File.separator +
                mSession.getBoiteId()+File.separator +
                setTopBoxBean.getVersion().getLogo_name());
        if (!logoFile.exists()) {
            LogFileUtil.write("Update logo but logo file not exits");
            LogUtils.w("Update logo but logo file not exits");
            return false;
        }

        String md5Str = setTopBoxBean.getVersion().getLogo_md5();
        if (logoFile.length() <= 0) {
            LogFileUtil.write("Update logo but logo file is empty");
            LogUtils.w("Update logo but logo file is empty");
            return false;
        }
        if (TextUtils.isEmpty(md5Str) || TextUtils.isEmpty(md5Str.trim())) {
            LogFileUtil.write("Update logo but md5 file is empty");
            LogUtils.w("Update logo but md5 file is empty");
            return false;
        }

        try {
            if (md5Str.equals(AppUtils.getMD5(org.apache.commons.io.FileUtils.readFileToByteArray(logoFile)))) {
                String newPath = AppUtils.getSDCardPath()+"/Pictures/" + logoFile.getName();
                FileUtils.copyFile(logoFile.getAbsolutePath(),newPath);
                File logo = new File(newPath);
                if (md5Str.equals(AppUtils.getMD5(org.apache.commons.io.FileUtils.readFileToByteArray(logo)))){
                    isSuccess = true;
                    mSession.setSplashPath("/Pictures/" + logoFile.getName());
                    mSession.setSplashVersion(setTopBoxBean.getVersion().getLogo_version());
                }else{
                    LogFileUtil.write("copy end logo but logo md5 value is not match");
                    LogUtils.w("copy end logo but logo md5 value is not match");
                    isSuccess = false;
                }
            } else {
                LogFileUtil.write("Update logo but logo md5 value is not match");
                LogUtils.w("Update logo but logo md5 value is not match");
                isSuccess = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess = false;
        }

        return isSuccess;
    }


}
