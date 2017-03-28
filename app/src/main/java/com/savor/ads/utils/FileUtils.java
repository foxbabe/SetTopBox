package com.savor.ads.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by Administrator on 2016/12/17.
 */

public class FileUtils {

    public static void delDir(File file){
        try{
            if (file.exists()){
                if (file.isDirectory()){
                    File[] files = file.listFiles();
                    for (File f:files){
                        if(f.isFile()){
                            f.delete();
                        }else{
                            delDir(f);
                        }
                    }
                }
                file.delete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 复制单个文件
     */
    public static void copyFile(String oldPath, String newPath) {
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            File newfile = new File(newPath);
            if (!newfile.getParentFile().exists()) {
                newfile.getParentFile().mkdirs();
            }
            if (oldfile.exists()) { //文件存在时
                inStream = new FileInputStream(oldPath); //读入原文件
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (fs != null) {
                try {
                    fs.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

    }
}
