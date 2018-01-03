package com.savor.ads.utils;

import android.content.Context;
import android.text.TextUtils;


import com.amlogic.update.OtaUpgradeUtils;
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

public class UpdateUtil implements ApiRequestListener, OtaUpgradeUtils.ProgressListener {

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


    /**
     * 升级系统rom
     *
     * @param file
     */
    private void updateRom(final File file) {
        if (file == null || !file.exists()) {
            return;
        }
        final GiecUpdateSystem giecUpdateSystem = new GiecUpdateSystem(mContext);
        final OtaUpgradeUtils otaUpgradeUtils = new OtaUpgradeUtils(mContext);
        final int updateMode = giecUpdateSystem.createAmlScript(file.getAbsolutePath(), false, false);
        if (giecUpdateSystem != null) {
            giecUpdateSystem.write2File();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                giecUpdateSystem.copyBKFile();
                otaUpgradeUtils.setDeleteSource(false);
                otaUpgradeUtils.upgrade(file, UpdateUtil.this, updateMode);
            }
        }).start();
    }

    public static boolean updateApk(File file) {
        if (file.length() <= 0) {
            file.delete();
            LogFileUtil.writeException(new Throwable("apk update fatal, updateapksamples.apk length is 0"));
            return false;
        }

        boolean isflag = false;
        try {
            Process proc = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(proc.getOutputStream());
            try {
                dos.writeBytes("mount -o remount rw /system\n");
                dos.flush();
                String catCommand = "cat " + file.getPath() + " > /system/app/1.apk\n";
                dos.writeBytes(catCommand);
                dos.flush();

                Thread.sleep(5000);
                File file1 = new File("/system/app/1.apk");
                if (file1.length() > 0) {
//                    dos.writeBytes("rm -r " + file.getPath() + "\n");
//                    dos.flush();
                    dos.writeBytes("mv /system/app/1.apk /system/app/savormedia.apk\n");
                    dos.flush();
                    Thread.sleep(1000);
//                        dos.writeBytes("reboot\n");
//                        dos.flush();
                    isflag = true;
                } else {
                    file.delete();
                    file1.delete();
                    LogFileUtil.writeException(new Throwable("apk update fatal, 1.apk length is 0"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                dos.close();
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            proc.destroy();

            if (isflag) {
                ShellUtils.reboot();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return isflag;
    }

    public static boolean updateApk4Giec(File file) {
        boolean isSuccess = true;
        if (file.length() <= 0) {
            file.delete();
            LogFileUtil.writeException(new Throwable("apk update fatal, updateapksamples.apk length is 0"));
            return false;
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

//                file.delete();

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
                            if (!AppUtils.isMstar()) {
                                updateRom(f);
                            }
                        }
                    } else if (AppApi.APK_DOWNLOAD_FILENAME.equals(fileName)) {
                        if (md5Value != null && md5Value.equals(upgradeInfo.getApkMd5())) {
                            //升级APK
                            if (AppUtils.isMstar()) {
                                updateApk(f);
                            } else {
                                updateApk4Giec(f);
                            }
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

    @Override
    public void onProgress(int i) {

    }

    @Override
    public void onVerifyFailed(int i, Object o) {

    }

    @Override
    public void onCopyProgress(int i) {

    }

    @Override
    public void onCopyFailed(int i, Object o) {

    }

    @Override
    public void onStopProgress(int i) {

    }
}
