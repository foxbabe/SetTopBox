package com.savor.ads.utils;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by zhanghq on 2016/12/14.
 */

public class ShellUtils {

    public static void reboot() {
        try {
            Runtime.getRuntime().exec("reboot");
//            Runtime.getRuntime().exec("su -c reboot");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
}
