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
    private static String mLogFilePath;
    private static String mExceptionFilePath;
    private static String mApFilePath;

    public static void init() {
        mLogFilePath = AppUtils.getSDCardPath() + File.separator + LOG_FILE_NAME;
        mExceptionFilePath = AppUtils.getSDCardPath() + File.separator + EXCEPTION_FILE_NAME;
        mApFilePath = AppUtils.getSDCardPath() + File.separator + AP_FILE_NAME;
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
}
