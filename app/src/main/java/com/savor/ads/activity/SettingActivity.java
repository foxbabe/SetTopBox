package com.savor.ads.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.savor.ads.R;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.core.AppApi;
import com.savor.ads.customview.IPEditText;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.ShellUtils;

public class SettingActivity extends BaseActivity {

    private ViewGroup mBaseLl;
    private ViewGroup mEditIpLl;
    private ViewGroup mUseVirtualVp;
    private RelativeLayout mServerIpRl;
    private Switch mUseVirtualSwitch;
    private ViewGroup mStandaloneVg;
    private Switch mStandaloneSwitch;
    private TextView mServerIpTv;
    private IPEditText mIPEditText;
    private Button mConfirmBtn;

    private String mServerIp;
    private boolean mIsEditIp;
    private AlertDialog mModifyIpDialog;
    private AlertDialog mUseVirtualDialog;
    private AlertDialog mNotUseVirtualDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mBaseLl = (ViewGroup) findViewById(R.id.ll_base);
        mEditIpLl = (ViewGroup) findViewById(R.id.ll_edit_ip);
        mUseVirtualVp = (ViewGroup) findViewById(R.id.rl_use_virtual);
        mServerIpRl = (RelativeLayout) findViewById(R.id.rl_server_ip);
        mUseVirtualSwitch = (Switch) findViewById(R.id.use_switch);
        mStandaloneVg = (ViewGroup) findViewById(R.id.rl_standalone);
        mStandaloneSwitch = (Switch) findViewById(R.id.standalone_switch);
        mServerIpTv = (TextView) findViewById(R.id.tv_server_ip);
        mIPEditText = (IPEditText) findViewById(R.id.et_ip);
        mConfirmBtn = (Button) findViewById(R.id.btn_ok);

        mUseVirtualSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mServerIpRl.setFocusable(false);
                } else {
                    mServerIpRl.setFocusable(true);
                }
            }
        });
        mStandaloneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSession.setStandalone(true);
                    mUseVirtualVp.setVisibility(View.GONE);
                    mServerIpRl.setVisibility(View.GONE);
                } else {
                    mSession.setStandalone(false);
                    mUseVirtualVp.setVisibility(View.VISIBLE);
                    mServerIpRl.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (mSession.isStandalone()) {
            mStandaloneSwitch.setChecked(true);
        } else {
            mStandaloneSwitch.setChecked(false);

            if (mSession.isUseVirtualSp()) {
                mServerIpTv.setText(AppApi.VIRTUAL_SP_HOST);
                mUseVirtualSwitch.setChecked(true);
                mServerIpRl.setFocusable(false);
            } else {
                mUseVirtualSwitch.setChecked(false);
                if (mSession.getServerInfo() != null) {
                    mServerIp = mSession.getServerInfo().getServerIp();
                }
                mServerIpTv.setText(mServerIp);
                mServerIpRl.setFocusable(true);
            }
        }
        mBaseLl.requestFocus();

        if (!TextUtils.isEmpty(mServerIp)) {
            String[] splits = mServerIp.split("\\.");
            if (splits.length == 4) {
                mIPEditText.setText(splits[0], splits[1], splits[2], splits[3]);
            }
        }
        mIPEditText.setListener();
    }

    @Override
    public void onBackPressed() {
        if (mIsEditIp) {
            mIsEditIp = false;
            mBaseLl.setVisibility(View.VISIBLE);
            mEditIpLl.setVisibility(View.GONE);
            mBaseLl.requestFocus();
        } else {
            super.onBackPressed();

            gotoAdsPlayer();
        }
    }

    private void gotoAdsPlayer() {
        if (!TextUtils.isEmpty(AppUtils.getMainMediaPath()) && GlobalValues.getInstance().PLAY_LIST != null && !GlobalValues.getInstance().PLAY_LIST.isEmpty()) {
            Intent intent = new Intent();
            intent.setClass(this, AdsPlayerActivity.class);
            startActivity(intent);
        }
//        finish();
    }

    private void gotoTvSearch() {
        if (AppUtils.isMstar()) {
            ActivitiesManager.getInstance().popSpecialActivity(TvPlayerActivity.class);
            Intent intent = new Intent();
            intent.setClass(this, TvPlayerActivity.class);
            intent.putExtra(TvPlayerActivity.EXTRA_IS_AUTO_SEARCHING, true);
            startActivity(intent);
        } else {
            ActivitiesManager.getInstance().popSpecialActivity(TvPlayerGiecActivity.class);
            Intent intent = new Intent();
            intent.setClass(this, TvPlayerGiecActivity.class);
            intent.putExtra(TvPlayerGiecActivity.EXTRA_IS_AUTO_SEARCHING, true);
            startActivity(intent);
        }
    }


    // 修改IP地址
    public void doModifyIp(View v) {
        final String serverIp = mIPEditText.getText();

        if (mModifyIpDialog == null) {
            mModifyIpDialog = new AlertDialog.Builder(SettingActivity.this)
                    .setTitle("提示")
                    .setMessage("修改服务器地址后需要重启系统，是否确定修改？")
                    .setNegativeButton("是", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!TextUtils.isEmpty(serverIp)) {
                                mSession.setServerInfo(new ServerInfo(serverIp.trim(), 3));
                            } else {
                                mSession.setServerInfo(null);
                            }

                            ShellUtils.reboot();
                        }
                    })
                    .setPositiveButton("否", null)
                    .create();
        }
        mModifyIpDialog.show();
    }

    /**
     * 小平台IP点击事件
     *
     * @param v
     */
    public void showIpDialog(View v) {
        mBaseLl.setVisibility(View.GONE);
        mEditIpLl.setVisibility(View.VISIBLE);
        mIPEditText.requestFocus();
        mIsEditIp = true;

    }

    /**
     * TV搜索频道点击事件
     *
     * @param v
     */
    public void goSearch(View v) {
        gotoTvSearch();
        finish();
    }

    /**
     *
     * @param v
     */
    public void switchUseVirtual(View v) {
        if (mSession.isUseVirtualSp()) {
            if (mNotUseVirtualDialog == null) {
                mNotUseVirtualDialog = new AlertDialog.Builder(SettingActivity.this)
                        .setTitle("提示")
                        .setMessage("确定使用真实小平台?")
                        .setNegativeButton("是", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                mSession.setUseVirtualSp(false);
                                mUseVirtualSwitch.setChecked(false);

                                mSession.setServerInfo(null);

                                ShellUtils.reboot();
                            }
                        })
                        .setPositiveButton("否", null)
                        .create();
            }
            mNotUseVirtualDialog.show();
        } else {
            if (mUseVirtualDialog == null) {
                mUseVirtualDialog = new AlertDialog.Builder(SettingActivity.this)
                        .setTitle("提示")
                        .setMessage("确定使用虚拟小平台:" + AppApi.VIRTUAL_SP_HOST + "?")
                        .setNegativeButton("是", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                mSession.setUseVirtualSp(true);
                                mUseVirtualSwitch.setChecked(true);

                                mSession.setServerInfo(new ServerInfo(AppApi.VIRTUAL_SP_HOST, 3));

                                ShellUtils.reboot();
                            }
                        })
                        .setPositiveButton("否", null)
                        .create();
            }
            mUseVirtualDialog.show();
        }
    }

    public void switchStandalone(View v) {
        mStandaloneSwitch.setChecked(!mStandaloneSwitch.isChecked());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (mIsEditIp) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    handled = true;
                    mIPEditText.requestFocus();
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    handled = true;
                    mConfirmBtn.requestFocus();
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (mConfirmBtn.isFocused()) {
                        handled = true;
                    }
                    break;
            }
        }
        return handled || super.onKeyDown(keyCode, event);
    }
}
