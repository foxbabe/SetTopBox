package com.savor.ads.core;

/*
 * Copyright (C) 2010 mAPPn.Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.bean.AdMasterResult;
import com.savor.ads.bean.AdsMeiSSPResult;
import com.savor.ads.bean.PrizeInfo;
import com.savor.ads.bean.ServerInfo;
import com.savor.ads.bean.TvProgramGiecResponse;
import com.savor.ads.bean.TvProgramResponse;
import com.savor.ads.bean.UpgradeInfo;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.DesUtils;
import com.savor.ads.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;
import tianshu.ui.api.TsUiApiV20171122;

/**
 * API 响应结果解析工厂类，所有的API响应结果解析需要在此完成。
 *
 * @author andrew
 * @date 2011-4-22
 */
public class ApiResponseFactory {
    public final static String TAG = "ApiResponseFactory";
    // 当前服务器时间
    private static String webtime = "";
    public static Object getResponse(Context context, AppApi.Action action,
                                     Response response, String key, boolean isCache) {

        if (action == AppApi.Action.AD_BAIDU_ADS) {
            // 百度聚屏是特殊的类型，需要使用protobuff解析
            TsUiApiV20171122.TsApiResponse tsApiResponse = null;
            try {
                byte[] content = response.body().bytes();
                tsApiResponse = TsUiApiV20171122.TsApiResponse.parseFrom(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return tsApiResponse;
        }

        //转换器
        String requestMethod = "";
        Object result = null;
        boolean isDes = false;
        Session session = Session.get(context);
        String jsonResult = null;
        try {
            jsonResult = response.body().string();

        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } finally {
            response.close();
        }
        if (jsonResult == null) {
            return null;
        }

        String header = response.header("des");
        key = response.header("X-SMALL-TYPE");
        if (header != null && Boolean.valueOf(header)) {
            isDes = true;
        }
        if (isDes) {
            jsonResult = DesUtils.decrypt(jsonResult);
        }

        LogUtils.i("jsonResult:" + jsonResult);
        JSONObject rSet;
        JSONObject info = null;
        JSONArray infoArray = null;
        String infoJson = "";
        ResponseErrorMessage error;
        try {
            rSet = new JSONObject(jsonResult);
            if (rSet.has("code")) {
                int code = rSet.getInt("code");
                if (AppApi.HTTP_RESPONSE_STATE_SUCCESS == code) {
                    try {
                        info = rSet.getJSONObject("result");
                        infoJson = info.toString();
                    } catch (JSONException ex) {
                        try {
                            infoArray = rSet.getJSONArray("result");
                            infoJson = infoArray.toString();
                        } catch (JSONException e) {
                            try {
                                infoJson = rSet.getString("result");
                            } catch (Exception e2) {
                                infoJson = rSet.toString();
                            }

                        }
                    }

                    /**缓存返回数据包*/
//					if(isCache){
//						String serverKey = response.getFirstHeader("key").getValue();
//						String webtimeKey=response.getFirstHeader("webtime").getValue();
//						HttpCacheManager.getInstance(context).saveCacheData(key, serverKey,webtimeKey, infoJson);
//					}
                } else {
                    try {
                        if (rSet.has("msg")) {
                            String msg = rSet.getString("msg");
                            error = new ResponseErrorMessage();
                            error.setCode(code);
                            error.setMessage(msg);
                            error.setJson(jsonResult);
                            return error;
                        }
//				    	infoJson=info.toString();
                    } catch (JSONException ex) {
                        try {
                            String msg = rSet.getString("msg");
                            error = new ResponseErrorMessage();
                            error.setCode(code);
                            error.setMessage(msg);
                            error.setJson(jsonResult);
                            return error;
                        } catch (JSONException e) {
                            try {
                                infoJson = rSet.getString("result");
                            } catch (Exception e2) {
                                LogUtils.d(e.toString());
                            }

                        }
                    }
                }
            }
            result = parseResponse(action, infoJson, rSet,key);
        } catch (Exception e) {
            LogUtils.d(requestMethod + " has other unknown Exception", e);
            e.printStackTrace();
        }

        return result;
    }

    public static Object parseResponse(AppApi.Action action, String info, JSONObject ret,String key) {
        Object result = null;
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
		LogUtils.i("info:-->" + info);
        if (info == null) {
            return result;
        }
        switch (action) {
            case SP_GET_UPGRADE_INFO_JSON:
                result = gson.fromJson(info, new TypeToken<UpgradeInfo>() {
                }.getType());
                if (result instanceof UpgradeInfo){
                    UpgradeInfo upgradeInfo = (UpgradeInfo)result;
                    if (ConstantValues.VIRTUAL.equals(key)){
                        upgradeInfo.setVirtual(true);
                    }else{
                        upgradeInfo.setVirtual(false);
                    }
                    result =upgradeInfo;
                }
                break;
            case SP_POST_UPLOAD_PROGRAM_JSON:
                result = info;
                break;
            case SP_POST_UPLOAD_PROGRAM_GIEC_JSON:
                result = info;
                break;
            case CP_GET_SP_IP_JSON:
                result = gson.fromJson(info, new TypeToken<ServerInfo>() {
                }.getType());
                break;
            case SP_GET_TV_MATCH_DATA_FROM_JSON:
                result = gson.fromJson(info, new TypeToken<TvProgramResponse>() {
                }.getType());
                break;
            case SP_GET_TV_MATCH_DATA_FROM_GIEC_JSON:
                result = gson.fromJson(info, new TypeToken<TvProgramGiecResponse>() {
                }.getType());
                break;
            case CP_GET_PRIZE_JSON:
                result = gson.fromJson(info, new TypeToken<PrizeInfo>() {
                }.getType());
                break;
            case PH_NOTIFY_STOP_JSON:
                result = info;
                break;
            case CP_GET_HEARTBEAT_PLAIN:
                result = info;
                break;
            case CP_GET_ADMASTER_CONFIG_JSON:
                result = gson.fromJson(info, new TypeToken<AdMasterResult>() {
                }.getType());
                break;
            case CP_POST_DEVICE_TOKEN_JSON:
                result = info;
                break;
            case SP_POST_NETSTAT_JSON:
                result = info;
                break;
            case CP_POST_PLAY_LIST_JSON:
                result = info;
                break;
            case CP_POST_DOWNLOAD_LIST_JSON:
                result = info;
                break;
            case CP_POST_SDCARD_STATE_JSON:
                result = info;
                break;
            case CP_MINIPROGRAM_FORSCREEN_JSON:
                try {
                    JSONObject jsonObject = new JSONObject(info);
                    result =jsonObject.getInt("is_sapp_forscreen");
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case AD_MEI_VIDEO_ADS_JSON:
            case AD_MEI_IMAGE_ADS_JSON:
                try{
                    List<AdsMeiSSPResult> adsMeiSSPResults = new ArrayList<>();
                    JSONArray jsonArray= ret.getJSONArray("ad");
                    for (int i=0;i<jsonArray.length();i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String admnative = jsonObject.getString("admnative");
                        AdsMeiSSPResult adsMeiSSPResult = gson.fromJson(admnative, new TypeToken<AdsMeiSSPResult>() {
                        }.getType());
                        adsMeiSSPResults.add(adsMeiSSPResult);
                    }
                    result = adsMeiSSPResults;
                    LogUtils.d("213");
//                    JSONArray impression = admnativeJSON.getJSONArray("impression");
//                    JSONObject image = admnativeJSON.getJSONObject("image");
//                    JSONObject video = admnativeJSON.getJSONObject("video");
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        return result;
    }

}