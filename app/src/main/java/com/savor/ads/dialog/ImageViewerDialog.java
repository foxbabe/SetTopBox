package com.savor.ads.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.savor.ads.R;
import com.savor.ads.utils.GlideImageLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ImageViewerDialog extends Dialog {

    private static final int SWITCH_TIME = 1000 * 10;

    private ViewPager mImagesVp;
    private TextView mIndexerTv;

    private Context mContext;
    private List<String> mDataSource;

    private int mCurrentIndex;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int index = mCurrentIndex + 1;
            index = index < mDataSource.size() ? index : 0;
            mImagesVp.setCurrentItem(index, true);
            mHandler.sendEmptyMessageDelayed(0, SWITCH_TIME);
            return true;
        }
    });
    private StringPagerAdapter mAdapter;

    public ImageViewerDialog(Context context, List<String> dataSource, int currentIndex) {
        super(context, R.style.image_pager_dialog_theme);
        mContext = context;
        mDataSource = dataSource;
        mCurrentIndex = currentIndex;
        if (mCurrentIndex < 0 || mCurrentIndex >= mDataSource.size()) {
            mCurrentIndex = 0;
        }
    }

//    public static void showDialog(Context context, List<String> dataSource, int currentIndex) {
//        if (dataSource == null || dataSource.size() <= 0) {
//            return;
//        }
//        if (currentIndex > dataSource.size()) {
//            currentIndex = 0;
//        }
//        ImageViewerDialog dialog = new ImageViewerDialog(context, dataSource, currentIndex);
//        dialog.show();
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_image_view_pager);

        RelativeLayout rootLayout = (RelativeLayout) findViewById(R.id.rl_root);
        rootLayout.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ImageViewerDialog.this.dismiss();
            }
        });
        mImagesVp = (ViewPager) findViewById(R.id.vp_images);
        mIndexerTv = (TextView) findViewById(R.id.tv_indexer);

        mIndexerTv.setText(mCurrentIndex + "/" + mDataSource.size());
        if (mDataSource.size() <= 1) {
            mIndexerTv.setVisibility(View.GONE);
        }

        mAdapter = new StringPagerAdapter(mContext, mDataSource);
        mImagesVp.setAdapter(mAdapter);
        mImagesVp.setCurrentItem(mCurrentIndex);
        mImagesVp.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int arg0) {
                mCurrentIndex = arg0;
                mIndexerTv.setText(mCurrentIndex + "/" + mDataSource.size());
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {

            }

            @Override
            public void onPageScrollStateChanged(int arg0) {

            }
        });

        setDialogAttributes();
    }

    @Override
    public void show() {
        super.show();
        if (mDataSource != null && mDataSource.size() > 0) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.sendEmptyMessageDelayed(0, SWITCH_TIME);
        }
    }

    private void setDialogAttributes() {
        Window window = getWindow(); // 得到对话框
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.width = WindowManager.LayoutParams.MATCH_PARENT;
        wl.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(wl);
    }

    public void resetImages(ArrayList<String> usbImgPathList, int currentIndex) {
        mAdapter.setDataSource(usbImgPathList);
        mImagesVp.setCurrentItem(currentIndex, false);
        mCurrentIndex = currentIndex;
        if (mCurrentIndex < 0 || mCurrentIndex >= mDataSource.size()) {
            mCurrentIndex = 0;
        }
    }

    class StringPagerAdapter extends PagerAdapter {

        private Context mContext;
        private List<String> mImages;

        private HashMap<Integer, View> mViewList;

        public StringPagerAdapter(Context context, List<String> images) {
            mContext = context;
            mImages = images;
            mViewList = new HashMap<Integer, View>();
        }

        @Override
        public int getCount() {
            return mImages == null ? 0 : mImages.size();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mViewList.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final View view = View.inflate(mContext, R.layout.view_image_item,
                    null);
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
//            imageView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    ImageViewerDialog.this.dismiss();
//                }
//            });
            GlideImageLoader.loadImageWithoutCache(mContext, mImages.get(position), imageView);
            mViewList.put(position, view);
            container.addView(view);
            return view;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        public void setDataSource(ArrayList<String> usbImgPathList) {
            mImages = usbImgPathList;
            notifyDataSetChanged();
        }
    }
}
