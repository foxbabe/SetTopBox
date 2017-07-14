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
    private TabHost tabHost;
    //    public Handler mHandler = null;
    private ListView setlList;
    private final int FLAG_LIST = -1;
    private final int FLAG_TAB1 = 0;
    private final int FLAG_TAB2 = 1;
    private final int FLAG_TAB3 = 2;
    private final int FLAG_TAB4 = 3;
    private IPEditText url_text;

    private int flag = FLAG_LIST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        tabHost = (TabHost) findViewById(R.id.tabhost);
        initView();
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("第1个标签")
                .setContent(R.id.tab1));
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("第二个标签")
                .setContent(R.id.tab2));
        tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("第三个标签")
                .setContent(R.id.tab3));
        tabHost.addTab(tabHost.newTabSpec("tab4").setIndicator("第4个标签")
                .setContent(R.id.tab4));


        tabHost.setCurrentTab(0);
        tabHost.getTabWidget().setVisibility(View.GONE);
    }

    private void initView() {

//        SharedPreferences preferences = getSharedPreferences("config_IP", Context.MODE_PRIVATE);

        url_text = (IPEditText) this.findViewById(R.id.serurl);

//        String firstIP = preferences.getString("firstIP", "192");
//        String secondIP = preferences.getString("secondIP", "168");
//        String thirdIP = preferences.getString("thirdIP", "199");
//        String fourthIP = preferences.getString("fourthIP", "150");

        if (mSession.getServerInfo() != null) {
            String ip = mSession.getServerInfo().getServerIp();
            String[] ipParts = ip.split("\\.");
            if (ipParts.length == 4) {
                url_text.setText(ipParts[0], ipParts[1], ipParts[2], ipParts[3]);
            }
        }

        List<String> list = null;
        list = new ArrayList<String>();
        list.add("视频制式");
        list.add("电视设置");
        list.add("自动搜索");
        list.add("服务器地址");
        setlList = (ListView) this.findViewById(R.id.setlistView1);
        SetListAdapter adapter = new SetListAdapter(this, list,
                R.layout.setlist_item,
                R.id.setlname);
        setlList.setAdapter(adapter);
        //	setlList.requestFocus();
        //	setlList.setSelected(true);


        setlList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {


            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {


                LogUtils.i("" + arg0.getSelectedItem().toString());
                updataTab(arg0.getSelectedItem().toString());

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });


    }

    private void updataTab(String name) {
        if (name.equals("视频制式")) {
            LogUtils.i("" + name);
            tabHost.setCurrentTab(0);
            setlList.requestFocus();

            return;
        } else if (name.equals("电视设置")) {
            LogUtils.i("" + name);
            tabHost.setCurrentTab(1);
            setlList.requestFocus();
            return;
        } else if (name.equals("自动搜索")) {
            LogUtils.i("" + name);
            tabHost.setCurrentTab(2);
            setlList.requestFocus();
            return;
        } else if (name.equals("服务器地址")) {
            LogUtils.i("" + name);
            tabHost.setCurrentTab(3);
            setlList.requestFocus();
            return;
        }
    }

    private void focusTab() {
        RelativeLayout tab = null;
        setFlag(tabHost.getCurrentTab());
        switch (flag) {
            case FLAG_TAB1:
                tab = (RelativeLayout) this.findViewById(R.id.tab1);
                break;
            case FLAG_TAB2:
                tab = (RelativeLayout) this.findViewById(R.id.tab2);
                break;
            case FLAG_TAB3:
                tab = (RelativeLayout) this.findViewById(R.id.tab3);
                break;
            case FLAG_TAB4:
                tab = (RelativeLayout) this.findViewById(R.id.tab4);
                break;
        }
        //RelativeLayout tab4 = (RelativeLayout) this.findViewById(R.id.tab4);
        //tabHost.getCurrentTabView().requestFocus();
        tab.requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if(mHandler.hasMessages(50)){
//            return false;
//        }
//        mHandler.sendEmptyMessageDelayed(50, 800);
        switch (flag) {
            case FLAG_LIST:
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        LogUtils.i("KEYCODE_DPAD_LEFT");
                        //changeChannel(-1);

                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:


                        focusTab();
                        //url_text.requestFocus();
                        LogUtils.i("KEYCODE_DPAD_RIGHT" + tabHost.getCurrentTab());
                        //tabHost.requestFocus();
                        //tabHost.getTabWidget().requestFocus();
                        //changeChannel(+1);
                        break;
                    case KeyEvent.KEYCODE_BACK:
                        toAd();
                        break;
                    case 225://验证码
                        return true;

                }
                break;
            case FLAG_TAB4:
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        //changeChannel(-1);
                        setlList.requestFocus();
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        setFlag(FLAG_LIST);
                        //changeChannel(+1);

                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        //	ser_button.requestFocus();
                        break;
                    case KeyEvent.KEYCODE_BACK:
                        toAd();
                        break;
                    case 225://验证码
                        return true;
                }
                break;

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

    private void setFlag(int flag) {
        this.flag = flag;
    }


    // 修改IP地址
    public void ser_qued(View v) {
        IPEditText url_text = (IPEditText) findViewById(R.id.serurl);
        final String epgurl = url_text.getText(this);

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
