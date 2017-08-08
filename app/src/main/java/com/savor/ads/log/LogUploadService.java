package com.savor.ads.log;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.savor.ads.BuildConfig;
import com.savor.ads.core.Session;
import com.savor.ads.oss.OSSValues;
import com.savor.ads.oss.ResuambleUpload;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.util.Hashtable;

public class LogUploadService {

    private final static String TAG = "LogUploadSer";
    private static Hashtable<String, String> mLogLocalList = new Hashtable<String, String>();
    private Context context;
    private Session session;
    private OSS oss;

    public LogUploadService(Context context) {
        this.context = context;
        session = Session.get(context);

        initOSSClient();
    }

    private void initOSSClient() {
        OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(OSSValues.accessKeyId, OSSValues.accessKeySecret);

        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        OSSLog.enableLog();
        oss = new OSSClient(context, BuildConfig.OSS_ENDPOINT, credentialProvider, conf);
    }

    public void start() {

        new Thread() {
            @Override
            public void run() {

                try {
                    sleep(1000 * 60 * 10);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                //只保留loged目录下面当月以及上月的日志
                File[] files = new File(AppUtils.getFilePath(context, AppUtils.StorageFile.loged)).listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.getName().endsWith(".blog")) {
                            file.delete();
                            continue;
                        }

                        String name = file.getName();
                        String[] split = name.split("_");
                        String currentDate = AppUtils.getTime("month");
                        String currentMonth = currentDate.substring(4, 6);
                        String logMonth = null;
                        if (split.length == 4) {    // 老版日志命名结构
                            logMonth = split[3].substring(4, 6);
                        } else if (split.length == 2) {     // 新版日志命名结构，例：FCD5D900B8B6_2017061415.blog
                            logMonth = split[1].substring(4, 6);
                        }
                        if (!TextUtils.isEmpty(logMonth)) {
                            int logMonthInt = Integer.parseInt(logMonth);
                            int curMonthInt = Integer.parseInt(currentMonth);
                            int diff = curMonthInt - logMonthInt;
                            diff = diff < 0 ? diff + 12 : diff;
                            // 删除大于1个月的日志
                            if (diff > 1) {
                                file.delete();
                            }
                        }
                    }
                }

                uploadLotteryRecordFile();

                while (true) {
                    uploadFile();
                    try {
                        Thread.sleep(1000 * 5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();

    }

    private void uploadLotteryRecordFile() {
        File[] files = getAllLogInfo(AppUtils.StorageFile.lottery);
        if (files != null && files.length > 0) {
            for (final File file : files) {
                final String name = file.getName();
                final String path = file.getPath();
                if (file.isFile()) {

                    if (name.contains(AppUtils.getTime("date"))) {
                        continue;
                    }
                    final String archive = path + ".zip";
                    try {
                        AppUtils.zipFile(new File(path), new File(archive), name + ".zip");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (new File(archive).exists()) {
                        final String object_key = archive.substring(1, archive.length());
                        String oss_file_path = OSSValues.uploadLotteryPath + name + ".zip";
                        new ResuambleUpload(oss,
                                BuildConfig.OSS_BUCKET_NAME,
                                oss_file_path,
                                object_key,
                                new LogUploadService.UploadResult() {
                                    @Override
                                    public void isSuccessOSSUpload(boolean flag) {
                                        if (flag) {
                                            file.delete();
                                        }
                                        if (new File(archive).exists()) {
                                            new File(archive).delete();
                                        }
                                    }
                                }).resumableUpload();
                    }
                }
            }
        }

    }

    private void uploadFile() {
        File[] files = getAllLogInfo(AppUtils.StorageFile.log);
        if (files != null && files.length > 0) {
            for (File file : files) {
                final String name = file.getName();
                final String path = file.getPath();
                if (file.isFile()) {
                    String[] split = name.split("_");
                    if (split.length != 2) {
                        continue;
                    }
                    final String time = split[1].substring(0, 10);
                    if (time.equals(AppUtils.getTime("hour"))) {
                        continue;
                    }
                    final String archive = path + ".zip";

                    if (!TextUtils.isEmpty(session.getOss_file_path())) {

                        File sourceFile = new File(path);
                        final File zipFile = new File(archive);
                        try {
                            AppUtils.zipFile(sourceFile, zipFile, name + ".zip");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (zipFile.exists()) {
                            final String object_key = archive.substring(1, archive.length());
                            String oss_file_path = session.getOss_file_path() + name + ".zip";

                            new ResuambleUpload(oss,
                                    BuildConfig.OSS_BUCKET_NAME,
                                    oss_file_path,
                                    object_key,
                                    new LogUploadService.UploadResult() {
                                        @Override
                                        public void isSuccessOSSUpload(boolean flag) {
                                            if (flag) {
                                                afterOSSUpload(name, time);
                                            }
                                            if (zipFile.exists()) {
                                                zipFile.delete();
                                            }
                                        }
                                    }).resumableUpload();
                        }
                    }
                }
            }
        }

    }


    /**
     * 获取log目录下所有日志
     */
    private File[] getAllLogInfo(AppUtils.StorageFile storage) {
        String path = AppUtils.getFilePath(context, storage);
        File[] files = new File(path).listFiles();
        if (files == null || files.length <= 0)
            return null;
        for (File f : files) {
            if (f.isFile() && f.exists()) {
                String filePath = f.getPath();
                String fileName = f.getName();
                if (fileName.contains(".zip")) {
                    f.delete();
                    continue;
                }
            }
        }
        files = new File(path).listFiles();
        return files;
    }

    private void afterOSSUpload(String fileName, String time) {
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(time)) {
            return;
        }
        String filepath = AppUtils.getFilePath(context, AppUtils.StorageFile.log) + fileName;
        String currentTime = AppUtils.getTime("hour");
        if (!time.equals(currentTime) && new File(filepath).exists()) {
            String deskPath = AppUtils.getFilePath(context, AppUtils.StorageFile.loged);
            new File(filepath).renameTo(new File(deskPath + fileName));
        }

    }


    public interface UploadResult {
        void isSuccessOSSUpload(boolean flag);
    }
}
