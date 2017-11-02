package com.savor.ads.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;

import com.savor.ads.R;
import com.savor.ads.adapter.SetListAdapter;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.customview.IPEditText;
import com.savor.ads.utils.ActivitiesManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.ShellUtils;

import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                toAd();
                break;
            case 225://验证码
                return true;

        }
        return super.onKeyDown(keyCode, event);
    }

    private void toAd() {
        if (!TextUtils.isEmpty(AppUtils.getSDCardPath()) && GlobalValues.PLAY_LIST != null && !GlobalValues.PLAY_LIST.isEmpty()) {
            Intent intent = new Intent();
            intent.setClass(this, AdsPlayerActivity.class);
            startActivity(intent);
        }
        finish();
    }

    private void toTvSou() {
        ActivitiesManager.getInstance().popSpecialActivity(TvPlayerActivity.class);
        Intent intent = new Intent();
        intent.setClass(this, TvPlayerActivity.class);
        intent.putExtra(TvPlayerActivity.EXTRA_IS_AUTO_SEARCHING, true);
        startActivity(intent);
    }


    // 修改IP地址
    public void ser_qued(View v) {
//        IPEditText url_text = (IPEditText) findViewById(R.id.serurl);
        final String epgurl = "";

        AlertDialog dialog = new AlertDialog.Builder(SettingActivity.this)
                .setTitle("提示")
                .setMessage("修改服务器地址后需要重启系统，是否确定修改？")
                .setNegativeButton("是", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(epgurl)) {
                            mSession.setServerInfo(new ServerInfo(epgurl.trim(), 3));
                        } else {
                            mSession.setServerInfo(null);
                        }

                        ShellUtils.reboot();
                    }
                })
                .setPositiveButton("否", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        dialog.show();
    }


    // TV搜索频道
    public void sou_qued(View v) {
        toTvSou();
        finish();
    }
}
