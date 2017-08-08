package com.savor.ads.utils;

import android.text.TextUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by zhanghq on 2016/12/30.
 */

public class LogFileUtil {

    private static final String LOG_FILE_NAME = "savor_log.txt";
    private static final String EXCEPTION_FILE_NAME = "savor_exception.txt";
    private static final String AP_FILE_NAME = "savor_ap.txt";
    private static final String BOOT_DIR_NAME = "savor_boot";
    private static final int BOOT_LOG_MAX_LENGTH = 1024 * 1024 * 2;
    private static String mLogFilePath;
    private static String mExceptionFilePath;
    private static String mApFilePath;
    private static String mBootDirPath;

    public static void init() {
        mLogFilePath = AppUtils.getSDCardPath() + File.separator + LOG_FILE_NAME;
        mExceptionFilePath = AppUtils.getSDCardPath() + File.separator + EXCEPTION_FILE_NAME;
        mApFilePath = AppUtils.getSDCardPath() + File.separator + AP_FILE_NAME;
        mBootDirPath = AppUtils.getSDCardPath() + File.separator + BOOT_DIR_NAME;
        File file = new File(mLogFilePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File exceptionFile = new File(mExceptionFilePath);
        if (!exceptionFile.exists()) {
            try {
                exceptionFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File apFile = new File(mApFilePath);
        if (!apFile.exists()) {
            try {
                apFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File bootDir = new File(mBootDirPath);
        if (!bootDir.exists()) {
            bootDir.mkdir();
        }
    }

    public static void write(String msg) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(mLogFilePath, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileWriter != null) {
            try {
                fileWriter.write(AppUtils.getCurTime() + " " + msg + "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeException(Throwable ex) {
        String title = (ex.getLocalizedMessage() != null) ? (ex.getClass().getName() + ": " + ex.getLocalizedMessage()) : ex.getClass().getName();
        StringBuilder sb = new StringBuilder(title + "\r\n");
        StackTraceElement[] trace = ex.getStackTrace();
        if (trace != null) {
            for (StackTraceElement traceElement : trace)
                sb.append("\tat " + traceElement + "\r\n");
        }
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(mExceptionFilePath, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileWriter != null) {
            try {
                fileWriter.write(AppUtils.getCurTime() + " " + sb.toString() + "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeApInfo(String msg) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(mApFilePath, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileWriter != null) {
            try {
                fileWriter.write(AppUtils.getCurTime() + " " + msg+ "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeBootInfo(String bootTime) {
        LogUtils.d("writeBootInfo bootTime = " + bootTime);
        if (TextUtils.isEmpty(bootTime)) {
            return;
        }
        final String month = bootTime.substring(5, 7);

        // 删除6个月前的开机日志
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int monthInt = Integer.parseInt(month);

                    LogUtils.d("Try to delete old log, mBootDirPath:" + mBootDirPath);
                    File bootLogDir = new File(mBootDirPath);
                    for (File file : bootLogDir.listFiles()) {
                        try {
                            String logMonth = file.getName();
                            int logMonthInt = Integer.parseInt(logMonth);
                            int diff = monthInt - logMonthInt;
                            diff = diff < 0 ? diff + 12 : diff;
                            LogUtils.d("diff = " +diff);
                            if (diff > 6) {
                                file.delete();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        String filePath = mBootDirPath + File.separator + month;
        File file = new File(filePath);
        // 单月启动日志文件长度大于阈值不再记录数据，以免写爆sdcard
        if (file.length() > BOOT_LOG_MAX_LENGTH) {
            return;
        }

        // 开始写文件
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileWriter != null) {
            try {
                fileWriter.write("TV boot at " + bootTime+ "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileWriter.flush();
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
