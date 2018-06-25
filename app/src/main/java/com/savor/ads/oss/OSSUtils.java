package com.savor.ads.oss;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.GetObjectRequest;
import com.alibaba.sdk.android.oss.model.GetObjectResult;
import com.alibaba.sdk.android.oss.model.ObjectMetadata;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.alibaba.sdk.android.oss.model.Range;
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest;
import com.alibaba.sdk.android.oss.model.ResumableUploadResult;
import com.savor.ads.BuildConfig;
import com.savor.ads.core.Session;
import com.savor.ads.log.LogUploadService;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;

/**
 * Created by bichao on 12/21/16.
 */
public class OSSUtils {
    private Context context;
    private OSS oss;
    /**桶名称**/
    private String bucketName;
    /**阿里云上传日志所用**/
    private String objectKey;
    /**阿里云下载视频所用*/
    private String objectKey2;
    private String uploadFilePath;
    private LogUploadService.UploadCallback mUploadCallback;
    private File localFile;
    private boolean isDownloaded;
    public OSSUtils(Context context,String bucketName, String objectKey, String uploadFilePath, LogUploadService.UploadCallback result) {
        this.context = context;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadFilePath = uploadFilePath;
        this.mUploadCallback = result;
        initOSSClient();
    }

    public OSSUtils(Context context,String bucketName, String objectKey,File file) {
        this.context = context;
        this.bucketName = bucketName;
        this.objectKey2 = objectKey;
        this.localFile = file;
        initOSSClient();
    }

    void initOSSClient(){
        if (oss!=null){
            return;
        }
        OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(OSSValues.accessKeyId, OSSValues.accessKeySecret);

        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
        conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
        conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
        conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
        OSSLog.enableLog();
        oss = new OSSClient(context, BuildConfig.OSS_ENDPOINT, credentialProvider, conf);
    }
    // 异步断点上传，不设置记录保存路径，只在本次上传内做断点续传
//    public void resumableUpload() {
//        // 创建断点上传请求
//        ResumableUploadRequest request = new ResumableUploadRequest(bucketName, objectKey, uploadFilePath);
//        // 设置上传过程回调
//        request.setProgressCallback(new OSSProgressCallback<ResumableUploadRequest>() {
//            @Override
//            public void onProgress(ResumableUploadRequest request, long currentSize, long totalSize) {
//                LogUtils.d("currentSize: " + currentSize + " totalSize: " + totalSize);
//            }
//        });
//        // 异步调用断点上传
//        OSSAsyncTask resumableTask = oss.asyncResumableUpload(request, new OSSCompletedCallback<ResumableUploadRequest, ResumableUploadResult>() {
//            @Override
//            public void onSuccess(ResumableUploadRequest request, ResumableUploadResult result) {
//                LogUtils.d("success!");
//                mUploadCallback.isSuccessOSSUpload(true);
//            }
//
//            @Override
//            public void onFailure(ResumableUploadRequest request, ClientException clientExcepion, ServiceException serviceException) {
//                // 请求异常
//                if (clientExcepion != null) {
//                    // 本地异常如网络异常等
//                    clientExcepion.printStackTrace();
//                }
//                if (serviceException != null) {
//                    // 服务异常
//                    LogUtils.e("ErrorCode" + serviceException.getErrorCode());
//                    LogUtils.e("RequestId" + serviceException.getRequestId());
//                    LogUtils.e("HostId" + serviceException.getHostId());
//                    LogUtils.e("RawMessage" + serviceException.getRawMessage());
//                }
//                mUploadCallback.isSuccessOSSUpload(false);
//            }
//        });
//
//        resumableTask.waitUntilFinished();
//    }

    // 异步断点上传，设置记录保存路径，即使任务失败，下次启动仍能继续
    public void resumableUploadWithRecordPathSetting() {

        String recordDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/oss_record/";

        File recordDir = new File(recordDirectory);

        // 要保证目录存在，如果不存在则主动创建
        if (!recordDir.exists()) {
            recordDir.mkdirs();
        }

        // 创建断点上传请求，参数中给出断点记录文件的保存位置，需是一个文件夹的绝对路径
        ResumableUploadRequest request = new ResumableUploadRequest(bucketName, objectKey, uploadFilePath, recordDirectory);
        // 设置上传过程回调
        request.setProgressCallback(new OSSProgressCallback<ResumableUploadRequest>() {
            @Override
            public void onProgress(ResumableUploadRequest request, long currentSize, long totalSize) {
                LogUtils.d("currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });


        OSSAsyncTask resumableTask = oss.asyncResumableUpload(request, new OSSCompletedCallback<ResumableUploadRequest, ResumableUploadResult>() {
            @Override
            public void onSuccess(ResumableUploadRequest request, ResumableUploadResult result) {
                LogUtils.d("success!");
            }

            @Override
            public void onFailure(ResumableUploadRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    LogUtils.e("ErrorCode" + serviceException.getErrorCode());
                    LogUtils.e("RequestId" + serviceException.getRequestId());
                    LogUtils.e("HostId" + serviceException.getHostId());
                    LogUtils.e("RawMessage" + serviceException.getRawMessage());
                }
            }
        });

        resumableTask.waitUntilFinished();
    }

    /**
     * 阿里云OSS异步上传文件
     */
    public void asyncUploadFile(){
        // 构造上传请求
        PutObjectRequest put = new PutObjectRequest(bucketName, objectKey, uploadFilePath);
        // 异步上传时可以设置进度回调
        put.setProgressCallback(new OSSProgressCallback<PutObjectRequest>() {
            @Override
            public void onProgress(PutObjectRequest request, long currentSize, long totalSize) {
                Log.d("PutObject", "currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });
        OSSAsyncTask task = oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d("PutObject", "UploadSuccess");
                Log.d("ETag", result.getETag());
                Log.d("RequestId", result.getRequestId());
                mUploadCallback.isSuccessOSSUpload(true);
            }
            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
                mUploadCallback.isSuccessOSSUpload(false);
            }
        });
    }
    /**
     * OSS同步下载方法
     */
    public boolean syncDownload() {
        isDownloaded = false;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        //构造下载文件请求
        GetObjectRequest get = new GetObjectRequest(bucketName, objectKey2);
        //设置下载进度回调
        get.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                OSSLog.logDebug("getobj_progress: " + currentSize + "  total_size: " + totalSize, false);
            }
        });
        try {
            // 同步执行下载请求，返回结果
            GetObjectResult getResult = oss.getObject(get);
            Log.d("Content-Length", "" + getResult.getContentLength());
            outputStream = new FileOutputStream(localFile);
            // 获取文件输入流
            inputStream = getResult.getObjectContent();
            byte[] buffer = new byte[2048];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                // 处理下载的数据，比如图片展示或者写入文件等
                outputStream.write(buffer, 0, len);
                outputStream.flush();
            }
            // 下载后可以查看文件元信息
            ObjectMetadata metadata = getResult.getMetadata();
            Log.d("ContentType", metadata.getContentType());
            isDownloaded = true;
        } catch (ClientException e1) {
            // 本地异常如网络异常等
            e1.printStackTrace();
        } catch (ServiceException e2) {
            // 服务异常
            Log.e("RequestId", e2.getRequestId());
            Log.e("ErrorCode", e2.getErrorCode());
            Log.e("HostId", e2.getHostId());
            Log.e("RawMessage", e2.getRawMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (inputStream!=null){
                    inputStream.close();
                }
                if (outputStream!=null){
                    outputStream.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return isDownloaded;
    }
    /**
     * OSS异步下载方法
     */
    public void asyncDownload(){
        GetObjectRequest get = new GetObjectRequest(bucketName, objectKey2);
        //设置下载进度回调
        get.setProgressListener(new OSSProgressCallback<GetObjectRequest>() {
            @Override
            public void onProgress(GetObjectRequest request, long currentSize, long totalSize) {
                OSSLog.logDebug("getobj_progress: " + currentSize+"  total_size: " + totalSize, false);
            }
        });
        OSSAsyncTask task = oss.asyncGetObject(get, new OSSCompletedCallback<GetObjectRequest, GetObjectResult>() {
            @Override
            public void onSuccess(GetObjectRequest request, GetObjectResult result) {
                FileOutputStream outputStream = null;
                // 请求成功
                InputStream inputStream = result.getObjectContent();
                byte[] buffer = new byte[2048];
                int len;
                try {
                    while ((len = inputStream.read(buffer)) != -1) {
                        // 处理下载的数据
                        outputStream.write(buffer, 0, len);
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onFailure(GetObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                // 请求异常
                if (clientExcepion != null) {
                    // 本地异常如网络异常等
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });
    }

    /**
     * 指定范围下载
     * @param targetFile
     * @return
     */
    public boolean specifiedRangeDownload(final String targetFile){
        isDownloaded = false;
        final String fileName = new File(targetFile).getName();
        long pos =0;
        final Session session = Session.get(context);

        RandomAccessFile randomAccessFile=null;
        try{
            GetObjectRequest get = new GetObjectRequest(bucketName, objectKey2);

            if (AppUtils.isFileExist(targetFile)){
//                randomAccessFile = new RandomAccessFile(new File(targetFile),"rwd");
                HashMap<String,Long> hashMap = session.getDownloadFilePosition();
                if (hashMap.containsKey(fileName)){
                    pos = hashMap.get(fileName);
                }
            }else{
                // 同步执行下载请求，返回结果
                GetObjectResult getResult = oss.getObject(get);
                Log.d("Content-Length", "" + getResult.getContentLength());
                randomAccessFile = new RandomAccessFile(targetFile,"rwd");
                randomAccessFile.setLength(getResult.getContentLength());
            }

            // 设置范围
            get.setRange(new Range(pos, Range.INFINITE)); // 下载0到99字节共100个字节，文件范围从0开始计算
            // get.setRange(new Range(100, Range.INFINITE)); // 下载从100个字节到结尾
            OSSAsyncTask task = oss.asyncGetObject(get, new OSSCompletedCallback<GetObjectRequest, GetObjectResult>() {
                @Override
                public void onSuccess(GetObjectRequest request, GetObjectResult result) {
                    long length=0;
                    HashMap<String,Long> hashMap = session.getDownloadFilePosition();
                    if (hashMap.containsKey(fileName)){
                        length = hashMap.get(fileName);
                    }

                    RandomAccessFile randomAccessFile =null;
                    InputStream inputStream = null;
//                    FileOutputStream outputStream = null;
                    //记录下载位置长度
                    try {
//                        randomAccessFile = new RandomAccessFile(new File(filePath),"rwd");
                        randomAccessFile = new RandomAccessFile(targetFile,"rwd");
                        if (length!=0){
                            randomAccessFile.seek(length);
                        }
//                        outputStream = new FileOutputStream(localFile);
                        // 请求成功
                        inputStream = result.getObjectContent();
                        byte[] buffer = new byte[2048];
                        int len;
                        while ((len = inputStream.read(buffer)) != -1) {
                            // 处理下载的数据
//                            outputStream.write(buffer, 0, len);
                            randomAccessFile.write(buffer,0,len);
//                            randomAccessFile.flush();
                            length = length+len;

                        }
                        isDownloaded = true;
                        hashMap.remove(fileName);
                    } catch (IOException e) {
                        hashMap.put(fileName,length);
                        e.printStackTrace();
                        isDownloaded = false;
                    }finally {
                        try {
                            if (inputStream != null){
                                inputStream.close();
                            }
//                            if (outputStream != null){
//                                outputStream.close();
//                            }
                        }catch (IOException e){
                            e.printStackTrace();
                        }

                    }
                }
                @Override
                public void onFailure(GetObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                    // 请求异常
                    if (clientExcepion != null) {
                        // 本地异常如网络异常等
                        clientExcepion.printStackTrace();
                    }
                    if (serviceException != null) {
                        // 服务异常
                        Log.e("ErrorCode", serviceException.getErrorCode());
                        Log.e("RequestId", serviceException.getRequestId());
                        Log.e("HostId", serviceException.getHostId());
                        Log.e("RawMessage", serviceException.getRawMessage());
                    }
                }
            });
        }catch (ClientException e){
            e.printStackTrace();
        }catch (ServiceException e2) {
            // 服务异常
            Log.e("RequestId", e2.getRequestId());
            Log.e("ErrorCode", e2.getErrorCode());
            Log.e("HostId", e2.getHostId());
            Log.e("RawMessage", e2.getRawMessage());
        }catch (Exception e){
            e.printStackTrace();
        }

        return isDownloaded;
    }
}
