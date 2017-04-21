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
    private TextView mRoomNameTv;
    private TextView mSignalSourceTv;
    private TextView mTvSwitchTimeTv;
    private TextView mEthernetMacTv;
    private TextView mWlanMacTv;
    private TextView mEthernetIpTv;
    private TextView mWlanIpTv;
    private TextView mAdsPeriodTv;
    private TextView mVodPeriodTv;
    private TextView mProPeriodTv;
    private TextView mAdvPeriodTv;
    private TextView mLogoPeriodTv;
    private TextView mLoadingPeriodTv;
    private TextView mServerIpTv;
    private TextView mLastPowerOnTimeTv;
    private TextView mVolumeTv;

    private TextView mProjectVolumeTv;
    private TextView mVodVolumeTv;
    private TextView mTvVolumeTv;

    private LinearLayout mDownloadingPlaylistLl;
    private LinearLayout mDownloadingVodLl;
    private LinearLayout mPreparedPlaylistLl;

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
        mSignalSourceTv = (TextView) findViewById(R.id.tv_signal_source);
        mEthernetMacTv = (TextView) findViewById(R.id.tv_ethernet_mac);
        mWlanMacTv = (TextView) findViewById(R.id.tv_wlan_mac);
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

        mDownloadingPlaylistLl = (LinearLayout) findViewById(R.id.ll_downloading_playlist);
        mDownloadingVodLl = (LinearLayout) findViewById(R.id.ll_downloading_vod);
        mPreparedPlaylistLl = (LinearLayout) findViewById(R.id.ll_prepared_playlist);
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
        mEthernetIpTv.setText(AppUtils.getEthernetIP());
        mWlanIpTv.setText(AppUtils.getWlanIP());
        mEthernetMacTv.setText(session.getEthernetMac());
        mWlanMacTv.setText(session.getWlanMac());
        mAdsPeriodTv.setText(session.getAdsPeriod());
        mVodPeriodTv.setText(session.getVodPeriod());
        mAdvPeriodTv.setText(session.getAdvPeriod());
        mProPeriodTv.setText(session.getProPeriod());
        mLogoPeriodTv.setText(session.getSplashVersion());
        mLoadingPeriodTv.setText(session.getLoadingVersion());

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
        mVolumeTv.setText(String.valueOf(session.getVolume()));
        mProjectVolumeTv.setText(String.valueOf(session.getProjectVolume()));
        mVodVolumeTv.setText(String.valueOf(session.getVodVolume()));
        mTvVolumeTv.setText(String.valueOf(session.getTvVolume()));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.weight = 1;
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params2.weight = 1;
        params2.leftMargin = DensityUtil.dip2px(getContext(), 40);

        mDownloadingPlaylistLl.removeAllViews();
        if (session.getDownloadingPlayListVersion() != null && !session.getDownloadingPlayListVersion().isEmpty()) {
            for (int i = 0; i < session.getDownloadingPlayListVersion().size(); i += 2) {
                LinearLayout linearLayout = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_row, null);

                LinearLayout itemLl = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_item, null);
                TextView labelTv = (TextView) itemLl.findViewById(R.id.tv_label);
                TextView contentTv = (TextView) itemLl.findViewById(R.id.tv_content);
                labelTv.setText(session.getDownloadingPlayListVersion().get(i).getLabel());
                contentTv.setText(session.getDownloadingPlayListVersion().get(i).getVersion());
                linearLayout.addView(itemLl, params);

                if (i + 1 < session.getDownloadingPlayListVersion().size()) {
                    LinearLayout itemLl2 = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_item, null);
                    TextView labelTv2 = (TextView) itemLl2.findViewById(R.id.tv_label);
                    TextView contentTv2 = (TextView) itemLl2.findViewById(R.id.tv_content);
                    labelTv2.setText(session.getDownloadingPlayListVersion().get(i + 1).getLabel());
                    contentTv2.setText(session.getDownloadingPlayListVersion().get(i + 1).getVersion());
                    linearLayout.addView(itemLl2, params2);
                } else {
                    View view = new View(getContext());
                    linearLayout.addView(view, params2);
                }

                mDownloadingPlaylistLl.addView(linearLayout);
            }
        }

        mDownloadingVodLl.removeAllViews();
        if (session.getDownloadingVodVersion() != null && !session.getDownloadingVodVersion().isEmpty()) {
            for (int i = 0; i < session.getDownloadingVodVersion().size(); i += 2) {
                LinearLayout linearLayout = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_row, null);

                LinearLayout itemLl = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_item, null);
                TextView labelTv = (TextView) itemLl.findViewById(R.id.tv_label);
                TextView contentTv = (TextView) itemLl.findViewById(R.id.tv_content);
                labelTv.setText(session.getDownloadingVodVersion().get(i).getLabel());
                contentTv.setText(session.getDownloadingVodVersion().get(i).getVersion());
                linearLayout.addView(itemLl, params);

                if (i + 1 < session.getDownloadingVodVersion().size()) {
                    LinearLayout itemLl2 = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_item, null);
                    TextView labelTv2 = (TextView) itemLl2.findViewById(R.id.tv_label);
                    TextView contentTv2 = (TextView) itemLl2.findViewById(R.id.tv_content);
                    labelTv2.setText(session.getDownloadingVodVersion().get(i + 1).getLabel());
                    contentTv2.setText(session.getDownloadingVodVersion().get(i + 1).getVersion());
                    linearLayout.addView(itemLl2);
                } else {
                    View view = new View(getContext());
                    linearLayout.addView(view, params2);
                }

                mDownloadingVodLl.addView(linearLayout);
            }
        }

        mPreparedPlaylistLl.removeAllViews();
        if (session.getNextPlayListVersion() != null && !session.getNextPlayListVersion().isEmpty()) {
            for (int i = 0; i < session.getNextPlayListVersion().size(); i += 2) {
                LinearLayout linearLayout = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_row, null);

                LinearLayout itemLl = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_item, null);
                TextView labelTv = (TextView) itemLl.findViewById(R.id.tv_label);
                TextView contentTv = (TextView) itemLl.findViewById(R.id.tv_content);
                labelTv.setText(session.getNextPlayListVersion().get(i).getLabel());
                contentTv.setText(session.getNextPlayListVersion().get(i).getVersion());
                linearLayout.addView(itemLl, params);

                if (i + 1 < session.getNextPlayListVersion().size()) {
                    LinearLayout itemLl2 = (LinearLayout) View.inflate(getContext(), R.layout.layout_box_info_item, null);
                    TextView labelTv2 = (TextView) itemLl2.findViewById(R.id.tv_label);
                    TextView contentTv2 = (TextView) itemLl2.findViewById(R.id.tv_content);
                    labelTv2.setText(session.getNextPlayListVersion().get(i + 1).getLabel());
                    contentTv2.setText(session.getNextPlayListVersion().get(i + 1).getVersion());
                    linearLayout.addView(itemLl2, params2);
                }

                mPreparedPlaylistLl.addView(linearLayout);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {

        return super.onKeyDown(keyCode, event);
    }
}
