package com.savor.ads.customview;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.savor.ads.R;
import com.savor.ads.utils.LogUtils;

public class IPEditText extends LinearLayout {

    private EditText mFirstIP;
    private EditText mSecondIP;
    private EditText mThirdIP;
    private EditText mFourthIP;

    private String mText1;
    private String mText2;
    private String mText3;
    private String mText4;

//	private SharedPreferences mPreferences;

    public IPEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        /**
         * 初始化控件
         */
        View view = LayoutInflater.from(context).inflate(R.layout.custom_my_edittext, this);
        mFirstIP = (EditText) findViewById(R.id.ip_first);
        mSecondIP = (EditText) findViewById(R.id.ip_second);
        mThirdIP = (EditText) findViewById(R.id.ip_third);
        mFourthIP = (EditText) findViewById(R.id.ip_fourth);
//		mPreferences = context.getSharedPreferences("config_IP", Context.MODE_PRIVATE);
        OperatingEditText(context);
    }

    /**
     * 获得EditText中的内容,当每个Edittext的字符达到三位时,自动跳转到下一个EditText,当用户点击.时,
     * 下一个EditText获得焦点
     */
    private void OperatingEditText(final Context context) {
        mFirstIP.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {

                /**
                 * 获得EditTe输入内容,做判断,如果大于255,提示不合法,当数字为合法的三位数下一个EditText获得焦点,
                 * 用户点击啊.时,下一个EditText获得焦点
                 */
                if (s != null && s.length() > 0) {
                    if (s.length() <= 2) {
                        mText1 = s.toString().trim();
                    } else {
                        mText1 = s.toString().trim();
                        LogUtils.i("第一个edittext为" + mText1);
                        try {
                            if (Integer.parseInt(mText1) > 255) {
                                Toast.makeText(context, "每一段IP不能大于255",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            mSecondIP.setFocusable(true);
                            mSecondIP.requestFocus();
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    mText1 = s.toString().trim();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mSecondIP.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                /**
                 * 获得EditTe输入内容,做判断,如果大于255,提示不合法,当数字为合法的三位数下一个EditText获得焦点,
                 * 用户点击啊.时,下一个EditText获得焦点
                 */
                if (s != null && s.length() > 0) {
                    if (s.length() <= 2) {
                        mText2 = s.toString().trim();
                    } else {
                        mText2 = s.toString().trim();
                        try {
                            if (Integer.parseInt(mText2) > 255) {
                                Toast.makeText(context, "每一段IP不能大于255",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            mThirdIP.setFocusable(true);
                            mThirdIP.requestFocus();
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    mText2 = s.toString().trim();
                    mFirstIP.setFocusable(true);
                    mFirstIP.requestFocus();
                    mFirstIP.setSelection(mFirstIP.getText().length());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        mThirdIP.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                /**
                 * 获得EditTe输入内容,做判断,如果大于255,提示不合法,当数字为合法的三位数下一个EditText获得焦点,
                 * 用户点击啊.时,下一个EditText获得焦点
                 */
                if (s != null && s.length() > 0) {
                    if (s.length() <= 2) {
                        mText3 = s.toString().trim();
                    } else {
                        mText3 = s.toString().trim();
                        try {
                            if (Integer.parseInt(mText3) > 255) {
                                Toast.makeText(context, "每一段IP不能大于255",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            mFourthIP.setFocusable(true);
                            mFourthIP.requestFocus();
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    mText3 = s.toString().trim();
                    mSecondIP.setFocusable(true);
                    mSecondIP.requestFocus();
                    mSecondIP.setSelection(mSecondIP.getText().length());
                }
            }


            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mFourthIP.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                /**
                 * 获得EditTe输入内容,做判断,如果大于255,提示不合法,当数字为合法的三位数下一个EditText获得焦点,
                 * 用户点击啊.时,下一个EditText获得焦点
                 */
                if (s != null && s.length() > 0) {
                    mText4 = s.toString().trim();
                    try {
                        if (Integer.parseInt(mText4) > 255) {
                            Toast.makeText(context, "每一段IP不能大于255", Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    mText4 = s.toString().trim();
                    mThirdIP.setFocusable(true);
                    mThirdIP.requestFocus();
                    mThirdIP.setSelection(mThirdIP.getText().length());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public String getText(Context context) {
        if (TextUtils.isEmpty(mText1) || TextUtils.isEmpty(mText2)
                || TextUtils.isEmpty(mText3) || TextUtils.isEmpty(mText4)) {
            Toast.makeText(context, "请输入合法的ip地址", Toast.LENGTH_SHORT).show();
            return "";
        }

        return mText1 + "." + mText2 + "." + mText3 + "." + mText4;
    }

    public void setText(String firstIP, String secondIP, String thirdIP, String fourthIP) {
        mFirstIP.setText(firstIP);
        mSecondIP.setText(secondIP);
        mThirdIP.setText(thirdIP);
        mFourthIP.setText(fourthIP);
    }
}
