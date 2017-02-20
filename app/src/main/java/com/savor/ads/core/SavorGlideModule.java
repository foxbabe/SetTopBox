package com.savor.ads.core;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.GlideModule;
import com.savor.ads.utils.OkHttpUrlLoader;

import java.io.InputStream;

/**
 * Created by zhanghq on 2017/1/17.
 */

public class SavorGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
//        builder.setDecodeFormat(DecodeFormat.PREFER_RGB_565);
    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        glide.register(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory());
    }
}
