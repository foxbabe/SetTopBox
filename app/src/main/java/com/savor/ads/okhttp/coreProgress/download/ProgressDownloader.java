package com.savor.ads.okhttp.coreProgress.download;

import android.util.Log;

import com.savor.ads.utils.LogUtils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by Administrator on 2016/12/9.
 */

public class ProgressDownloader {
    public static final String TAG = "ProgressDownloader";
    public final static int CONNECT_TIMEOUT =60;
    public final static int READ_TIMEOUT=10;
    public final static int WRITE_TIMEOUT=10;
    private ProgressResponseListener progressResponseListener;
    private String url;
    private OkHttpClient client;
    //下载文件存储的位置
    private File destination;
    private Call call;
    public ProgressDownloader (String url,File destination,ProgressResponseListener progressResponseListener){
        this.url = url;
        this.destination = destination;
        this.progressResponseListener = progressResponseListener;
        //在下载、暂停后的继续下载中可复用同一个client对象
        client = getProgressClient();
    }
    public ProgressDownloader (String url,File destination){
        this.url = url;
        this.destination = destination;
        //在下载、暂停后的继续下载中可复用同一个client对象
        client = getProgressClient();
    }

    //每次下载需要新建新的Call对象
    private Call newCall(long startPoints){
        Request request = new Request.Builder()
                .url(url)
                .header("RANGE","bytes="+startPoints+"-")//断线续传需要用到的，提示下载的区域
                .build();
        return client.newCall(request);
    }

    public OkHttpClient getProgressClient(){
        //拦截器，用上ProgressResponseBody
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse.newBuilder()
                        .body(new ProgressResponseBody(originalResponse.body(),progressResponseListener))
                        .build();
            }
        };
        return new OkHttpClient.Builder()
                .addNetworkInterceptor(interceptor)
//                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)//设置读取超时时间
//                .writeTimeout(WRITE_TIMEOUT,TimeUnit.SECONDS)//设置写的超时时间
//                .connectTimeout(CONNECT_TIMEOUT,TimeUnit.SECONDS)//设置连接超时时间
                .build();
    }

    //startsPoint指定开始下载的点
    public boolean download(final long startsPoint){
        call = newCall(startsPoint);
        try {
            Response response = call.execute();
            if(response!=null&&response.code()==200){
                save(response,0);
                response.close();
            }else{
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return  true;
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                System.out.print(e.toString());
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                save(response, startsPoint);
//            }
//        });
    }

    public void pause(){
        if(call!=null){
            call.cancel();
        }
    }

    private void save(Response response,long startsPoint){
        ResponseBody body = response.body();
        InputStream inputStream = body.byteStream();
//        FileChannel channelOut = null;
//        //随机访问文件，可以指定断点续传的起始位置
//        RandomAccessFile randomAccessFile = null;
        FileOutputStream fileOutputStream = null;
        try{
//            randomAccessFile = new RandomAccessFile(destination,"rwd");
//            //Chanel NIO中的用法，由于RandomAccessFile没有使用缓存策略，直接使用会使得下载速度变慢，亲测缓存下载3.3秒的文件，用普通的RandomAccessFile需要20多秒。
//            channelOut = randomAccessFile.getChannel();
//            //内存映射，直接使用RandomAccessFile，使用其seek方法指定下载的起始位置，使用缓存下载，在这里指定下载位置
//            MappedByteBuffer mappedByteBuffer = channelOut.map(FileChannel.MapMode.READ_WRITE,startsPoint,body.contentLength());
            fileOutputStream = new FileOutputStream(destination);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer))!=-1){
//                ByteBuffer bf  = mappedByteBuffer.put(buffer,0,len);
//                LogUtils.v("while---------" + url+"   " + randomAccessFile.length());
                fileOutputStream.write(buffer, 0, len);
            }
        }catch (Exception e){
            if (progressResponseListener != null) {
                progressResponseListener.FailedDownload();
            }
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
//                if (channelOut != null) {
//                    channelOut.close();
//                }
//                if (randomAccessFile != null) {
//                    randomAccessFile.close();
//                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
