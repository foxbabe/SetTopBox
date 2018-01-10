package com.savor.ads;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.PushCustomBean;
import com.savor.ads.bean.RTBPushItem;
import com.savor.ads.database.DBHelper;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.TechnicalLogReporter;
import com.umeng.message.UmengMessageService;
import com.umeng.message.entity.UMessage;

import org.android.agoo.common.AgooConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class UMessageIntentService extends UmengMessageService {

    @Override
    public void onMessage(Context context, Intent intent) {
        Log.d("UMessageIntentService", "onMessage");

        try {
            String message = intent.getStringExtra(AgooConstants.MESSAGE_BODY);
            UMessage msg = new UMessage(new JSONObject(message));
            LogUtils.d("message=" + message);      //消息体
            LogFileUtil.writeKeyLogInfo("UPush onMessage custom is " + msg.custom);

            if (!TextUtils.isEmpty(msg.custom)) {
                JSONObject jsonObject = new JSONObject(msg.custom);
                int type = jsonObject.getInt("type");
                if (1 == type) {
                    // RTB推送
                    ArrayList<RTBPushItem> rtbPushItems = new Gson().fromJson(jsonObject.getString("data"), new TypeToken<ArrayList<RTBPushItem>>() {
                    }.getType());
                    ArrayList<MediaLibBean> rtbMedias = new ArrayList<>();
                    if (rtbPushItems != null) {
                        for (RTBPushItem item : rtbPushItems) {
                            String selection = DBHelper.MediaDBInfo.FieldName.VID + "=? ";
                            String[] selectionArgs = new String[]{item.getId()};
                            List<MediaLibBean> list = DBHelper.get(this).findRtbadsMediaLibByWhere(selection, selectionArgs);
                            if (list != null) {
                                rtbMedias.add(list.get(0));
                            }
                        }
                    }
                    GlobalValues.RTB_PUSH_ADS = rtbMedias;
                    if (fillPlayList()) {
                        sendBroadcast(new Intent(ConstantValues.RTB_ADS_PUSH_ACTION));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean fillPlayList() {
        LogUtils.d("开始fillPlayList");
        ArrayList<MediaLibBean> playList = DBHelper.get(this).getOrderedPlayList();

        if (playList != null && !playList.isEmpty()) {
            int rtbIndex = 0;
            for (int i = 0; i < playList.size(); i++) {
                MediaLibBean bean = playList.get(i);

                // 特殊处理ads数据
                if (bean.getType().equals(ConstantValues.ADS)) {
                    String selection = DBHelper.MediaDBInfo.FieldName.LOCATION_ID
                            + "=? ";
                    String[] selectionArgs = new String[]{bean.getLocation_id()};
                    List<MediaLibBean> list = DBHelper.get(this).findAdsByWhere(selection, selectionArgs);
                    if (list != null && !list.isEmpty()) {
                        for (MediaLibBean item :
                                list) {
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date startDate = null;
                            Date endDate = null;
                            try {
                                startDate = format.parse(item.getStart_date());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            try {
                                endDate = format.parse(item.getEnd_date());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            Date now = new Date();
                            if (startDate != null && endDate != null &&
                                    now.after(startDate) && now.before(endDate)) {
                                bean.setVid(item.getVid());
                                bean.setDuration(item.getDuration());
                                bean.setMd5(item.getMd5());
                                bean.setName(item.getName());
                                bean.setMediaPath(item.getMediaPath());
                                break;
                            }
                        }
                    }
                }

                if (GlobalValues.RTB_PUSH_ADS != null && !GlobalValues.RTB_PUSH_ADS.isEmpty()) {
                    if (ConstantValues.RTB_ADS.equals(bean.getType())) {
                        MediaLibBean rtbItem = GlobalValues.RTB_PUSH_ADS.get(rtbIndex++);
                        bean.setName(rtbItem.getName());
                        bean.setMediaPath(rtbItem.getMediaPath());
                        bean.setAdmaster_sin(rtbItem.getAdmaster_sin());
                        bean.setChinese_name(rtbItem.getChinese_name());
                        bean.setDuration(rtbItem.getDuration());
                        bean.setVid(rtbItem.getVid());
                        bean.setMd5(rtbItem.getMd5());
                        bean.setPeriod(rtbItem.getPeriod());
                    }
                }
                File mediaFile = new File(bean.getMediaPath());
                boolean fileCheck = false;
                if (!TextUtils.isEmpty(bean.getMd5()) &&
                        !TextUtils.isEmpty(bean.getMediaPath()) &&
                        mediaFile.exists()) {
                    if (!bean.getMd5().equals(AppUtils.getEasyMd5(mediaFile))) {
                        fileCheck = true;

                        TechnicalLogReporter.md5Failed(this, bean.getVid());
                    }
                } else {
                    fileCheck = true;
                }

                if (fileCheck) {
                    LogUtils.e("媒体文件校验失败! vid:" + bean.getVid());
                    // 校验失败时将文件路径置空，下面会删除掉为空的项
                    bean.setMediaPath(null);
                    if (mediaFile.exists()) {
                        mediaFile.delete();
                    }

                    DBHelper.get(this).deleteDataByWhere(DBHelper.MediaDBInfo.TableName.NEWPLAYLIST,
                            DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                            new String[]{bean.getPeriod(), bean.getVid(), bean.getType()});
                    DBHelper.get(this).deleteDataByWhere(DBHelper.MediaDBInfo.TableName.PLAYLIST,
                            DBHelper.MediaDBInfo.FieldName.PERIOD + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.VID + "=? AND " +
                                    DBHelper.MediaDBInfo.FieldName.MEDIATYPE + "=?",
                            new String[]{bean.getPeriod(), bean.getVid(), bean.getType()});
                }
            }
        }

        if (playList != null && !playList.isEmpty()) {
            ArrayList<MediaLibBean> list = new ArrayList<>();
            for (MediaLibBean bean : playList) {
                if (!TextUtils.isEmpty(bean.getMediaPath())) {
                    list.add(bean);
                }
            }
            GlobalValues.PLAY_LIST = list;
            return true;
        } else {
            return false;
        }
    }
}