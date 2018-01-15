package com.savor.ads.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.savor.ads.BuildConfig;
import com.savor.ads.R;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.DensityUtil;
import com.savor.ads.utils.StringUtils;

/**
 * Created by zhanghq on 2016/12/12.
 */

public class BoxInfoDialog extends Dialog {

    private TextView mHotelNameTv;
    private TextView mRomVersionTv;
    private TextView mAppVersionTv;
    private TextView mSystemTimeTv;
    private TextView mRoomTypeTv;
    private TextView mRoomNameTv;
    private TextView mSignalSourceTv;
    private TextView mTvSwitchTimeTv;
    private TextView mEthernetMacTv;
    private TextView mWlanMacTv;
    private TextView mWlanMacLabelTv;
    private TextView mEthernetIpTv;
    private TextView mWlanIpLabelTv;
    private TextView mWlanIpTv;
    private TextView mAdsPeriodTv;
    private TextView mVodPeriodTv;
    private TextView mProPeriodTv;
    private TextView mAdvPeriodTv;
    private TextView mAdsDownloadPeriodTv;
    private TextView mVodDownloadPeriodTv;
    private TextView mProDownloadPeriodTv;
    private TextView mAdvDownloadPeriodTv;
    private TextView mLogoPeriodTv;
    private TextView mLoadingPeriodTv;
    private TextView mServerIpTv;
    private TextView mLastPowerOnTimeTv;
    private TextView mVolumeTv;

    private TextView mProjectVolumeTv;
    private TextView mVodVolumeTv;
    private TextView mTvVolumeTv;

    public BoxInfoDialog(Context context) {
        super(context, R.style.box_info_dialog_theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_box_info);
        setDialogAttributes();

        mHotelNameTv = (TextView) findViewById(R.id.tv_hotel_name);
        mRomVersionTv = (TextView) findViewById(R.id.tv_rom_version);
        mAppVersionTv = (TextView) findViewById(R.id.tv_app_version);
        mSystemTimeTv = (TextView) findViewById(R.id.tv_sys_time);
        mVolumeTv = (TextView) findViewById(R.id.tv_volume);
        mTvSwitchTimeTv = (TextView) findViewById(R.id.tv_switch_tv_time);
        mRoomNameTv = (TextView) findViewById(R.id.tv_room_name);
        mEthernetIpTv = (TextView) findViewById(R.id.tv_eth_ip);
        mWlanIpTv = (TextView) findViewById(R.id.tv_wlan_ip);
        mWlanMacLabelTv = (TextView) findViewById(R.id.tv_wlan_ip_label);
        mSignalSourceTv = (TextView) findViewById(R.id.tv_signal_source);
        mEthernetMacTv = (TextView) findViewById(R.id.tv_ethernet_mac);
        mWlanMacTv = (TextView) findViewById(R.id.tv_wlan_mac);
        mWlanMacLabelTv = (TextView) findViewById(R.id.tv_wlan_mac_label);
        mAdsPeriodTv = (TextView) findViewById(R.id.tv_ads_period);
        mVodPeriodTv = (TextView) findViewById(R.id.tv_vod_period);
        mProPeriodTv = (TextView) findViewById(R.id.tv_pro_period);
        mAdvPeriodTv = (TextView) findViewById(R.id.tv_adv_period);
        mLogoPeriodTv = (TextView) findViewById(R.id.tv_logo_version);
        mLoadingPeriodTv = (TextView) findViewById(R.id.tv_loading_version);
        mServerIpTv = (TextView) findViewById(R.id.tv_server_ip);
        mLastPowerOnTimeTv = (TextView) findViewById(R.id.tv_last_power_on_time);
        mProjectVolumeTv = (TextView) findViewById(R.id.tv_project_volume);
        mVodVolumeTv = (TextView) findViewById(R.id.tv_vod_volume);
        mTvVolumeTv = (TextView) findViewById(R.id.tv_tv_volume);
        mRoomTypeTv = (TextView) findViewById(R.id.tv_room_type);
        mAdsDownloadPeriodTv = (TextView) findViewById(R.id.tv_ads_download_period);
        mAdvDownloadPeriodTv = (TextView) findViewById(R.id.tv_adv_download_period);
        mProDownloadPeriodTv = (TextView) findViewById(R.id.tv_pro_download_period);
        mVodDownloadPeriodTv = (TextView) findViewById(R.id.tv_vod_download_period);
    }


    private void setDialogAttributes() {
        Window window = getWindow(); // 得到对话框
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.width = WindowManager.LayoutParams.MATCH_PARENT;
        wl.height = WindowManager.LayoutParams.MATCH_PARENT;
        wl.gravity = Gravity.CENTER;
        window.setAttributes(wl);
    }

    @Override
    public void show() {
        super.show();

        Session session = Session.get(getContext());
        mHotelNameTv.setText(session.getBoiteName());
        mRomVersionTv.setText(session.getRomVersion());
        mAppVersionTv.setText(session.getVersionName() + "_" + session.getVersionCode());
        mSystemTimeTv.setText(AppUtils.getCurTime());
        mSignalSourceTv.setText(AppUtils.getInputType(session.getTvInputSource()));
        mTvSwitchTimeTv.setText(String.valueOf(session.getSwitchTime()));
        mRoomNameTv.setText(session.getBoxName());
        mRoomTypeTv.setText(session.getRoomType());
        mEthernetIpTv.setText(AppUtils.getEthernetIP());
        mEthernetMacTv.setText(session.getEthernetMac());
        if (session.isStandalone()) {
            mWlanIpLabelTv.setText("U盘更新时间");
            mWlanIpTv.setText(session.getLastUDiskUpdateTime());
            mWlanMacLabelTv.setText("是否单机版");
            mWlanMacTv.setText("是");
        } else {
            mWlanIpTv.setText("无线IP地址");
            mWlanIpTv.setText(AppUtils.getWlanIP());
            mWlanMacTv.setText("无线MAC地址");
            mWlanMacTv.setText(session.getWlanMac());
        }
        mAdsPeriodTv.setText(session.getAdsPeriod());
        mVodPeriodTv.setText(session.getVodPeriod());
        mAdvPeriodTv.setText(session.getAdvPeriod());
        mProPeriodTv.setText(session.getProPeriod());
        mLogoPeriodTv.setText(session.getSplashVersion());
        mLoadingPeriodTv.setText(session.getLoadingVersion());
        mAdsDownloadPeriodTv.setText(session.getAdsDownloadPeriod());
        mAdvDownloadPeriodTv.setText(session.getAdvDownloadPeriod());
        mProDownloadPeriodTv.setText(session.getProDownloadPeriod());
        mVodDownloadPeriodTv.setText(session.getVodDownloadPeriod());

        if (!TextUtils.isEmpty(session.getProNextMediaPubTime())) {
            if (!TextUtils.isEmpty(session.getAdsNextPeriod()) && session.getAdsNextPeriod().equals(session.getAdsDownloadPeriod())) {
                mAdsDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.ic_check, 0);
            } else {
                mAdsDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            if (!TextUtils.isEmpty(session.getAdvNextPeriod()) && session.getAdvNextPeriod().equals(session.getAdvDownloadPeriod())) {
                mAdvDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.ic_check, 0);
            } else {
                mAdvDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            if (!TextUtils.isEmpty(session.getProNextPeriod()) && session.getProNextPeriod().equals(session.getProDownloadPeriod())) {
                mProDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.ic_check, 0);
            } else {
                mProDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
        } else {
            mAdsDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            mAdvDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            mProDownloadPeriodTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        if (session.getServerInfo() != null) {
            if (session.isConnectedToSP()) {
                mServerIpTv.setText(session.getServerInfo().getServerIp());
            } else {
                mServerIpTv.setText(Html.fromHtml("<font color=#E61A6B>"
                        + session.getServerInfo().getServerIp() + "</font> "));
            }
            if (session.getServerInfo().getSource() == 3) {
                mServerIpTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.ic_manual, 0);
            } else {
                mServerIpTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.ic_auto, 0);
            }
        } else {
            mServerIpTv.setText("");
            mServerIpTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
        mLastPowerOnTimeTv.setText(TextUtils.isEmpty(session.getLastStartTime()) ? "初次开机" : session.getLastStartTime());
        mVolumeTv.setText(String.valueOf(session.getVolume()));
        mProjectVolumeTv.setText(String.valueOf(session.getProjectVolume()));
        mVodVolumeTv.setText(String.valueOf(session.getVodVolume()));
        mTvVolumeTv.setText(String.valueOf(session.getTvVolume()));

    }
}
