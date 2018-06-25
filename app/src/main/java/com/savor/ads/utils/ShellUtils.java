package com.savor.ads.utils;

import android.util.Log;

import com.savor.ads.core.AppApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanghq on 2016/12/14.
 */

public class ShellUtils {

    /**
     * 通过shell命令
     * @param list 要执行的shell命令
     * @param action 0:不返回执行命令结果；1：返回命令结果
     * @return
     */
    public static JSONArray universalShellCommandMethod(List<String> list,int action){
        DataOutputStream dos = null;
        InputStream is = null;
        BufferedReader reader = null;
        JSONArray jsonArray = null;
        Process process = null;
        try {
//            list = new ArrayList<>();
//            list.add("su");
//            list.add("cd /sdcard/multicast/");
//            list.add("du -sh");
            process = Runtime.getRuntime().exec(list.get(0));
            dos = new DataOutputStream(process.getOutputStream());
            list.remove(0);
            for (String str:list){
                dos.writeBytes(str+"\n");
                dos.flush();
            }
//            if (action==1){
//                jsonArray = new JSONArray();
//                is = process.getInputStream();
//                reader = new BufferedReader(new InputStreamReader(is));
//                String len;
//                while ((len = reader.readLine())!=null){
//                    jsonArray.put(len+"\n");
//                }
//            }
//            process.waitFor();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (dos!=null){
                    dos.close();
                }
                if (is!=null){
                    is.close();
                }
                if (reader!=null){
                    reader.close();
                }
                if (process!=null){
                    process.waitFor();
                    process.destroy();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return jsonArray;
    }



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
            Runtime.getRuntime().exec("su -c reboot");
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
            process = Runtime.getRuntime().exec("su");
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
