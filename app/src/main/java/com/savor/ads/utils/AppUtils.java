package com.savor.ads.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.savor.ads.bean.VersionInfo;
import com.savor.ads.core.Session;

import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author 朵朵花开
 *         <p>
 *         常用系统工具类
 */
public class AppUtils {
    public static final int MSG_WHAT_TO_TVPLAY = 0x6001;
    public static final int MSG_WHAT_TO_ADSPLAY = 0x6002;
    public static final int MSG_WHAT_TO_SDMOUNTED = 0x6003;
    public static final int MSG_WHAT_TO_SDREMOVED = 0x6004;
    public static final int MSG_WHAT_TO_CANPLAY = 0x6005;
    public static final int MSG_WHAT_TO_INITPLISTEORROR = 0x6006;
    public static final int MSG_WHAT_TO_BROADCAST_MULTICAST = 0x6007;
    public static final int MSG_WHAT_TO_MULTICAST_PLAY = 0x6008;
    public static final int MSG_WHAT_TO_EXITMULTICAST = 0x6009;
    public static final int MSG_WHAT_TO_EXITTV = 0x6010;
    public static final int MSG_WHAT_TO_SHOWCODE = 0x6011;
    public static final int MSG_WHAT_TO_HIDDENCODE = 0x6012;
    public static final int MSG_WHAT_TO_MULTICAST_STOP = 0x6013;
    public static final int MSG_WHAT_TO_STAETLOG = 0x6014;
    public static final int MSG_WHAT_TO_SHOWSETSERVER = 0x6015;
    public static final int MSG_WHAT_TO_NETWORKCONNECTED = 0x6016;
    public static final int MSG_WHAT_TO_HIDDENINFO = 0x6017;
    public static final int MSG_WHAT_TO_PREPAREERROR = 0x6018;
    public static final int MSG_WHAT_TO_KEYENABLE = 0x6019;
    public static final int MSG_WHAT_TO_MULTICASTMEDIA = 0x6020;
    public static final int MSG_WHAT_TO_ROTATE = 0x6021;
    public static final int MSG_WHAT_TO_MULTICASTMEDIA_PLAY = 0x6022;
    public static final int MSG_WHAT_TO_MULTICASTMEDIA_PAUSE = 0x6023;
    public static final int MSG_WHAT_TO_MULTICASTMEDIA_AUTOSTOP = 0x6024;
    public static final int MSG_WHAT_TO_UPDATEAPK = 0x6025;
    public static final int MSG_WHAT_TO_QRCODE = 0x6026;
    public static final int MSG_WHAT_TO_CHANNELLOG = 0x6027;
    public static final int MSG_WHAT_TO_SDCARDSTATUS = 0x6028;
    public static final String MSG_BROADCAST_MULTICAST_ACTION = "android.intent.action.Multicast";

    public static final String BoxLogDir = "log/";
    public static final String BoxlogedDir = "loged/";
    public static final String BoxMediaDir = "media/";
    public static final String BoxMulticast = "multicast/";
    public static final String BoxLotteryDir= "lottery/";
    // UTF-8 encoding
    private static final String ENCODING_UTF8 = "UTF-8";

    /**
     * DATE FORMAT 日期格式 例如"yyyy-MM-dd HH:mm:ss"
     */
    public static final String DATEFORMAT_YYMMDD_HHMMSS = "yyyy-MM-dd HH:mm:ss";
    public static final String DATEFORMAT_YYMMDD = "yyyy-MM-dd";
    public static final long CACHE_EXPIRED_TIME = 1 * 24 * 60 * 60 * 1000;

    public static enum StorageMode {
        /**
         * 手机内存
         */
        MobileMemory,
        /**
         * 存储卡
         */
        SDCard;
    }

    public static enum StorageFile {
        /**
         * 缓存
         */
        cache,
        /**
         * 老规则日志
         */
        log,
        /**
         * 已上传到小平台的日志
         */
        loged,
        /**
         * 广告视频目录
         */
        media,
        /**
         * 点播视频目录
         */
        multicast,
        /**
         * SDCard根目录配置文件
         */
        config,
        /**
         * 抽奖记录
         */
        lottery,
        /**幻灯片所用图片*/
        ppt,
    }

    private static TrustManager[] trustAllCerts;
    private static StorageMode storageMode;

    /** SDCard是否可用 **/

    /**
     * SDCard的根路径
     **/
    private static String SDCARD_PATH;
    private static String EXTERNAL_SDCARD_PATH;
    public static final int NOCONNECTION = 0;
    public static final int WIFI = 1;
    public static final int MOBILE = 2;

    /**
     * 返回手机连接网络类型
     *
     * @param context
     * @return 0： 无连接  1：wifi  2： mobile
     */
    public static int getNetworkType(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        int networkType = NOCONNECTION;
        if (networkInfo != null) {
            int type = networkInfo.getType();
            networkType = type == ConnectivityManager.TYPE_WIFI ? WIFI : MOBILE;
        }
        return networkType;
    }


    /**
     * 取得SD卡路径，以/结尾
     *
     * @return SD卡路径
     */
    public static String getSDCardPath() {
        boolean IS_MOUNTED = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        if (!IS_MOUNTED) {
            return null;
        }
        if (null != SDCARD_PATH) {
            return SDCARD_PATH;
        }
        File path = Environment.getExternalStorageDirectory();
        String SDCardPath = path.getAbsolutePath();
        SDCardPath += SDCardPath.endsWith(File.separator) ? "" : File.separator;
        SDCARD_PATH = SDCardPath;
        return SDCardPath;
    }

    //获取外部存储卡地址
    public static String getExternalSDCardPath() {
        if (!TextUtils.isEmpty(EXTERNAL_SDCARD_PATH)){
            return EXTERNAL_SDCARD_PATH;
        }
        String sdcard_path = null;
        String sd_default = Environment.getExternalStorageDirectory().getAbsolutePath();
        LogUtils.d(sd_default);
        if (sd_default.endsWith("/")) {
            sd_default = sd_default.substring(0, sd_default.length() - 1);
        }
        // 得到路径
        try {
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec("mount");
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            String line;
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                if (line.contains("secure"))
                    continue;
                if (line.contains("asec"))
                    continue;
                if (line.contains("fat") && line.contains("/mnt/")) {
                    String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        if (sd_default.trim().equals(columns[1].trim())) {
                            continue;
                        }
                        sdcard_path = columns[1];
                    }
                } else if (line.contains("fuse") && line.contains("/mnt/")) {
                    String columns[] = line.split(" ");
                    if (columns != null && columns.length > 1) {
                        if (sd_default.trim().equals(columns[1].trim())) {
                            continue;
                        }
                        sdcard_path = columns[1];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        EXTERNAL_SDCARD_PATH = sdcard_path;
        return sdcard_path;
    }

    /**
     * @param context
     * @param mode    StorageFile.cache or StorageFile.file
     * @return
     */
    public static String getFilePath(Context context, StorageFile mode) {
        String path = getExternalSDCardPath();

        File targetLogFile = new File(path + File.separator, BoxLogDir);
        if (!targetLogFile.exists()) {
            targetLogFile.mkdir();
        }
        File targetLogedFile = new File(path + File.separator, BoxlogedDir);
        if (!targetLogedFile.exists()) {
            targetLogedFile.mkdir();
        }
        File targetMediaFile = new File(path + File.separator, BoxMediaDir);
        if (!targetMediaFile.exists()) {
            targetMediaFile.mkdir();
        }
        File targetMulticastFile = new File(path + File.separator, BoxMulticast);
        if (!targetMulticastFile.exists()) {
            targetMulticastFile.mkdir();
        }
        File targetCacheFile = new File(path + File.separator, "cache");
        if (!targetCacheFile.exists()) {
            targetCacheFile.mkdir();
        }
        File targetLotteryFile = new File(path + File.separator,BoxLotteryDir);
        if (!targetLotteryFile.exists()){
            targetLotteryFile.mkdir();
        }
        File targetPptFile = new File(path + File.separator, "ppt");
        if (!targetPptFile.exists()) {
            targetPptFile.mkdir();
        }
        File targetConfigTxtFile = new File(path + File.separator + ConstantValues.CONFIG_TXT);
        if (mode == StorageFile.log) {
            path = targetLogFile.getAbsolutePath() + File.separator;
        } else if (mode == StorageFile.loged) {
            path = targetLogedFile.getAbsolutePath() + File.separator;
        } else if (mode == StorageFile.media) {
            path = targetMediaFile.getAbsolutePath() + File.separator;
        } else if (mode == StorageFile.multicast) {
            path = targetMulticastFile.getAbsolutePath() + File.separator;
        } else if (mode == StorageFile.config) {
            path = targetConfigTxtFile.getAbsolutePath();
        } else if (mode == StorageFile.cache) {
            path = targetCacheFile.getAbsolutePath() + File.separator;
        } else if (mode == StorageFile.lottery) {
            path = targetLotteryFile.getAbsolutePath() + File.separator;
        } else if (mode == StorageFile.ppt) {
            path = targetPptFile.getAbsolutePath() + File.separator;
        }
        return path;
    }


    /**
     * 判断文件夹是否为空
     *
     * @param path
     * @return
     */
    public static boolean isDirNull(String path) {

        File file = new File(path);
        if (file.exists()) {
            return true;
        } else
            return false;

    }

    /**
     * 判断文件是否存在
     *
     * @param path
     * @return
     */
    public static boolean isFileExist(String path) {

        if (path == null || path.length() <= 0) {
            return false;
        }
        File file = new File(path);
        if (file.exists() && file.length() > 0) {
            return true;
        } else {
            return false;
        }
    }

    //复制文件
    public static void copyFile(String srcFile, String destFile) {
        // 复制文件
        int byteread = 0; // 读取的字节数
        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(srcFile);
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];

            while ((byteread = in.read(buffer)) != -1) {
                out.write(buffer, 0, byteread);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static String getMD5Method(File f) {
        //下面生成图片的md5加密
//        StringBuilder allTex = new StringBuilder();
        InputStream in = null;
        byte[] frontb = null;
        byte[] backb = null;
        byte[] newb1 = null;
        byte[] newb2 = null;
        String endb = null;
        try {

            FileInputStream stream = new java.io.FileInputStream(f);
            LogUtils.i("一次读多个字节,当前文件" + f.getName() + "的字节数是：" + showAvailableBytes(stream));
//            allTex.append(String.valueOf(showAvailableBytes(stream)));
            int pos = 0;// 从第几个字节开始读
            int len = 200;// 读几个字节
            //			stream.skip(pos); // 跳过之前的字节数
            frontb = new byte[len];
            stream.read(frontb);

            int allChar = showAvailableBytes(stream);
            if (allChar > 400) {
                int poss = allChar - 200;// 从第几个字节开始读
                int lens = 200;// 读几个字节
                stream.skip(poss); // 跳过之前的字节数
                byte[] bs = new byte[200];
                stream.read(bs);
                backb = bs;
            }

            newb1 = new byte[frontb.length];
            System.arraycopy(frontb, 0, newb1, 0, frontb.length);
            newb2 = new byte[backb.length];
            System.arraycopy(backb, 0, newb2, 0, backb.length);
//            newb = new byte[frontb.length + backb.length];
//            System.arraycopy(frontb, 0, newb, 0, frontb.length);
//            System.arraycopy(backb, 0, newb, frontb.length, backb.length);
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
            }
        }
        // 调取md5方法，生成一个md5串
        String md5Vod1 = MD5(newb1);
        String md5Vod2 = MD5(newb2);

        String md5Vod = getMD5(md5Vod1+md5Vod2);
        return md5Vod;
    }

    /**
     * 显示输入流中还剩的字节数
     */
    private static int showAvailableBytes(InputStream in) {
        try {
            //       LogUtils.i(("当前字节输入流中的字节数为:" + in.available());
            return in.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * MD5加密
     *
     * @param
     * @return
     */
    public final static String MD5(byte[] b) {
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        try {
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(b);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void trustAllSSLForHttpsURLConnection() {
        // Create a trust manager that does not validate certificate chains
        if (trustAllCerts == null) {
            trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};
        }
        // Install the all-trusting trust manager
        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Throwable e) {
            LogUtils.e(e.getMessage(), e);
        }
        HttpsURLConnection.setDefaultHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    /**
     * Returns whether the network is available
     */
    public static boolean isNetworkAvailable(Context context) {

        if (context == null) {
            return false;
        }

        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            LogUtils.e("couldn't get connectivity manager");
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0, length = info.length; i < length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns whether the network is mobile
     */
    public static boolean isMobileNetwork(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            // LogUtils.w(Constants.TAG, "couldn't get connectivity manager");
        } else {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWifiNetwork(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity == null) {
            // LogUtils.w(Constants.TAG, "couldn't get connectivity manager");
        } else {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }
        return false;
    }

    public static String getCurTime() {
        SimpleDateFormat df = new SimpleDateFormat(DATEFORMAT_YYMMDD_HHMMSS);//设置日期格式
        return df.format(new Date());// new Date()为获取当前系统时间
    }

    /**
     * 根据日期格式获取当前日期
     */
    public static String getCurTime(String format) {
        SimpleDateFormat dfTemp = new SimpleDateFormat(format);//设置日期格式
        return dfTemp.format(new Date());// new Date()为获取当前系统时间
    }

    public static Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(DATEFORMAT_YYMMDD_HHMMSS);//设置日期格式
        return df.parse(dateStr);
    }

    public static int calculateMonthDiff(String dateSmall, String dateBig, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Calendar bef = Calendar.getInstance();
        Calendar aft = Calendar.getInstance();
        bef.setTime(sdf.parse(dateSmall));
        aft.setTime(sdf.parse(dateBig));
        int result = aft.get(Calendar.MONTH) - bef.get(Calendar.MONTH);
        int month = (aft.get(Calendar.YEAR) - bef.get(Calendar.YEAR)) * 12;
        return month + result;
    }

    /**
     * 根据日期格式解析日期
     */
    public static Date parseDate(String dateStr, String format) throws ParseException {
        SimpleDateFormat dfTemp = new SimpleDateFormat(format);//设置日期格式
        return dfTemp.parse(dateStr);
    }

    /**
     * 根据日期格式获取当前日期
     */
    public static String getStrTime(String time) {
        String mTime = time.replaceAll("-", "");
        return mTime;// new Date()为获取当前系统时间
    }

    public static void clearPptTmpFiles(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 删除幻灯片文件
                String pptPath = getFilePath(context, StorageFile.ppt);
                File pptDirectory = new File(pptPath);
                if (pptDirectory.exists()) {
                    LogFileUtil.write("clearAllCache will clear ppt dir");
                    File[] files = pptDirectory.listFiles();

                    if (files != null) {
                        for (File file : files) {
                            com.savor.ads.utils.FileUtils.deleteFile(file);
                        }
                    }
                }
            }
        }).start();
    }

    public static void clearAllCache(final Context context) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String cachePath = getFilePath(context, StorageFile.cache);
                File cacheDirectory = new File(cachePath);
                if (cacheDirectory.exists()) {
                    LogFileUtil.write("clearAllCache will clear cache dir");
                    File[] files = cacheDirectory.listFiles();

                    if (files != null) {
                        for (File file : files) {
                            com.savor.ads.utils.FileUtils.deleteFile(file);
                        }
                    }
                }

                // 删除sdcard中form表单处理可能遗留的临时文件
                String sdcardPath = getSDCardPath();
                if (!TextUtils.isEmpty(sdcardPath)) {
                    File sdcardDirectory = new File(sdcardPath);
                    if (sdcardDirectory.exists()) {
                        LogFileUtil.write("clearAllCache will clear MultiPart files");
                        File[] files = sdcardDirectory.listFiles();

                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile() && file.getName().startsWith("MultiPart")) {
                                    com.savor.ads.utils.FileUtils.deleteFile(file);
                                }
                            }
                        }
                    }
                }
            }
        }).start();
    }

    public static void clearAllFile(final Context context) {

        Thread clearTask = new Thread() {
            @Override
            public void run() {
                String logPath = getFilePath(context, StorageFile.log);
                File logDirectory = new File(logPath);
                if (logDirectory.exists()) {
                    String[] files = logDirectory.list();

                    if (files == null || files.length == 0) {
                        return;
                    }
                    for (String file : files) {
                        new File(logDirectory, file).delete();
                    }
                }

            }
        };
        clearTask.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        clearTask.start();
    }

    /**
     * <p>
     * Get UTF8 bytes from a string
     * </p>
     *
     * @param string String
     * @return UTF8 byte array, or null if failed to get UTF8 byte array
     */
    public static byte[] getUTF8Bytes(String string) {
        if (string == null)
            return new byte[0];

        try {
            return string.getBytes(ENCODING_UTF8);
        } catch (UnsupportedEncodingException e) {
            /*
             * If system doesn't support UTF-8, use another way
             */
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(bos);
                dos.writeUTF(string);
                byte[] jdata = bos.toByteArray();
                bos.close();
                dos.close();
                byte[] buff = new byte[jdata.length - 2];
                System.arraycopy(jdata, 2, buff, 0, buff.length);
                return buff;
            } catch (IOException ex) {
                return new byte[0];
            }
        }
    }

    /**
     * <p>
     * Get string in UTF-8 encoding
     * </p>
     *
     * @param b byte array
     * @return string in utf-8 encoding, or empty if the byte array is not encoded with UTF-8
     */
    public static String getUTF8String(byte[] b) {
        if (b == null)
            return "";
        return getUTF8String(b, 0, b.length);
    }

    /**
     * <p>
     * Get string in UTF-8 encoding
     * </p>
     */
    public static String getUTF8String(byte[] b, int start, int length) {
        if (b == null) {
            return "";
        } else {
            try {
                return new String(b, start, length, ENCODING_UTF8);
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }
    }

    public static void chmod(String permission, String path) {
        try {
            String command = "chmod " + permission + " " + path;
            Runtime runtime = Runtime.getRuntime();
            Process proc = runtime.exec(command);
            if (proc != null) {
                BufferedReader is = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                String line = null;
                while ((line = is.readLine()) != null) {
                    LogUtils.d("aMarket line:" + line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断当前应用是否是顶栈
     *
     * @param context
     * @return
     */
    public static boolean isAppOnForeground(Context context) {
        PackageInfo info = null;
        try {
            info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String curPackage = info.packageName;
            ActivityManager mActivityManager = ((ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE));
            List<RunningTaskInfo> tasksInfo = mActivityManager.getRunningTasks(1);
            if (tasksInfo != null && tasksInfo.size() > 0) {
                if (!TextUtils.isEmpty(curPackage) && curPackage.equals(tasksInfo.get(0).topActivity.getPackageName())) {
                    /**当前应用是顶栈*/
                    return true;
                }

            }
        } catch (Exception ex) {
            LogUtils.e(ex.toString());
        }
        return false;
    }

    public static long getFileSizes(File f) {
        long s = 0;
        FileInputStream fis = null;
        try {
            if (!f.exists()) {
                return s;
            }
            fis = new FileInputStream(f);
            s = fis.available();
        } catch (Exception ex) {
            LogUtils.e(ex.toString());
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fis = null;
        }

        return s;
    }


    /**
     * (获得本机的IP地址
     *
     * @return
     */
    public static String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (android.os.Build.VERSION.SDK_INT > 10) {
                        /**android 4.0以上版本*/
                        if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
                            return inetAddress.getHostAddress().toString();
                        }
                    } else {
                        if (!inetAddress.isLoopbackAddress()) {
                            return inetAddress.getHostAddress().toString();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * 计算md5值
     */
    public static byte[] getMd5(String str) {
        if (str == null) {
            return null;
        }
        byte[] result = null;
        try {
            result = getMd5(str.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        return result;
    }

    /**
     * 计算md5值
     */
    public static byte[] getMd5(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StreamUtils su = new StreamUtils(true);
        try {
            su.copyStreamInner(new ByteArrayInputStream(bytes), null);
        } catch (IOException e) {
        }
        return su.getMD5();
    }

    /**
     * Get MD5 Code
     */
    public static String getMD5(String text) {
        try {
            byte[] byteArray = text.getBytes("utf8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(byteArray, 0, byteArray.length);
            return StringUtils.toHexString(md.digest(), false);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Get MD5 Code
     */
    public static String getMD5(byte[] byteArray) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(byteArray, 0, byteArray.length);
            return StringUtils.toHexString(md.digest(), false);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    /**
     * 清除过期的缓存文件（FIle里）
     */
    public static void clearExpiredFile(final Context context, final boolean isAllFile) {
        Thread clearTask = new Thread() {
            @Override
            public void run() {
                try {
                    String path = AppUtils.getFilePath(context, StorageFile.log);
                    File cacheDirectory = new File(path);
                    if (cacheDirectory.exists()) {
                        File[] files = cacheDirectory.listFiles();

                        if (files == null || files.length == 0) {
                            return;
                        }

                        long currentTime = System.currentTimeMillis();
                        for (File f : files) {
                            if (isAllFile) {
                                f.delete();
                                continue;
                            }
                            long lastTime = f.lastModified();
                            if (currentTime - lastTime > CACHE_EXPIRED_TIME) {
                                f.delete();
                            }
                        }
                    }
                } catch (Exception ex) {
                    LogUtils.e(ex.toString());
                }
            }
        };
        clearTask.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        clearTask.start();
    }

    /**
     * 清除过期的缓存文件（FIle里）
     */
    public static void clearExpiredCacheFile(final Context context) {
        Thread clearTask = new Thread() {
            @Override
            public void run() {
                try {
                    String path = AppUtils.getFilePath(context, StorageFile.cache);
                    File cacheDirectory = new File(path);
                    if (cacheDirectory.exists()) {
                        File[] files = cacheDirectory.listFiles();

                        if (files == null || files.length == 0) {
                            return;
                        }

                        long currentTime = System.currentTimeMillis();
                        for (File f : files) {
                            long lastTime = f.lastModified();
                            if (currentTime - lastTime > CACHE_EXPIRED_TIME) {
                                f.delete();
                            }
                        }
                    }
                } catch (Exception ex) {
                    LogUtils.e(ex.toString());
                }
            }
        };
        clearTask.setPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        clearTask.start();
    }

    /**
     * 隐藏软键盘
     *
     * @param activity
     */
    public static void hideSoftKeybord(Activity activity) {

        if (null == activity) {
            return;
        }
        try {
            final View v = activity.getWindow().peekDecorView();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        } catch (Exception e) {

        }
    }

    /**
     * 强制聚焦并打开键盘
     *
     * @param activity
     * @param editText
     */
    public static void tryFocusEditText(Activity activity, EditText editText) {

        if (editText.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    public static boolean isAppProcessRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
        String packageName = context.getPackageName();
        for (RunningAppProcessInfo info : runningAppProcesses) {
            if (info.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }


//    /**
//     * 获取当前系统日期时间
//     *
//     * @param format "all"获取年月日加时间
//     *               "date"只获取当前年月日
//     *               "time"只获取当前时间
//     * @return
//     */
//    public static String getTime(String format) {
//        Time t = new Time(); // or Time t=new Time("GMT+8"); 加上Time Zone资料。
//        t.setToNow(); // 取得系统时间。
//        String monthM = "";
//        String monthDayM = "";
//        String hourM = "";
//        String minuteM = "";
//        if (t.month + 1 < 10) {
//            monthM = "0" + (t.month + 1);
//        } else
//            monthM = "" + (t.month + 1);
//        if (t.monthDay < 10) {
//            monthDayM = "0" + t.monthDay;
//        } else
//            monthDayM = "" + t.monthDay;
//        if (t.hour < 10) {
//            hourM = "0" + t.hour;
//        } else {
//            hourM = "" + t.hour;
//        }
//
//        if (t.minute < 10) {
//            minuteM = "0" + t.minute;
//        } else {
//            minuteM = "" + t.minute;
//        }
//        String time = null;
//        switch (format) {
//            case "all":
//                time = "" + t.year + monthM + monthDayM + hourM + minuteM;
//                break;
//            case "hour":
//                time = "" + t.year + monthM + monthDayM + hourM;
//                break;
//            case "date":
//                time = "" + t.year + monthM + monthDayM;
//                break;
//            case "month":
//                time = "" + t.year + monthM;
//                break;
//            case "time":
//                time = "" + hourM + minuteM;
//                break;
//        }
//        return time;
//    }

    /**
     * wifi IP
     */
    public static String getWifiIp(Context con) {
        WifiManager wifiManager = (WifiManager) con
                .getSystemService(Context.WIFI_SERVICE);// 获取WifiManager

        WifiInfo wifiinfo = wifiManager.getConnectionInfo();

        int i = wifiinfo.getIpAddress();
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + ((i >> 24) & 0xFF);

    }

    public static String getWifiName(Context con) {

        WifiManager wifiManager = (WifiManager) con
                .getSystemService(Context.WIFI_SERVICE);// 获取WifiManager
        WifiInfo wifiinfo = wifiManager.getConnectionInfo();
        return wifiinfo.getSSID();
    }

    /**
     * 根据下标返回信号源描述
     *
     * @param index
     * @return
     */
    public static String getInputType(int index) {
        String type = "ANT IN";
        switch (index) {
            case 0:
                type = "ANT IN";
                break;
            case 1:
                type = "HDMI IN";
                break;
            case 2:
                type = "AV IN";
                break;
        }
        return type;
    }

    /**
     * 获取以太网MAC地址
     *
     * @return
     */
    public static String getEthernetMacAddr() {
        String cmd = "busybox ifconfig eth0";
        Process process = null;
        InputStream is = null;
        BufferedReader reader = null;
        String result = "";
        try {
            process = Runtime.getRuntime().exec(cmd);
            is = process.getInputStream();
            reader = new BufferedReader(
                    new InputStreamReader(is));
            String line = reader.readLine();
            if (!TextUtils.isEmpty(line)) {
                result = line.substring(line.indexOf("HWaddr") + 6).trim()
                        .replaceAll(":", "");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 获取以太网 IP
     *
     * @return
     */
    public static String getEthernetIP() {
        String cmd = "busybox ifconfig eth0";
        Process process = null;
        InputStream is = null;
        BufferedReader reader = null;
        String result = "";
        try {
            process = Runtime.getRuntime().exec(cmd);
            is = process.getInputStream();
            reader = new BufferedReader(
                    new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {
                if (!TextUtils.isEmpty(line) && line.trim().startsWith("inet ")) {
                    result = line.substring(line.indexOf("addr:") + 5);
                    result = result.substring(0, result.indexOf(" ")).trim();
                    break;
                }
                line = reader.readLine();
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
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 获取Wlan MAC地址
     *
     * @return
     */
    public static String getWlanMacAddr() {
        String cmd = "busybox ifconfig wlan0";
        Process process = null;
        InputStream is = null;
        BufferedReader reader = null;
        String result = "";
        try {
            process = Runtime.getRuntime().exec(cmd);
            is = process.getInputStream();
            reader = new BufferedReader(
                    new InputStreamReader(is));
            String line = reader.readLine();
            if (!TextUtils.isEmpty(line)) {
                result = line.substring(line.indexOf("HWaddr") + 6).trim()
                        .replaceAll(":", "");
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
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 获取Wlan IP
     *
     * @return
     */
    public static String getWlanIP() {
        String cmd = "busybox ifconfig wlan0";
        Process process = null;
        InputStream is = null;
        BufferedReader reader = null;
        String result = "";
        try {
            process = Runtime.getRuntime().exec(cmd);
            is = process.getInputStream();
            reader = new BufferedReader(
                    new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {
                if (!TextUtils.isEmpty(line) && line.trim().startsWith("inet ")) {
                    result = line.substring(line.indexOf("addr:") + 5);
                    result = result.substring(0, result.indexOf(" ")).trim();
                    break;
                }
                line = reader.readLine();
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
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    //检查redian文件夹下是否有图有真相
    public static boolean checkIsImageFile(String fName) {

        boolean isImageFile;
        // 获取扩展名
        String FileEnd = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
        if (FileEnd.equals("jpg") || FileEnd.equals("png") || FileEnd.equals("gif")
                || FileEnd.equals("jpeg") || FileEnd.equals("bmp")) {
            isImageFile = true;
        } else {
            isImageFile = false;
        }
        return isImageFile;
    }


    public static boolean zipFile(File srcFile, File destFile, String comment) throws Exception {
        ZipOutputStream zOutStream = null;
        if (srcFile == null || destFile == null) return false;

        long fileSize = srcFile.length();
        if (!srcFile.exists()) {
            return false;
        }
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            boolean flag = destFile.createNewFile();
            zOutStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(destFile), (int) fileSize / 2));
            ZipEntry en = new ZipEntry(srcFile.getName());
            en.setSize(srcFile.length());
            zOutStream.putNextEntry(en);
            zOutStream.setComment(comment);
            byte[] byFile = FileUtils.readFileToByteArray(srcFile);
            zOutStream.write(byFile);
            zOutStream.flush();
            zOutStream.close();
        } catch (Exception e) {
            // TODO: handle exception
            LogUtils.e(e.toString());
            return false;
        } finally {
            try {
                if (zOutStream != null) {
                    zOutStream.close();
                    zOutStream = null;
                }
            } catch (Exception e2) {
                // TODO: handle exception
                LogUtils.e(e2.toString());
            }
        }
        return true;
    }

    /**
     *
     * @param context
     * @param serverVersion
     * @param type：1是rom升级，2是apk升级
     * @return
     */
    public static boolean needUpdate(Context context,String serverVersion,int type){
        Session session = Session.get(context);
        if (serverVersion == null){
            return false;
        }
        String localVersion = null;
        if (type ==1){
            String rom = session.getRomVersion();
            if (!TextUtils.isEmpty(rom)){
                localVersion = rom.replace("V","").trim();
            }else{
                return false;
            }
        }else{
            localVersion = session.getVersionName();
        }
        if (!TextUtils.isEmpty(serverVersion)&&!localVersion.equals(serverVersion)){
            return true;
        }
        return false;
    }


    public static Bitmap getLoacalBitmap(String url) {

        try {

            FileInputStream fis = new FileInputStream(url);

            return BitmapFactory.decodeStream(fis);

        } catch (FileNotFoundException e) {

            e.printStackTrace();

            return null;

        }

    }

    //获取热点状态
    public static int getWifiAPState(Context context) {
        int state = -1;
        try {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            Method method2 = wifiManager.getClass().getMethod("getWifiApState");
            state = (Integer) method2.invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return state;
    }
    public static boolean setWifiApEnabled(Context context, boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (enabled) { // disable WiFi in any case
            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
            wifiManager.setWifiEnabled(false);
        }
        try {
            //热点的配置类
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.allowedAuthAlgorithms.clear();
            wifiConfig.allowedGroupCiphers.clear();
            wifiConfig.allowedKeyManagement.clear();
            wifiConfig.allowedPairwiseCiphers.clear();
            wifiConfig.allowedProtocols.clear();
            //配置热点的名称
            String ssid = "";
            if (!TextUtils.isEmpty(Session.get(context).getBoxName())) {
                ssid = Session.get(context).getBoxName();
            } else {
                String mac = getEthernetMacAddr();
                if (!TextUtils.isEmpty(mac) && mac.length() > 3) {
                    ssid = "RD" + mac.substring(mac.length() - 3);
                } else {
                    String timestamp = String.valueOf(System.currentTimeMillis());
                    ssid = "RD" + timestamp.substring(timestamp.length() - 5);
                }
            }
            wifiConfig.SSID = ssid;
            wifiConfig.preSharedKey = "11111111";
            wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfig.allowedKeyManagement.set(4 /*WifiConfiguration.KeyMgmt.NONE*/);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN/*WPA*/);
            wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfig.status = WifiConfiguration.Status.ENABLED;
            //通过反射调用设置热点
            Method method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            //返回热点打开状态
            return (Boolean) method.invoke(wifiManager, wifiConfig, enabled);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isWifiEnabled(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static String getWifiApIp() {
        String cmd = "ip route show";
        Process process = null;
        InputStream is = null;
        BufferedReader reader = null;
        String result = "";
        try {
            process = Runtime.getRuntime().exec(cmd);
            is = process.getInputStream();
            reader = new BufferedReader(
                    new InputStreamReader(is));
            String line = null;
            while ((line = reader.readLine()) != null){
                line = line.trim();
                if (line.contains("wlan0") && line.contains("proto kernel")) {
                    result = line.substring(line.lastIndexOf(" ") + 1).trim();
                    break;
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String getWifiApName(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration wifiConfig = (WifiConfiguration)method.invoke(wifiManager);
            return wifiConfig.SSID;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检测是否可播放下一期
     * @param context
     * @return
     */
    public static boolean checkPlayTime(Context context) {
        boolean canPlayNext = false;
        Session session = Session.get(context);
        String pubTime = session.getProNextMediaPubTime();
        if (!TextUtils.isEmpty(pubTime)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date pubDate = format.parse(pubTime);
                if (pubDate.getTime() < System.currentTimeMillis()) {
                    LogUtils.d("checkPlayTime 已到达发布时间，将更新期号");
                    LogFileUtil.write("checkPlayTime 已到达发布时间，将更新期号");
                    canPlayNext = true;


                    session.setProPeriod(session.getProNextPeriod());
                    session.setProNextPeriod(null);
                    session.setProNextMediaPubTime(null);

                    session.setAdvPeriod(session.getAdvNextPeriod());
                    session.setAdvNextPeriod(null);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                LogFileUtil.write("checkPlayTime 检测发布时间异常:" + e.getLocalizedMessage());
            }
        }
        return canPlayNext;
    }

    public static String getShowingSSID(Context context) {
        String ssid = AppUtils.getWifiName(context);
        if (TextUtils.isEmpty(ssid)) {
            ssid = Session.get(context).getBoxName();
        }
        return ssid;
    }

    public static String findSpecifiedPeriodByType(ArrayList<VersionInfo> versionList, String type) {
        String period = "";
        if (versionList != null && type != null) {
            for (VersionInfo versionInfo : versionList) {
                if (type.equals(versionInfo.getType())) {
                    period = versionInfo.getVersion();
                    break;
                }
            }
        }
        return period;
    }
}