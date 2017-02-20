package com.savor.ads.utils;

import java.io.File;

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
}
