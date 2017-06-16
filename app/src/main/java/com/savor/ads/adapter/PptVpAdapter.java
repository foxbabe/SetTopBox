package com.savor.ads.adapter;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.savor.ads.R;
import com.savor.ads.bean.PptImage;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.GlideImageLoader;
import com.savor.ads.utils.GlobalValues;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by zhang.haiqiang on 2017/6/13.
 */

public class PptVpAdapter extends PagerAdapter {

    private Context mContext;
    private ArrayList<PptImage> pptImages;

    private HashMap<Integer, View> mViewList;

    public PptVpAdapter(Context mContext, ArrayList<PptImage> pptImages) {
        this.mContext = mContext;
        this.pptImages = pptImages;
        mViewList = new HashMap<Integer, View>();
    }

    @Override
    public int getCount() {
        if (pptImages == null) {
            return 0;
        } else {
//            if (pptImages.size() == 1) {
//                return 1;
//            } else {
//                return Integer.MAX_VALUE;
//            }
            return pptImages.size();
        }
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
//        position = position % pptImages.size();
        container.removeView(mViewList.get(position));
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
//        position = position % pptImages.size();
        final View view = View.inflate(mContext, R.layout.view_image_item,
                null);
        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.ppt) + GlobalValues.CURRENT_PROJECT_DEVICE_ID + File.separator + pptImages.get(position).getName();
        GlideImageLoader.loadImage(mContext, path, imageView, 0, 0);
        mViewList.put(position, view);
        container.addView(view);
        return view;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public void setDataSource(ArrayList<PptImage> pptImages) {
        this.pptImages = pptImages;
        mViewList.clear();
        notifyDataSetChanged();
    }
}
