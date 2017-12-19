package com.savor.ads.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/12/17.
 */

public class FileUtils {

    public static void deleteFile(File file){
        try{
            if (file.exists()){
                if (file.isDirectory()){
                    File[] files = file.listFiles();
                    for (File f:files){
                        if(f.isFile()){
                            f.delete();
                        }else{
                            deleteFile(f);
                        }
                    }
                }
                file.delete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void copyDir(String srcDirPath, String dstDirPath) {
        File srcDir = new File(srcDirPath);
        File dstDir = new File(dstDirPath);
        if (!dstDir.isDirectory())
            return;
        if (!dstDir.exists()) {
            dstDir.mkdir();
        }

        if (srcDir.exists() && srcDir.isDirectory()) {
            File[] files = srcDir.listFiles();
            for (File file : files) {
                File dstFile = new File(dstDirPath + file.getName());
                if (file.isFile()) {
                    if (dstFile.exists() && dstFile.length() == file.length()) {
                        continue;
                    }

                    try {
                        copyFile(file, dstFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    copyDir(file.getPath(), dstFile.getPath());
                }
            }
        }
    }

    /**
     * 复制单个文件
     */
    public static void copyFile(String oldPath, String newPath) throws Exception{
        copyFile(new File(oldPath), new File(newPath));
    }

    public static void copyFile(File oldFile, File newFile) throws Exception{
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {
            int bytesum = 0;
            int byteread = 0;
            if (!newFile.getParentFile().exists()) {
                newFile.getParentFile().mkdirs();
            }
            if (oldFile.exists()) { //文件存在时
                inStream = new FileInputStream(oldFile); //读入原文件
                fs = new FileOutputStream(newFile);
                byte[] buffer = new byte[2048];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);
                }
            }
        } catch (Exception e) {
            throw new Exception();
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

    public static void write(String filePath, String content) {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(content);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String read(String filePath) {
        String content = null;
        File file = new File(filePath);
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                int length = fileInputStream.available();

                byte[] buffer = new byte[length];
                fileInputStream.read(buffer);

                content = new String(buffer, "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return content;
    }


    /**
     * 读取文件的内容
     * @param file 想要读取的文件对象
     * @return 返回文件内容
     */
    public static List<String> readFile(File file){
        BufferedReader br = null;
        List<String> cfgList = new ArrayList<>();
        try{
            String string = null;
            br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            while((string = br.readLine())!=null){//使用readLine方法，一次读一行
                cfgList.add(string);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if (br != null){
                try{
                    br.close();
                }catch (Exception e){}
            }
        }
        return cfgList;
    }

    public static String readFileToStr(File file) {
        BufferedReader br = null;
        StringBuffer jsonContent = new StringBuffer();
        try {
            String string = null;
            br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            while ((string = br.readLine()) != null) {//使用readLine方法，一次读一行
                jsonContent.append(string);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                }
            }
        }
        return jsonContent.toString();
    }
    public static byte[] readByte(String filePath) {
        byte[] buffer = null;
        File file = new File(filePath);
        if (file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                int length = fileInputStream.available();

                buffer = new byte[length];
                fileInputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buffer;

    }
}
