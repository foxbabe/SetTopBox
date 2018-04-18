package com.savor.ads.utils;

import android.text.TextUtils;

import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.AppServiceOk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RetryHandler {

    private static volatile boolean mIsRunning = false;
    private static List<RequestWrap> mRequestWraps;

    public static void enqueue(String url, int endTime) {
        if (mRequestWraps == null) {
            mRequestWraps = Collections.synchronizedList(new ArrayList<RequestWrap>());
        }

        mRequestWraps.add(new RequestWrap(url, -1, endTime));

        doRequest();
    }

    private static void doRequest() {
        if (!mIsRunning) {
            mIsRunning = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mIsRunning && mRequestWraps != null) {
                        LogUtils.d("开始执行请求，数量：" + mRequestWraps.size());

                        for (final RequestWrap requestWrap : mRequestWraps) {
                            if (requestWrap != null && !requestWrap.done &&
                                    !TextUtils.isEmpty(requestWrap.url) &&
                                    requestWrap.endTime > System.currentTimeMillis() / 1000) {
                                new AppServiceOk(null, new ApiRequestListener() {
                                    @Override
                                    public void onSuccess(AppApi.Action method, Object obj) {
                                        LogUtils.d("请求成功！" + requestWrap.url);
                                        requestWrap.retryCount--;
                                        requestWrap.done = true;
                                    }

                                    @Override
                                    public void onError(AppApi.Action method, Object obj) {
                                        LogUtils.d("请求失败！" + requestWrap.url);
                                        requestWrap.retryCount--;
                                    }

                                    @Override
                                    public void onNetworkFailed(AppApi.Action method) {
                                        LogUtils.d("请求失败！" + requestWrap.url);
                                        requestWrap.retryCount--;
                                    }
                                }).simpleGet(requestWrap.url);
                            }
                        }

                        Iterator<RequestWrap> interator = mRequestWraps.iterator();
                        while (interator.hasNext()) {
                            if (interator.next().done) {
                                interator.remove();
                            }
                        }

                        if (mRequestWraps.isEmpty()) {
                            mIsRunning = false;
                            break;
                        } else {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        }
    }

    private static class RequestWrap {
        String url;
        int retryCount;
        int endTime;
        boolean done;

        private RequestWrap(String url, int retryCount, int endTime) {
            this.url = url;
            this.retryCount = retryCount;
            this.endTime = endTime;
        }
    }
}
