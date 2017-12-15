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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.savor.ads.R;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.customview.IPEditText;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.KeyCodeConstant;
import com.savor.ads.utils.ShellUtils;

public class SettingActivity extends BaseActivity {

    private ViewGroup mBaseLl;
    private ViewGroup mEditIpLl;
    private ViewGroup mUseVirtualVp;
    private RelativeLayout mServerIpRl;
    private Switch mUseVirtualSwitch;
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
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (mSession.isUseVirtualSp()) {
            mServerIpTv.setText(ConstantValues.VIRTUAL_SP_HOST);
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
        if (!TextUtils.isEmpty(AppUtils.getSDCardPath()) && GlobalValues.PLAY_LIST != null && !GlobalValues.PLAY_LIST.isEmpty()) {
            Intent intent = new Intent();
            intent.setClass(this, AdsPlayerActivity.class);
            startActivity(intent);
        }
//        finish();
    }

    private void gotoTvSearch() {
        ActivitiesManager.getInstance().popSpecialActivity(TvPlayerActivity.class);
        Intent intent = new Intent();
        intent.setClass(this, TvPlayerActivity.class);
        intent.putExtra(TvPlayerActivity.EXTRA_IS_AUTO_SEARCHING, true);
        startActivity(intent);
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
     * @param v
     */
    public void switchUseVirtual(View v) {
        if (mSession.isUseVirtualSp()) {
            if (mUseVirtualDialog == null) {
                mUseVirtualDialog = new AlertDialog.Builder(SettingActivity.this)
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
            mUseVirtualDialog.show();
        } else {
            if (mUseVirtualDialog == null) {
                mUseVirtualDialog = new AlertDialog.Builder(SettingActivity.this)
                        .setTitle("提示")
                        .setMessage("确定使用虚拟小平台:" + ConstantValues.VIRTUAL_SP_HOST + "?")
                        .setNegativeButton("是", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                mSession.setUseVirtualSp(true);
                                mUseVirtualSwitch.setChecked(true);

                                mSession.setServerInfo(new ServerInfo(ConstantValues.VIRTUAL_SP_HOST, 3));

                                ShellUtils.reboot();
                            }
                        })
                        .setPositiveButton("否", null)
                        .create();
            }
            mUseVirtualDialog.show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (mIsEditIp) {
            switch (keyCode) {
                case KeyCodeConstant.KEY_CODE_UP:
                    handled = true;
                    mIPEditText.requestFocus();
                    break;
                case KeyCodeConstant.KEY_CODE_DOWN:
                    handled = true;
                    mConfirmBtn.requestFocus();
                    break;
                case KeyCodeConstant.KEY_CODE_LEFT:
                case KeyCodeConstant.KEY_CODE_RIGHT:
                    if (mConfirmBtn.isFocused()) {
                        handled = true;
                    }
                    break;
            }
        }
        return handled || super.onKeyDown(keyCode, event);
    }
}
