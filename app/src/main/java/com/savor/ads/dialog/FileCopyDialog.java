package com.savor.ads.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.savor.ads.R;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.DensityUtil;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.KeyCode;
import com.savor.ads.utils.ShowMessage;

import java.io.File;

import static u.aly.cw.i;

/**
 * Created by zhanghq on 2016/12/12.
 */
public class FileCopyDialog extends Dialog {
    private TextView mTipsTv;

    private Context mContext;
    private Session mSession;

    private boolean mIsProcessing;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1 && msg.obj != null) {
                String text = msg.obj.toString();
                if (mTipsTv != null) {
                    mTipsTv.setText(mTipsTv.getText() + text + "\r\n");
                }
            }
            return true;
        }
    });

    public FileCopyDialog(Context context) {
        super(context, R.style.channel_searching_dialog_theme);

        mContext = context;
        mSession = Session.get(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_file_copy);
        setDialogAttributes();

        mTipsTv = (TextView) findViewById(R.id.tv_tips);
    }

    private void setDialogAttributes() {
        Window window = getWindow(); // 得到对话框
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.width = DensityUtil.dip2px(getContext(), 700);
        wl.height = DensityUtil.dip2px(getContext(), 500);
        wl.gravity = Gravity.CENTER;
        window.setAttributes(wl);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;
        if (keyCode == KeyCode.KEY_CODE_BACK) {
            if (mIsProcessing) {
                ShowMessage.showToast(mContext, "正在拷贝中，请稍候");
            } else {
                dismiss();
            }
            handled = true;
        }
        return handled || super.onKeyDown(keyCode, event);
    }

    @Override
    public void show() {
        super.show();

        mTipsTv.setText("");
        new Thread(new Runnable() {
            @Override
            public void run() {
                mIsProcessing = true;

                File mediaDir = new File(mSession.getUsbPath() + ConstantValues.USB_FILE_HOTEL_MEDIA_PATH);
                File multicastDir = new File(mSession.getUsbPath() + ConstantValues.USB_FILE_HOTEL_MULTICAST_PATH);
                if (mediaDir.isDirectory() && mediaDir.isDirectory()) {
                    File sdMediaFile = new File(AppUtils.getMainMediaPath() + "media/");
                    if (!sdMediaFile.exists()) {
                        sdMediaFile.mkdir();
                    }
                    File[] listFiles = mediaDir.listFiles();
                    for (int i = 0; i < listFiles.length; i++) {
                        File file = listFiles[i];
                        mHandler.sendMessage(mHandler.obtainMessage(1, "开始拷贝" + file.getPath() +
                                "(" + (i + 1) + "/" + listFiles.length + ")"));
                        boolean isSuccess = true;
                        File dstFile = new File(sdMediaFile, file.getName());
                        if (!dstFile.exists() || dstFile.length() != file.length()) {
                            try {
                                FileUtils.copyFile(file, dstFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                                isSuccess = false;
                            }
                        }
                        String resultMsg = "拷贝" + file.getPath();
                        if (isSuccess) {
                            resultMsg += "成功";
                        } else {
                            resultMsg += "失败";
                        }
                        mHandler.sendMessage(mHandler.obtainMessage(1, resultMsg));
                    }
                }
                if (multicastDir.isDirectory() && multicastDir.isDirectory()) {
                    File sdMulticastFile = new File(AppUtils.getMainMediaPath() + "multicast/");
                    if (!sdMulticastFile.exists()) {
                        sdMulticastFile.mkdir();
                    }
                    File[] listFiles = multicastDir.listFiles();
                    for (int i1 = 0; i1 < listFiles.length; i1++) {
                        File file = listFiles[i1];
                        mHandler.sendMessage(mHandler.obtainMessage(1, "开始拷贝" + file.getPath() +
                                "(" + (i1 + 1) + "/" + listFiles.length + ")"));
                        boolean isSuccess = true;
                        File dstFile = new File(sdMulticastFile, file.getName());
                        if (!dstFile.exists() || dstFile.length() != file.length()) {
                            try {
                                FileUtils.copyFile(file, dstFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                                isSuccess = false;
                            }
                        }
                        String resultMsg = "拷贝" + file.getPath();
                        if (isSuccess) {
                            resultMsg += "成功";
                        } else {
                            resultMsg += "失败";
                        }
                        mHandler.sendMessage(mHandler.obtainMessage(1, resultMsg));
                    }
                }

                mIsProcessing = false;
            }
        }).start();
    }
}
