package com.savor.ads.utils;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by zhanghq on 2016/12/14.
 */

public class ShellUtils {

    public static boolean deleteFile(String filePath) {

        boolean result = false;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            try {
                dos.writeBytes("rm -r " + filePath + "\n");
                dos.flush();
                dos.writeBytes("exit\n");
                dos.flush();
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                dos.close();
            }
            process.waitFor();
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return result;

    }

    public static boolean resetNetwork() {

        boolean result = false;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            try {
                dos.writeBytes("ifconfig eth0 down\n");
                dos.flush();
                dos.writeBytes("ifconfig eth0 up\n");
                dos.flush();
                dos.writeBytes("ifconfig wlan0 down\n");
                dos.flush();
                dos.writeBytes("ifconfig wlan0 up\n");
                dos.flush();
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    dos.close();
                    if (process != null) {
                        process.waitFor();
                        process.destroy();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    public static boolean unmountWlan1() {

        boolean result = false;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(process.getOutputStream());
            try {
                dos.writeBytes("ifconfig wlan1 down\n");
                dos.flush();
                result = true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    dos.close();
                    if (process != null) {
                        process.waitFor();
                        process.destroy();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    public static void reboot() {
        try {
            Runtime.getRuntime().exec("reboot");
//            Runtime.getRuntime().exec("su -c reboot");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * check whether has root permission
     *
     * @return
     */
//    public static boolean checkRootPermission() {
//        return execCommand("echo root", true, false).result == 0;
//    }

    public static boolean checkRootPermission(){
        Process process = null;
        DataOutputStream os = null;
        try{
            process = Runtime.getRuntime().exec("system/xbin/su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            if (exitValue == 0){
                return true;
            } else{
                return false;
            }
        } catch (Exception e){
            Log.d("*** DEBUG ***", "Unexpected error - Here is what I know: "+ e.getMessage());
                return false;
        } finally{
            try{
                if (os != null){
                    os.close();
                }
                process.destroy();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

//    //更新启动图的位置
//    public static boolean updateLogoPic(String arg) {
//        try {
//            if(AppUtils.isFileExist(GlobalValues.LOGO_FILE_PATH)){
//                File file = new File(GlobalValues.LOGO_FILE_PATH);
//                file.delete();
//            }
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        boolean isflag = false;
//        try {
//            java.lang.Process proc = Runtime.getRuntime().exec("su");
//            DataOutputStream dos = new DataOutputStream(proc.getOutputStream());
//            if (dos != null) {
//                try {
//
//                    dos.writeBytes("cat " + arg + " > " + GlobalValues.LOGO_FILE_PATH +"\n");
//                    dos.flush();
//                    dos.writeBytes("exit\n");
//                    dos.flush();
//                    isflag = true;
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    LogUtils.d(e.toString());
//                } finally {
//                    if (dos!=null){
//                        dos.close();
//                    }
//                }
//            }
//            try {
//                proc.waitFor();
//            } catch (InterruptedException e) {
//                LogUtils.d(e.toString());
//                e.printStackTrace();
//            }
//
//            try {
//                if (proc != null) {
//                    proc.exitValue();
//                }
//            } catch (IllegalThreadStateException e) {
//                proc.destroy();
//            }
//        } catch (IOException e) {
//            LogUtils.d(e.toString());
//            e.printStackTrace();
//        }
//        return isflag;
//
//    }
}
