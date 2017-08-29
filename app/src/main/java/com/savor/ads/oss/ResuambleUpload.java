package com.savor.ads.oss;

import android.os.Environment;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.callback.OSSProgressCallback;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.ResumableUploadRequest;
import com.alibaba.sdk.android.oss.model.ResumableUploadResult;
import com.savor.ads.log.LogUploadService;
import com.savor.ads.utils.LogUtils;

import java.io.File;

/**
 * Created by bichao on 12/21/16.
 */
public class ResuambleUpload {

    private OSS oss;
    private String bucketName;
    private String objectKey;
    private String uploadFilePath;
    private LogUploadService.UploadCallback mUploadCallback;
    public ResuambleUpload(OSS client, String bucketName, String objectKey, String uploadFilePath, LogUploadService.UploadCallback result) {
        this.oss = client;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadFilePath = uploadFilePath;
        this.mUploadCallback = result;
    }

    // 异步断点上传，不设置记录保存路径，只在本次上传内做断点续传
    public void resumableUpload() {
        // 创建断点上传请求
        ResumableUploadRequest request = new ResumableUploadRequest(bucketName, objectKey, uploadFilePath);
        // 设置上传过程回调
        request.setProgressCallback(new OSSProgressCallback<ResumableUploadRequest>() {
            @Override
            public void onProgress(ResumableUploadRequest request, long currentSize, long totalSize) {
                LogUtils.d("currentSize: " + currentSize + " totalSize: " + totalSize);
            }
        });
        // 异步调用断点上传
        OSSAsyncTask resumableTask = oss.asyncResumableUpload(request, new OSSCompletedCallback<ResumableUploadRequest, ResumableUploadResult>() {
            @Override
            public void onSuccess(ResumableUploadRequest request, ResumableUploadResult result) {
                LogUtils.d("success!");
                mUploadCallback.isSuccessOSSUpload(true);
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
                mUploadCallback.isSuccessOSSUpload(false);
            }
        });

        resumableTask.waitUntilFinished();
    }

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
}
