package com.savor.ads.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;


import com.savor.ads.bean.ServerInfo;
import com.savor.ads.bean.UpgradeInfo;
import com.savor.ads.bean.UpgradeResult;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.okhttp.coreProgress.download.ProgressDownloader;
import com.savor.ads.oss.OSSValues;


import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class UpdateUtil implements ApiRequestListener {

    private UpgradeInfo upgradeInfo = null;
    private static Context mContext = null;
    Session session = null;
    ServerInfo serverInfo;

    /**
     * 更新apk
     *
     * @param context
     */
    public UpdateUtil(Context context) {
        mContext = context;
        session = Session.get(mContext);
        serverInfo = session.getServerInfo();
        toCheckServerWhetherUpgrade();
    }

    private void toCheckServerWhetherUpgrade() {
        if (serverInfo != null) {
            AppApi.upgradeInfo(mContext, UpdateUtil.this, session.getVersionCode());
        }
    }

    private static void updateRom(File file) {

    }
    private static void updateApkNotSystem(File file){
        if (file.length() <= 0) {
            file.delete();
            LogFileUtil.writeException(new Throwable("apk update fatal, updateapksamples.apk length is 0"));
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setDataAndType(Uri.parse("file://" + file.getAbsolutePath()),
                "application/vnd.android.package-archive");
        mContext.startActivity(i);
    }
    private static void updateApk(File file) {
        if (file.length() <= 0) {
            file.delete();
            LogFileUtil.writeException(new Throwable("apk update fatal, updateapksamples.apk length is 0"));
            return;
        }

        boolean isflag = false;
        Process proc = null;
        String tempPath = "/system/priv-app/savormedia/temp.apk";
        String targetPath = "/system/priv-app/savormedia/savormedia.apk";
        try {
            proc = Runtime.getRuntime().exec("su");
            try {
                DataOutputStream dos = new DataOutputStream(proc.getOutputStream());
                dos.writeBytes("mount -o remount,rw /system\n");
                dos.flush();

                String catCommand = "cat " + file.getPath() + " > " + tempPath + "\n";
                dos.writeBytes(catCommand);
                dos.flush();
                Thread.sleep(2000);

                file.delete();

                File file1 = new File(tempPath);
                if (file1.length() > 0) {

                    dos.writeBytes("mv " + tempPath + " " + targetPath + "\n");
                    dos.flush();
                    Thread.sleep(1000);

                    dos.writeBytes("chmod 755 " + targetPath + "\n");
                    dos.flush();
                    Thread.sleep(1000);
                    isflag = true;
                } else {
                    file1.delete();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (proc != null) {
                try {
                    proc.destroy();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (isflag) {
            ShellUtils.reboot();
        }
    }

    @Override
    public void onSuccess(AppApi.Action method, Object obj) {
        switch (method) {
            case SP_GET_UPGRADE_INFO_JSON:
                if (obj instanceof UpgradeInfo) {
                    upgradeInfo = (UpgradeInfo) obj;
                    handleUpgradeInfo();
                }
                break;
            case SP_GET_UPGRADEDOWN:
                if (obj instanceof File) {
                    File f = (File) obj;
                    byte[] fRead;
                    String md5Value = null;
                    try {
                        fRead = FileUtils.readFileToByteArray(f);
                        md5Value = AppUtils.getMD5(fRead);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //比较本地文件MD5是否与服务器文件一致，如果一致则启动安装
                    String fileName = f.getName();
                    if (AppApi.ROM_DOWNLOAD_FILENAME.equals(fileName)) {
                        if (md5Value != null && md5Value.equals(upgradeInfo.getRomMd5())) {
                            //升级ROM
                            updateRom(f);
                        }
                    } else if (AppApi.APK_DOWNLOAD_FILENAME.equals(fileName)) {
                        if (md5Value != null && md5Value.equals(upgradeInfo.getApkMd5())) {
                            //升级APK
                            updateApk(f);
                        }
                    }
                }
                break;
        }
    }

    private void handleUpgradeInfo() {
        if (upgradeInfo == null) {
            return;
        }

        if (serverInfo != null && !TextUtils.isEmpty(upgradeInfo.getRomUrl())) {
            TechnicalLogReporter.romUpdate(mContext, upgradeInfo.getNewestRomVersion());
            AppApi.downVersion(serverInfo.getDownloadUrl() + upgradeInfo.getRomUrl(), mContext, this, 1);
        }
        if (serverInfo != null && !TextUtils.isEmpty(upgradeInfo.getApkUrl())) {
            ShowMessage.showToast(mContext, "发现新版本，开始下载");
            TechnicalLogReporter.apkUpdate(mContext, upgradeInfo.getNewestApkVersion());
            AppApi.downVersion(serverInfo.getDownloadUrl() + upgradeInfo.getApkUrl(), mContext, this, 2);
        }
    }


    @Override
    public void onError(AppApi.Action method, Object obj) {

    }

    @Override
    public void onNetworkFailed(AppApi.Action method) {

    }
}
