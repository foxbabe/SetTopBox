package com.savor.ads.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.savor.ads.BuildConfig;
import com.savor.ads.R;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.StringUtils;

/**
 * Created by zhanghq on 2016/12/12.
 */

public class BoxInfoDialog extends Dialog {

    private TextView mHotelNameTv;
    private TextView mRomVersionTv;
    private TextView mAppVersionTv;
    private TextView mSystemTimeTv;
    private TextView mRoomNameTv;
    private TextView mIpTv;
    private TextView mSignalSourceTv;
    private TextView mEthernetMacTv;
    private TextView mWlanMacTv;
    private TextView mAdsIssueTv;
    private TextView mVodIssueTv;
    private TextView mDownloadingAdsIssueTv;
    private TextView mDownloadingVodIssueTv;
    private TextView mServerIpTv;
    private TextView mLastPowerOnTimeTv;
    private TextView mTvSwitchTimeTv;
    private TextView mVolumeTv;

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
        mIpTv = (TextView) findViewById(R.id.tv_ip);
        mSignalSourceTv = (TextView) findViewById(R.id.tv_signal_source);
        mEthernetMacTv = (TextView) findViewById(R.id.tv_ethernet_mac);
        mWlanMacTv = (TextView) findViewById(R.id.tv_wlan_mac);
        mAdsIssueTv = (TextView) findViewById(R.id.tv_ads_issue);
        mVodIssueTv = (TextView) findViewById(R.id.tv_vod_issue);
        mDownloadingAdsIssueTv = (TextView) findViewById(R.id.tv_downloading_ads_issue);
        mDownloadingVodIssueTv = (TextView) findViewById(R.id.tv_downloading_vod_issue);
        mServerIpTv = (TextView) findViewById(R.id.tv_server_ip);
        mLastPowerOnTimeTv = (TextView) findViewById(R.id.tv_last_power_on_time);
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
        mAppVersionTv.setText(session.getVersionName());
        mSystemTimeTv.setText(AppUtils.getCurTime());
        mVolumeTv.setText(String.valueOf(session.getVolume()));
        mTvSwitchTimeTv.setText(String.valueOf(session.getSwitchTime()));
        String roomName = "";
        if (TextUtils.isEmpty(session.getRoomType())) {
            roomName = session.getBoxName();
        } else {
            roomName = session.getRoomType();
            if (!TextUtils.isEmpty(session.getBoxName())) {
                roomName = roomName + "-" + session.getBoxName();
            }
        }
        mRoomNameTv.setText(roomName);
        mIpTv.setText(AppUtils.getLocalIPAddress());
        mSignalSourceTv.setText(AppUtils.getInputType(session.getTvInputSource()));
        mEthernetMacTv.setText(session.getEthernetMac());
        mWlanMacTv.setText(session.getWlanMac());
        mAdsIssueTv.setText(session.getAdvertMediaPeriod());
        mVodIssueTv.setText(session.getMulticastMediaPeriod());
        mDownloadingAdsIssueTv.setText(session.getAdvertDownloadingPeriod());
        if (!TextUtils.isEmpty(session.getAdvertDownloadingPeriod()) &&
                session.getAdvertDownloadingPeriod().equals(session.getNextAdvertMediaPeriod())) {
            // 下载中期号和下一期期号相同时认为有待播的视频
            mDownloadingAdsIssueTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.mipmap.ic_check, 0);
        } else {
            mDownloadingAdsIssueTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        mDownloadingVodIssueTv.setText(session.getMulticastDownloadingPeriod());
        if (session.getServerInfo() != null) {
            if (Session.get(getContext()).isConnectedToSP()) {
                mServerIpTv.setText(session.getServerInfo().getServerIp());
            } else {
                mServerIpTv.setText(Html.fromHtml("<font color=#E61A6B>"
                        + session.getServerInfo().getServerIp() + "</font> "));
            }
        } else {
            mServerIpTv.setText("");
        }
        mLastPowerOnTimeTv.setText(TextUtils.isEmpty(session.getLastStartTime()) ? "初次开机" : session.getLastStartTime());
    }
}
