package com.savor.ads.utils;

import android.content.Context;
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

    private static boolean updateRom() {

        boolean isflag = false;
        try {
            Process proc = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(proc.getOutputStream());
            if (dos != null) {
                try {

                    dos.writeBytes("cat /mnt/sdcard/update_signed.zip > /cache/download/update_signed.zip\n");
                    dos.flush();
                    dos.writeBytes("exit\n");
                    dos.flush();
                    isflag = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    dos.close();
                }
            }
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            proc.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isflag;

    }

    private static boolean updateApk() {
        boolean isflag = false;
        try {
            Process proc = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(proc.getOutputStream());
            if (dos != null) {
                try {
                    dos.writeBytes("mount -o remount rw /system\n");
                    dos.flush();
                    dos.writeBytes("cat /mnt/sdcard/updateapksamples.apk > /system/app/1.apk\n");
                    dos.flush();
                    dos.writeBytes("rm -r /mnt/sdcard/updateapksamples.apk\n");
                    dos.flush();
                    dos.writeBytes("mv /system/app/1.apk /system/app/savormedia.apk\n");
                    dos.flush();
                    dos.writeBytes("reboot\n");
                    dos.flush();
                    isflag = true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    dos.close();
                }
            }
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            proc.destroy();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return isflag;

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
                if (obj instanceof FileDownProgress) {
                    FileDownProgress fs = (FileDownProgress) obj;
                    long now = fs.getNow();
                    long total = fs.getTotal();

                } else if (obj instanceof File) {
                    File f = (File) obj;
                    byte[] fRead;
                    String md5Value = null;
                    try {
                        fRead = FileUtils.readFileToByteArray(f);
                        md5Value = AppUtils.getMD5(fRead);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    //比较本地文件版本是否与服务器文件一致，如果一致则启动安装
                    String fileName = f.getName();
                    String filePath = f.getPath();
                    if (fileName.equals(AppApi.ROM_DOWNLOAD_FILENAME)) {
                        if (md5Value != null && md5Value.equals(upgradeInfo.getRomMd5())) {
                            //升级ROM
                            updateRom();
                        }
                    } else if (fileName.equals(AppApi.APK_DOWNLOAD_FILENAME)) {
                        if (md5Value != null && md5Value.equals(upgradeInfo.getApkMd5())) {
                            //升级APK
                            updateApk();
                        }
                    }


                }
                break;
//				case SP_GET_LOGO_DOWN:
//					if (obj instanceof FileDownProgress){
//						FileDownProgress fs = (FileDownProgress) obj;
//						long now = fs.getNow();
//						long total = fs.getTotal();
//
//					}else if (obj instanceof File) {
//						File f = (File) obj;
//						byte[] fRead;
//						String md5Value = AppUtils.getMD5Method(f);
//						//比较本地文件版本是否与服务器文件一致，如果一致则启动安装
//						if (md5Value!=null&&md5Value.equals(upgradeInfo.getLogo_md5())){
//							ShellUtils.updateLogoPic(f.getAbsolutePath());
//						}
//					}
//					break;
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
