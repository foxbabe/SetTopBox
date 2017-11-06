package com.savor.ads.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.DrawableRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

/**
 * Glide图片加载工具类
 * 1、调用者须确保在主线程调用load***相关方法
 * 2、应用于CircleImageView等圆形ImageView时须使用禁用动画效果
 * <p>
 * 详细说明文档参见：https://github.com/bumptech/glide
 * Created by zhanghq on 2016/6/25.
 */
public class GlideImageLoader {

    private static int globalPlaceholderResId;
    private static int globalFailedResId;

    public static void setGlobalPlaceholderResId(int globalPlaceholderResId) {
        GlideImageLoader.globalPlaceholderResId = globalPlaceholderResId;
    }

    public static void setGlobalFailedResId(int globalFailedResId) {
        GlideImageLoader.globalFailedResId = globalFailedResId;
    }

    public static void clearView(View view) {
        Glide.clear(view);
    }

    public static void clearCache(final Context context, boolean memory, boolean disk) {
        if (memory) {
            Glide.get(context).clearMemory();
        }
        if (disk) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Glide.get(context).clearDiskCache();
                }
            }).start();
        }
    }

    public static void loadImage(Context context, String imgPath, ImageView imageView) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        loadImage(appContext, imgPath, imageView, globalPlaceholderResId, globalFailedResId);
    }

    public static void loadImageWithoutCache(Context context, String imgPath, ImageView imageView, int placeholderResId, int failedResId) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        Glide.with(appContext)
                .load(imgPath)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(placeholderResId)
                .error(failedResId)
                .into(imageView);
    }

    public static void loadImageWithoutCache(Context context, String imgPath, ImageView imageView, RequestListener listener) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        Glide.with(appContext)
                .load(imgPath)
                .listener(listener)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(imageView);
    }


    public static void loadImage(Context context, String imgPath, ImageView imageView, int placeHolderId, RequestListener listener) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        loadImage(appContext, imgPath, imageView, placeHolderId, placeHolderId, listener);
    }

    public static void loadImage(Context context, String imgPath, ImageView imageView, int placeholderResId, int failedResId) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        Glide.with(appContext)
                .load(imgPath)
                .placeholder(placeholderResId)
                .error(failedResId)
                .crossFade()
                .into(imageView);
    }

    public static void loadImage(Context context, String imgPath, ImageView imageView, int placeholderResId, int failedResId, RequestListener listener) {
        if (!(listener instanceof RequestListener))
            throw new RuntimeException("this listener is not RequestListener type!");

        Glide.with(context)
                .load(imgPath).listener(listener)
                .placeholder(placeholderResId)
                .error(failedResId)
                .crossFade()
                .into(imageView);
    }

    public static void loadImageWithThumbnail(Context context, String imgPath, String thumbnailPath, ImageView imageView, int placeholderResId, int failedResId, RequestListener listener) {
        if (!(listener instanceof RequestListener))
            throw new RuntimeException("this listener is not RequestListener type!");

        if (!TextUtils.isEmpty(thumbnailPath)) {
            DrawableRequestBuilder<String> thumbnailRequest = Glide.with(context)
                    .load(thumbnailPath);
            Glide.with(context)
                    .load(imgPath)
                    .listener(listener)
                    .placeholder(placeholderResId)
                    .error(failedResId)
                    .crossFade()
                    .thumbnail(thumbnailRequest)
                    .into(imageView);
        } else {
            Glide.with(context)
                    .load(imgPath)
                    .listener(listener)
                    .placeholder(placeholderResId)
                    .error(failedResId)
                    .crossFade()
                    .into(imageView);
        }
    }

    public static void loadImageWithNoAnimate(Context context, String imgPath, ImageView imageView, int placeholderResId, int failedResId) {
        Glide.with(context)
                .load(imgPath)
                .placeholder(placeholderResId)
                .error(failedResId)
                .dontAnimate()
                .crossFade()
                .into(imageView);
    }

    public static void loadImage(Fragment fragment, String imgPath, ImageView imageView) {
        loadImage(fragment, imgPath, imageView, globalPlaceholderResId, globalFailedResId);
    }

    public static void loadImage(Fragment fragment, String imgPath, ImageView imageView, int placeholderResId, int failedResId) {
        Glide.with(fragment)
                .load(imgPath)
                .placeholder(placeholderResId)
                .error(failedResId)
                .crossFade()
                .into(imageView);
    }

    public static void loadImageWithNoAnimate(Fragment fragment, String imgPath, ImageView imageView, int placeholderResId, int failedResId) {
        Glide.with(fragment)
                .load(imgPath)
                .placeholder(placeholderResId)
                .error(failedResId)
                .dontAnimate()
                .crossFade()
                .into(imageView);
    }
}
