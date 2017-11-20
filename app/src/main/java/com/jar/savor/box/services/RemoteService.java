package com.jar.savor.box.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.jar.savor.box.interfaces.OnRemoteOperationListener;
import com.jar.savor.box.vo.BaseResponse;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.PptRequestVo;
import com.jar.savor.box.vo.PptResponseVo;
import com.jar.savor.box.vo.PptVideoRequestVo;
import com.jar.savor.box.vo.PptVideoResponseVo;
import com.jar.savor.box.vo.QueryPosBySessionIdResponseVo;
import com.jar.savor.box.vo.QueryStatusResponseVo;
import com.jar.savor.box.vo.ResponseT;
import com.jar.savor.box.vo.RotateResponseVo;
import com.jar.savor.box.vo.SeekResponseVo;
import com.jar.savor.box.vo.StopResponseVo;
import com.jar.savor.box.vo.VideoPrepareRequestVo;
import com.jar.savor.box.vo.VolumeResponseVo;
import com.savor.ads.bean.PptImage;
import com.savor.ads.bean.PptVideo;
import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.projection.ProjectionManager;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.FileUtils;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.StringUtils;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class RemoteService extends Service {
    private String TAG = "ControllService";
    private Server server = new Server(8080);
    private static OnRemoteOperationListener listener;
    private RemoteService.ServerThread mServerAsyncTask;

    public RemoteService() {
    }

    public void setOnRemoteOpreationListener(OnRemoteOperationListener listener1) {
        listener = listener1;
    }

    public void onCreate() {
        super.onCreate();
        LogUtils.d("-------------------> Service onCreate");
        this.mServerAsyncTask = new RemoteService.ServerThread();
        this.mServerAsyncTask.start();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("-------------------> Service onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        LogUtils.e("RemoteService" + "onDestroy");
        super.onDestroy();
        if (RemoteService.this.server != null) {
            try {
                RemoteService.this.server.stop();
            } catch (Exception var2) {
                var2.printStackTrace();
            }
        }
        if (this.mServerAsyncTask != null) {
            this.mServerAsyncTask.interrupt();
            this.mServerAsyncTask = null;
        }

    }

    public IBinder onBind(Intent intent) {
        return new OperationBinder();
    }

    private class ControllHandler extends AbstractHandler implements ApiRequestListener {

        private ControllHandler() {
            mLock = new Object();
        }

        private Object mLock;

        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            synchronized (mLock) {
                LogUtils.w("***********一次请求处理开始...***********");
                LogUtils.d("target = " + target);
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(200);
                baseRequest.setHandled(true);

                if (GlobalValues.IS_BOX_BUSY) {
                    BaseResponse baseResponse = new BaseResponse();
                    baseResponse.setInfo("机顶盒繁忙，请稍候再试");
                    baseResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    String resp = new Gson().toJson(baseResponse);
                    LogUtils.d("返回结果:" + resp);
                    response.getWriter().println(resp);
                } else {
                    String version = request.getHeader("version");
                    boolean isWebReq = false;   // 是否是h5来的请求
                    try {
                        String temp = request.getParameter("web");
                        if (!TextUtils.isEmpty(temp)) {
                            isWebReq = Boolean.parseBoolean(temp);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // 由于h5不支持改http header，故这里对isWebReq做特殊处理
                    if ("1.0".equals(version) || isWebReq) {
                        handleRequestV10(request, response, isWebReq);
                    } /*else {
                        handleRequestOld(request, response);
                    }*/
                }
                LogUtils.w("***********一次请求处理结束...***********");
            }
        }

//        private void handleRequestOld(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
//            if (request.getContentType().contains("application/json")) {
//
//                //region 普通Json请求
//                String reqJson = getBodyString(request);
//
//                LogUtils.d("ServerName = " + request.getServerName() + " 客户端请求的内容 = " + reqJson);
//                BaseRequestVo fromJson = (BaseRequestVo) (new Gson()).fromJson(reqJson, BaseRequestVo.class);
//                LogUtils.d(fromJson != null ? "客户端请求功能 = " + fromJson.getFunction() : "无法解析请求");
//                String resJson = "";
//                if ("prepare".equalsIgnoreCase(fromJson.getFunction())) {
//                    LogUtils.e("enter method listener.prepare");
//                    PrepareRequestVo prepareRequest = (PrepareRequestVo) (new Gson()).fromJson(reqJson, PrepareRequestVo.class);
//                    if (!TextUtils.isEmpty(prepareRequest.getDeviceId()) &&
//                            (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
//                                    prepareRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID))) {
//                        GlobalValues.CURRENT_PROJECT_DEVICE_ID = prepareRequest.getDeviceId();
//                        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = prepareRequest.getDeviceName();
//                        PrepareResponseVo object = RemoteService.listener.prepare(prepareRequest);
//                        if (object.getResult() != ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
//                            GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
//                            GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
//                        }
//                        resJson = new Gson().toJson(object);
//                    } else {
//                        PrepareResponseVo vo = new PrepareResponseVo();
//                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
//                        resJson = new Gson().toJson(vo);
//                    }
//                } else if ("play".equalsIgnoreCase(fromJson.getFunction())) {
//                    LogUtils.d("enter method listener.play");
//                    PlayRequestVo playRequst = (PlayRequestVo) (new Gson()).fromJson(reqJson, PlayRequestVo.class);
//                    if (!TextUtils.isEmpty(playRequst.getDeviceId()) && playRequst.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
//                        PlayResponseVo object = RemoteService.listener.play(playRequst.getRate());
//                        resJson = new Gson().toJson(object);
//                    }
//                } else if ("rotate".equalsIgnoreCase(fromJson.getFunction())) {
//                    LogUtils.d("enter method listener.rotate");
//                    RotateRequestVo rotateRequest = (RotateRequestVo) (new Gson()).fromJson(reqJson, RotateRequestVo.class);
//                    if (!TextUtils.isEmpty(rotateRequest.getDeviceId()) && rotateRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
//                        RotateResponseVo object = RemoteService.listener.rotate(rotateRequest.getRotatevalue());
//                        resJson = new Gson().toJson(object);
//                    }
//                } else if ("seek_to".equalsIgnoreCase(fromJson.getFunction())) {
//                    LogUtils.d("enter method listener.seek");
//                    SeekRequestVo seekRequest = (SeekRequestVo) (new Gson()).fromJson(reqJson, SeekRequestVo.class);
//                    if (!TextUtils.isEmpty(seekRequest.getDeviceId()) && seekRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
//                        SeekResponseVo object = RemoteService.listener.seek(seekRequest.getAbsolutepos());
//                        resJson = new Gson().toJson(object);
//                    }
//                } else if ("stop".equalsIgnoreCase(fromJson.getFunction())) {
//                    LogUtils.e("enter method listener.stop");
//                    StopRequestVo stopRequest = (StopRequestVo) (new Gson()).fromJson(reqJson, StopRequestVo.class);
//                    if (!TextUtils.isEmpty(stopRequest.getDeviceId()) && stopRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
//                        StopResponseVo object = RemoteService.listener.stop();
//                        resJson = new Gson().toJson(object);
//
//                        GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
//                        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
//                        GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
//                    }
//                } else if ("volume".equalsIgnoreCase(fromJson.getFunction())) {
//                    LogUtils.d("enter method listener.volume");
//                    VolumeRequestVo volumeRequest = (VolumeRequestVo) (new Gson()).fromJson(reqJson, VolumeRequestVo.class);
//                    if (!TextUtils.isEmpty(volumeRequest.getDeviceId()) && volumeRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
//                        VolumeResponseVo object = RemoteService.listener.volume(volumeRequest.getAction());
//                        resJson = new Gson().toJson(object);
//                    }
//                } else if ("query".equalsIgnoreCase(fromJson.getFunction())) {
//                    LogUtils.d("enter method listener.query");
//                    QueryRequestVo queryRequest = (QueryRequestVo) (new Gson()).fromJson(reqJson, QueryRequestVo.class);
//                    if (!TextUtils.isEmpty(queryRequest.getDeviceId()) && queryRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
//                        Object object = RemoteService.listener.query();
//                        resJson = new Gson().toJson(object);
//                    } else {
//                        QueryPosBySessionIdResponseVo vo = new QueryPosBySessionIdResponseVo();
//                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//                        resJson = new Gson().toJson(vo);
//                    }
//                } else {
//                    LogUtils.d(" not enter any method");
//                    BaseResponse baseResponse9 = new BaseResponse();
//                    baseResponse9.setInfo("错误的功能");
//                    baseResponse9.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
//                    resJson = new Gson().toJson(baseResponse9);
//                }
//
//                LogUtils.d("返回结果:" + resJson);
//                response.getWriter().println(resJson);
//                //endregion
//            } else if (request.getContentType().contains("multipart/form-data;")) {
//
//                // 图片流投屏处理
//                handleStreamImageProjection(request, response);
//            }
//        }

        @NonNull
        private String getBodyString(HttpServletRequest request) throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(request.getInputStream(), Charset.forName("UTF-8")));
            char[] chars = new char[1024];
            int length = 0;
            while ((length = bufferedReader.read(chars, 0, 1024)) != -1) {
                stringBuilder.append(chars, 0, length);
            }
            return stringBuilder.toString();
        }

        /**
         * v1.0接口处理请求的逻辑
         *
         * @param request
         * @param response
         * @throws IOException
         * @throws ServletException
         */
        private void handleRequestV10(HttpServletRequest request, HttpServletResponse response, boolean isWebReq) throws IOException, ServletException {
            String resJson = "";
            String path = request.getPathInfo();
            LogUtils.d("request:--" + request.toString());
            if (TextUtils.isEmpty(path)) {
                BaseResponse baseResponse = new BaseResponse();
                baseResponse.setInfo("错误的功能");
                baseResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                resJson = new Gson().toJson(baseResponse);
            } else {
                String deviceId = request.getParameter("deviceId");
                String deviceName = request.getParameter("deviceName");
                if (!TextUtils.isEmpty(deviceId)) {
                    resJson = distributeRequest(request, isWebReq, path, deviceId, deviceName);
                }
            }

            if (TextUtils.isEmpty(resJson)) {
                BaseResponse baseResponse = new BaseResponse();
                baseResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                baseResponse.setInfo("操作失败");
                resJson = new Gson().toJson(baseResponse);
            }

            if (isWebReq) {
                // h5请求的响应需要包裹，否则h5取不到json
                resJson = "h5turbine(" + resJson + ")";
            }
            LogUtils.d("返回结果:" + resJson);
            response.getWriter().println(resJson);
        }

        private String distributeRequest(HttpServletRequest request, boolean isWebReq, String action, String deviceId, String deviceName) throws IOException, ServletException {
            String resJson = "";
            // 标识是否强行投屏
            // forceProject等于0表示不是强制抢投，等于1表示强制投，等于-1表示是老版移动端调用
            int forceProject = -1;
            switch (action) {
                case "/vod":
                    resJson = handleVodRequest(request, isWebReq, deviceId, deviceName, forceProject);
                    break;
                case "/video":
                    resJson = handleVideoRequest(request, isWebReq, deviceId, deviceName, forceProject);
                    break;
                case "/pic":
                    resJson = handlePicRequest(request, isWebReq, deviceId, deviceName, resJson, forceProject);
                    break;
                case "/stop":
                    resJson = handleStopRequest(request, deviceId, resJson);
                    break;
                case "/rotate":
                    resJson = handleRotateRequest(request, deviceId, resJson);
                    break;
                case "/resume":
                    resJson = handleResumeRequest(request, deviceId, resJson);
                    break;
                case "/pause":
                    resJson = handlePauseRequest(request, deviceId, resJson);
                    break;
                case "/seek":
                    resJson = handleSeekRequest(request, deviceId, resJson);
                    break;
                case "/volume":
                    resJson = handleVolumeRequest(request, deviceId, resJson);
                    break;
                case "/query":
                    resJson = handleQueryRequest(request, deviceId);
                    break;
                case "/queryStatus":
                    resJson = handleQueryStatusRequest();
                    break;
                case "/showCode":
                    resJson = handleShowCodeRequest(deviceId);
                    break;
                case "/verify":
                    resJson = handleVerifyCodeRequest(request, deviceId);
                    break;
                case "/egg":
                    resJson = handleEggRequest(request, isWebReq, deviceId, deviceName, forceProject);
                    break;
                case "/hitEgg":
                    resJson = handleHitEggRequest(request, deviceId, resJson);
                    break;
                case "/restaurant/ppt":
                    resJson = handleRstrPicPptRequest(request, isWebReq, deviceId, deviceName, forceProject);
                    break;
                case "/restaurant/picUpload":
                    resJson = handleRstrPicUploadRequest(request, isWebReq, deviceId, deviceName);
                    break;
                case "/restaurant/v-ppt":
                    resJson = handleRstrVideoPptRequest(request, isWebReq, deviceId, deviceName, forceProject);
                    break;
                case "/restaurant/vidUpload":
                    resJson = handleRstrVidUploadRequest(request, isWebReq, deviceId, deviceName);
                    break;
                case "/restaurant/stop":
                    resJson = handleRstrStopRequest(deviceId);
                    break;
                default:
                    LogUtils.d(" not enter any method");
                    BaseResponse baseResponse = new BaseResponse();
                    baseResponse.setInfo("错误的功能");
                    baseResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    resJson = new Gson().toJson(baseResponse);
                    break;
            }
            return resJson;
        }

        //region 处理投屏请求的方法集合

        /**
         * 处理点播请求
         *
         * @param request
         * @param isWebReq
         * @param deviceId
         * @param deviceName
         * @param forceProject
         * @return
         */
        private String handleVodRequest(HttpServletRequest request, boolean isWebReq, String deviceId, String deviceName, int forceProject) {
            String resJson;
            String type = request.getParameter("type");
            String mediaName = request.getParameter("name");
            int position = 0;
            try {
                position = Integer.parseInt(request.getParameter("position"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            try {
                forceProject = Integer.parseInt(request.getParameter("force"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {

                BaseResponse object = RemoteService.listener.showVod(mediaName, type, position, isWebReq, TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID));
                if (object.getResult() == ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                    GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                    AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);
                }
                resJson = new Gson().toJson(object);
            } else {
                if (isWebReq || GlobalValues.IS_LOTTERY) {
                    BaseResponse vo = new BaseResponse();
                    vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    if (GlobalValues.IS_LOTTERY) {
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                    } else {
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                    }
                    resJson = new Gson().toJson(vo);
                } else {
                    if (forceProject == 1) {
                        BaseResponse object = RemoteService.listener.showVod(mediaName, type, position, isWebReq, true);
                        if (object.getResult() == ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                            // 通知上一个投屏者已被抢投
                            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP)) {
                                AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                            }

                            GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                            GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                            GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                            AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);
                        }
                        resJson = new Gson().toJson(object);

                    } else if (forceProject == -1) {
                        BaseResponse vo = new BaseResponse();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                        if (GlobalValues.IS_LOTTERY) {
                            vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                        } else {
                            vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                        }
                        resJson = new Gson().toJson(vo);
                    } else {
                        BaseResponse vo = new BaseResponse();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_ANOTHER_PROJECT);
                        vo.setInfo(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);

                        resJson = new Gson().toJson(vo);
                    }
                }
            }
            return resJson;
        }

        /**
         * 处理视频投屏请求
         *
         * @param request
         * @param isWebReq
         * @param deviceId
         * @param deviceName
         * @param forceProject
         * @return
         * @throws IOException
         */
        private String handleVideoRequest(HttpServletRequest request, boolean isWebReq, String deviceId, String deviceName, int forceProject) throws IOException {
            String resJson;
            try {
                forceProject = Integer.parseInt(request.getParameter("force"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                String reqJson = getBodyString(request);
                VideoPrepareRequestVo req = (new Gson()).fromJson(reqJson, VideoPrepareRequestVo.class);
                if (!TextUtils.isEmpty(req.getMediaPath())) {

                    BaseResponse object = RemoteService.listener.showVideo(req.getMediaPath(), req.getPosition(), TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID));
                    if (object.getResult() == ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                        GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                        GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                        AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);
                    }
                    resJson = new Gson().toJson(object);
                } else {
                    BaseResponse vo = new BaseResponse();
                    vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    vo.setInfo("缺少视频路径");
                    resJson = new Gson().toJson(vo);
                }
            } else {
                if (isWebReq || GlobalValues.IS_LOTTERY) {
                    BaseResponse vo = new BaseResponse();
                    vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    if (GlobalValues.IS_LOTTERY) {
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                    } else {
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                    }
                    resJson = new Gson().toJson(vo);
                } else {
                    if (forceProject == 1) {
                        String reqJson = getBodyString(request);
                        VideoPrepareRequestVo req = (new Gson()).fromJson(reqJson, VideoPrepareRequestVo.class);
                        if (!TextUtils.isEmpty(req.getMediaPath())) {

                            BaseResponse object = RemoteService.listener.showVideo(req.getMediaPath(), req.getPosition(), true);
                            if (object.getResult() == ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                                // 通知上一个投屏者已被抢投
                                if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP)) {
                                    AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                                }

                                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                                GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                                AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);
                            }
                            resJson = new Gson().toJson(object);
                        } else {
                            BaseResponse vo = new BaseResponse();
                            vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                            vo.setInfo("缺少视频路径");
                            resJson = new Gson().toJson(vo);
                        }
                    } else if (forceProject == -1) {
                        BaseResponse vo = new BaseResponse();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                        if (GlobalValues.IS_LOTTERY) {
                            vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                        } else {
                            vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                        }
                        resJson = new Gson().toJson(vo);
                    } else {
                        BaseResponse vo = new BaseResponse();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_ANOTHER_PROJECT);
                        vo.setInfo(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);

                        resJson = new Gson().toJson(vo);
                    }
                }
            }
            return resJson;
        }

        /**
         * 处理图片投屏请求
         *
         * @param request
         * @param isWebReq
         * @param deviceId
         * @param deviceName
         * @param resJson
         * @param forceProject
         * @return
         * @throws IOException
         * @throws ServletException
         */
        private String handlePicRequest(HttpServletRequest request, boolean isWebReq, String deviceId, String deviceName, String resJson, int forceProject) throws IOException, ServletException {
            String isThumbnail = request.getParameter("isThumbnail");
            String imageId = request.getParameter("imageId");
            String seriesId = request.getParameter("seriesId");
            int imageType = 1;
            try {
                imageType = Integer.parseInt(request.getParameter("imageType"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            int rotation = 0;
            try {
                rotation = Integer.parseInt(request.getParameter("rotation"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            try {
                forceProject = Integer.parseInt(request.getParameter("force"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (request.getContentType().contains("multipart/form-data;")) {

                // 图片流投屏处理
                resJson = handleStreamImageProjection(request, imageType, deviceId, deviceName,
                        isThumbnail, imageId, rotation, seriesId, forceProject, isWebReq);
            }
            return resJson;
        }

        /**
         * 处理退出投屏请求
         *
         * @param request
         * @param deviceId
         * @param resJson
         * @return
         */
        private String handleStopRequest(HttpServletRequest request, String deviceId, String resJson) {
            LogUtils.e("enter method listener.stop");
            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                String projectId = request.getParameter("projectId");
                StopResponseVo object = RemoteService.listener.stop(projectId);
                resJson = new Gson().toJson(object);

                GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
            }
            return resJson;
        }

        /**
         * 处理图片旋转请求
         *
         * @param request
         * @param deviceId
         * @param resJson
         * @return
         */
        private String handleRotateRequest(HttpServletRequest request, String deviceId, String resJson) {
            LogUtils.d("enter method listener.rotate");
            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                String projectId = request.getParameter("projectId");
                RotateResponseVo object = RemoteService.listener.rotate(90, projectId);
                resJson = new Gson().toJson(object);
            }
            return resJson;
        }

        /**
         * 处理视频恢复播放请求
         *
         * @param request
         * @param deviceId
         * @param resJson
         * @return
         */
        private String handleResumeRequest(HttpServletRequest request, String deviceId, String resJson) {
            LogUtils.d("enter method listener.resume");
            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                String projectId = request.getParameter("projectId");
                PlayResponseVo object = RemoteService.listener.play(1, projectId);
                resJson = new Gson().toJson(object);
            }
            return resJson;
        }

        /**
         * 处理视频暂停播放请求
         *
         * @param request
         * @param deviceId
         * @param resJson
         * @return
         */
        private String handlePauseRequest(HttpServletRequest request, String deviceId, String resJson) {
            LogUtils.d("enter method listener.pause");
            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                String projectId = request.getParameter("projectId");
                PlayResponseVo object = RemoteService.listener.play(0, projectId);
                resJson = new Gson().toJson(object);
            }
            return resJson;
        }

        /**
         * 处理视频拖动进度请求
         *
         * @param request
         * @param deviceId
         * @param resJson
         * @return
         */
        private String handleSeekRequest(HttpServletRequest request, String deviceId, String resJson) {
            LogUtils.d("enter method listener.seek");
            int positionSeek = Integer.parseInt(request.getParameter("position"));
            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                String projectId = request.getParameter("projectId");
                SeekResponseVo object = RemoteService.listener.seek(positionSeek, projectId);
                resJson = new Gson().toJson(object);
            }
            return resJson;
        }

        /**
         * 处理视频改变音量请求
         *
         * @param request
         * @param deviceId
         * @param resJson
         * @return
         */
        private String handleVolumeRequest(HttpServletRequest request, String deviceId, String resJson) {
            LogUtils.d("enter method listener.volume");
            int volumeAction = Integer.parseInt(request.getParameter("action"));
            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                String projectId = request.getParameter("projectId");
                VolumeResponseVo object = RemoteService.listener.volume(volumeAction, projectId);
                resJson = new Gson().toJson(object);
            }
            return resJson;
        }

        /**
         * 处理查询视频播放进度请求
         *
         * @param request
         * @param deviceId
         * @return
         */
        private String handleQueryRequest(HttpServletRequest request, String deviceId) {
            String resJson;
            LogUtils.d("enter method listener.query");
            if (!TextUtils.isEmpty(deviceId) &&
                    (deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                            deviceId.equals(GlobalValues.LAST_PROJECT_DEVICE_ID))) {
                String projectId = request.getParameter("projectId");
                Object object = RemoteService.listener.query(projectId);
                resJson = new Gson().toJson(object);
            } else {
                QueryPosBySessionIdResponseVo vo = new QueryPosBySessionIdResponseVo();
                vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                resJson = new Gson().toJson(vo);
            }
            return resJson;
        }

        /**
         * 处理查询投屏进度请求
         *
         * @return
         */
        private String handleQueryStatusRequest() {
            String resJson;
            LogUtils.d("enter method listener.queryStatus");
            QueryStatusResponseVo statusVo = new QueryStatusResponseVo();
            statusVo.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
            statusVo.setInfo("查询成功");
            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                statusVo.setStatus(1);
                statusVo.setDeviceId(GlobalValues.CURRENT_PROJECT_DEVICE_ID);
                statusVo.setDeviceName(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);
            } else {
                statusVo.setStatus(0);
            }
            resJson = new Gson().toJson(statusVo);
            return resJson;
        }

        /**
         * 处理显示数字码请求
         *
         * @param deviceId
         * @return
         */
        private String handleShowCodeRequest(String deviceId) {
            String resJson;
            LogUtils.d("enter method listener.showCode");
            if (!TextUtils.isEmpty(deviceId)) {
                RemoteService.listener.showCode();
                ResponseT vo = new ResponseT();

                vo.setCode(10000);
                resJson = new Gson().toJson(vo);
            } else {
                ResponseT vo = new ResponseT();
                vo.setCode(10001);
                resJson = new Gson().toJson(vo);
            }
            return resJson;
        }

        /**
         * 处理验码请求
         *
         * @param request
         * @param deviceId
         * @return
         */
        private String handleVerifyCodeRequest(HttpServletRequest request, String deviceId) {
            String resJson;
            LogUtils.d("enter method listener.verify");
            if (!TextUtils.isEmpty(deviceId)) {
                String code = request.getParameter("code");
                ResponseT vo = RemoteService.listener.verify(code);
                resJson = new Gson().toJson(vo);
            } else {
                ResponseT vo = new ResponseT();
                vo.setCode(10001);
                resJson = new Gson().toJson(vo);
            }
            return resJson;
        }

        /**
         * 处理显示砸蛋游戏请求
         *
         * @param request
         * @param isWebReq
         * @param deviceId
         * @param deviceName
         * @param forceProject
         * @return
         */
        private String handleEggRequest(HttpServletRequest request, boolean isWebReq, String deviceId, String deviceName, int forceProject) {
            String resJson;
            LogUtils.e("enter method listener.egg");
            try {
                forceProject = Integer.parseInt(request.getParameter("force"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {

                String date = request.getParameter("date");
                int hunger = 0;
                try {
                    hunger = Integer.parseInt(request.getParameter("hunger"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                BaseResponse object = RemoteService.listener.showEgg(date, hunger);
                if (object.getResult() == ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                    GlobalValues.IS_LOTTERY = true;
                    GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                    AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);
                }
                resJson = new Gson().toJson(object);
            } else {
                if (isWebReq || GlobalValues.IS_LOTTERY) {
                    BaseResponse vo = new BaseResponse();
                    vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    if (GlobalValues.IS_LOTTERY) {
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                    } else {
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                    }
                    resJson = new Gson().toJson(vo);
                } else {
                    if (forceProject == 1) {
                        String date = request.getParameter("date");
                        int hunger = 0;
                        try {
                            hunger = Integer.parseInt(request.getParameter("hunger"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        BaseResponse object = RemoteService.listener.showEgg(date, hunger);
                        if (object.getResult() == ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                            // 通知上一个投屏者已被抢投
                            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP)) {
                                AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                            }

                            GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                            GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                            GlobalValues.IS_LOTTERY = true;
                            GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                            AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);
                        }
                        resJson = new Gson().toJson(object);
                    } else if (forceProject == -1) {
                        BaseResponse vo = new BaseResponse();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                        if (GlobalValues.IS_LOTTERY) {
                            vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                        } else {
                            vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                        }
                        resJson = new Gson().toJson(vo);
                    } else {
                        BaseResponse vo = new BaseResponse();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_ANOTHER_PROJECT);
                        vo.setInfo(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);

                        resJson = new Gson().toJson(vo);
                    }
                }
            }
            return resJson;
        }

        /**
         * 处理砸蛋动作请求
         *
         * @param request
         * @param deviceId
         * @param resJson
         * @return
         */
        private String handleHitEggRequest(HttpServletRequest request, String deviceId, String resJson) {
            LogUtils.e("enter method listener.hitEgg");
            if (!TextUtils.isEmpty(deviceId) && (deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    deviceId.equals(GlobalValues.LAST_PROJECT_DEVICE_ID))) {
                String projectId = request.getParameter("projectId");
                BaseResponse object = RemoteService.listener.hitEgg(projectId);
                resJson = new Gson().toJson(object);

                GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
            }
            return resJson;
        }

        /**
         * 处理餐厅端投图片幻灯片请求
         *
         * @param request
         * @param isWebReq
         * @param deviceId
         * @param deviceName
         * @param forceProject
         * @return
         * @throws IOException
         */
        private String handleRstrPicPptRequest(HttpServletRequest request, boolean isWebReq, String deviceId, String deviceName, int forceProject) throws IOException {
            LogUtils.e("enter method listener.restaurant/ppt");
            String reqJson = getBodyString(request);
            PptRequestVo req = (new Gson()).fromJson(reqJson, PptRequestVo.class);
            String path = AppUtils.getFilePath(RemoteService.this, AppUtils.StorageFile.ppt) + deviceId + File.separator;
            File deviceIdDir = new File(path);
            PptResponseVo pptResponse = new PptResponseVo();
            pptResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
            pptResponse.setImages(req.getImages());

            boolean isAllExist = true;
            if (!deviceIdDir.exists()) {
                deviceIdDir.mkdirs();
            }

            if (pptResponse.getImages() != null) {
                for (PptImage pptImage :
                        pptResponse.getImages()) {
                    File imgFile = new File(path + pptImage.getName());
                    if (imgFile.exists()) {
                        pptImage.setExist(1);
                    } else {
                        pptImage.setExist(0);
                        isAllExist = false;
                    }
                }
            }

            // 将配置信息存文件
            FileUtils.write(path + AppUtils.getMD5(req.getName()) + ".cfg", new Gson().toJson(req));

            if (isAllExist) {

                try {
                    forceProject = Integer.parseInt(request.getParameter("force"));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                        deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                    boolean isNewDevice = TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID);

                    // 通知上一个投屏者已被抢投
                    if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP) &&
                            !GlobalValues.CURRENT_PROJECT_DEVICE_IP.equals(deviceId)) {
                        AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                    }

                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                    GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                    AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                    RemoteService.listener.showPpt(deviceId, req, isNewDevice);
                } else {
                    if (isWebReq || GlobalValues.IS_LOTTERY) {
                        pptResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                        if (GlobalValues.IS_LOTTERY) {
                            pptResponse.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                        } else {
                            pptResponse.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                        }
                    } else {
                        if (forceProject == 1) {

                            // 通知上一个投屏者已被抢投
                            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP) &&
                                    !GlobalValues.CURRENT_PROJECT_DEVICE_IP.equals(deviceId)) {
                                AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                            }

                            GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                            GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                            GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                            AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                            RemoteService.listener.showPpt(deviceId, req, true);
                        } else if (forceProject == -1) {
                            pptResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                            if (GlobalValues.IS_LOTTERY) {
                                pptResponse.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                            } else {
                                pptResponse.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                            }
                        } else {
                            pptResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_ANOTHER_PROJECT);
                            pptResponse.setInfo(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);
                        }
                    }
                }
            }

            return new Gson().toJson(pptResponse);
        }

        /**
         * 处理餐厅端图片幻灯片上传图片请求
         *
         * @param request
         * @param isWebReq
         * @param deviceId
         * @param deviceName
         * @return
         * @throws IOException
         * @throws ServletException
         */
        private String handleRstrPicUploadRequest(HttpServletRequest request, boolean isWebReq, String deviceId, String deviceName) throws IOException, ServletException {
            LogUtils.e("enter method listener.restaurant/picUpload");
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement((String) null);
            request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfigElement);
            BaseResponse object = null;
            if (request.getParts() != null) {
                Bitmap bitmap = null;
                String fileName = null, pptName = null;
                for (Part part : request.getParts()) {
                    switch (part.getName()) {
                        case "fileUpload":
                            bitmap = BitmapFactory.decodeStream(part.getInputStream());
                            break;
                        case "fileName":
                            fileName = StringUtils.inputStreamToString(part.getInputStream());
                            break;
                        case "pptName":
                            pptName = StringUtils.inputStreamToString(part.getInputStream());
                            break;
                    }
                    part.delete();
                }

                if (!TextUtils.isEmpty(pptName) && !TextUtils.isEmpty(fileName) && bitmap != null) {
                    // 查找、读取幻灯片配置
                    String deviceIdDirPath = AppUtils.getFilePath(RemoteService.this, AppUtils.StorageFile.ppt) + deviceId + File.separator;
                    String configJson = FileUtils.read(deviceIdDirPath + AppUtils.getMD5(pptName) + ".cfg");
                    PptRequestVo reqVo = null;
                    if (!TextUtils.isEmpty(configJson)) {
                        reqVo = new Gson().fromJson(configJson, PptRequestVo.class);
                    }

                    if (reqVo != null) {
                        boolean isAllExist1 = true;
                        for (PptImage pptImage : reqVo.getImages()) {
                            if (fileName.equals(pptImage.getName())) {
                                pptImage.setExist(1);
                                FileUtils.write(deviceIdDirPath + AppUtils.getMD5(pptName) + ".cfg", new Gson().toJson(reqVo));
                            }

                            if (pptImage.getExist() != 1) {
                                isAllExist1 = false;
                            }
                        }

                        FileOutputStream outputStream = new FileOutputStream(deviceIdDirPath + fileName);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

                        object = new BaseResponse();
                        object.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

                        if (isAllExist1) {
                            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                                    deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                                boolean isNewDevice = TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID);

                                // 通知上一个投屏者已被抢投
                                if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP) &&
                                        !GlobalValues.CURRENT_PROJECT_DEVICE_IP.equals(deviceId)) {
                                    AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                                }

                                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                                GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                                AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                                RemoteService.listener.showPpt(deviceId, reqVo, isNewDevice);

                                object.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                            } else {
                                if (isWebReq || GlobalValues.IS_LOTTERY) {
                                    object.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                                    if (GlobalValues.IS_LOTTERY) {
                                        object.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                                    } else {
                                        object.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                                    }
                                } else {
                                    object.setResult(ConstantValues.SERVER_RESPONSE_CODE_ANOTHER_PROJECT);
                                    object.setInfo(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);
                                }
                            }
                        }
                    } else {
                        object = new BaseResponse();
                        object.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    }
                }
            }

            if (object == null) {
                // 请求格式错误
                object = new BaseResponse();
                object.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            }
            return new Gson().toJson(object);
        }

        /**
         * 处理餐厅端投视频幻灯片请求
         *
         * @param request
         * @param isWebReq
         * @param deviceId
         * @param deviceName
         * @param forceProject
         * @return
         * @throws IOException
         */
        private String handleRstrVideoPptRequest(HttpServletRequest request, boolean isWebReq, String deviceId, String deviceName, int forceProject) throws IOException {
            LogUtils.e("enter method listener.restaurant/v-ppt");
            PptVideoRequestVo pptVideoRequestVo = (new Gson()).fromJson(getBodyString(request), PptVideoRequestVo.class);
            String pathPptVideo = AppUtils.getFilePath(RemoteService.this, AppUtils.StorageFile.ppt) + deviceId + File.separator;
            File deviceIdDir = new File(pathPptVideo);
            PptVideoResponseVo pptVideoResponse = new PptVideoResponseVo();
            pptVideoResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
            pptVideoResponse.setVideos(pptVideoRequestVo.getVideos());

            boolean isAllVideoExist = true;
            if (!deviceIdDir.exists()) {
                deviceIdDir.mkdirs();
            }

            long neededSpace = 0;
            if (pptVideoResponse.getVideos() != null) {
                for (PptVideo pptVideo :
                        pptVideoResponse.getVideos()) {
                    File videoFile = new File(pathPptVideo + pptVideo.getName());
                    if (videoFile.exists()) {
                        pptVideo.setExist(1);
                    } else {
                        pptVideo.setExist(0);
                        isAllVideoExist = false;
                        neededSpace += pptVideo.getLength();
                    }
                }
            }

            if (AppUtils.getAvailableExtSize() - neededSpace < ConstantValues.EXTSD_LEAST_AVAILABLE_SPACE) {
                // 存储空间不足
                pptVideoResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                pptVideoResponse.setInfo("机顶盒存储空间不足，重启再试试吧");

                AppUtils.clearPptTmpFiles(RemoteService.this);
            } else {
                // 将配置信息存文件
                FileUtils.write(pathPptVideo + AppUtils.getMD5(pptVideoRequestVo.getName()) + ".v-cfg", new Gson().toJson(pptVideoRequestVo));

                if (isAllVideoExist) {

                    try {
                        forceProject = Integer.parseInt(request.getParameter("force"));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                            deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                        boolean isNewDevice = TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID);

                        // 通知上一个投屏者已被抢投
                        if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP) &&
                                !GlobalValues.CURRENT_PROJECT_DEVICE_IP.equals(deviceId)) {
                            AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                        }

                        GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                        GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                        AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                        RemoteService.listener.showVideoPpt(deviceId, pptVideoRequestVo, isNewDevice);
                    } else {
                        if (isWebReq || GlobalValues.IS_LOTTERY) {
                            pptVideoResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                            if (GlobalValues.IS_LOTTERY) {
                                pptVideoResponse.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                            } else {
                                pptVideoResponse.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                            }
                        } else {
                            if (forceProject == 1) {

                                // 通知上一个投屏者已被抢投
                                if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP) &&
                                        !GlobalValues.CURRENT_PROJECT_DEVICE_IP.equals(deviceId)) {
                                    AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                                }

                                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                                GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                                AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                                RemoteService.listener.showVideoPpt(deviceId, pptVideoRequestVo, true);
                            } else if (forceProject == -1) {
                                pptVideoResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                                if (GlobalValues.IS_LOTTERY) {
                                    pptVideoResponse.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                                } else {
                                    pptVideoResponse.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                                }
                            } else {
                                pptVideoResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_ANOTHER_PROJECT);
                                pptVideoResponse.setInfo(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);
                            }
                        }
                    }
                }
            }

            return new Gson().toJson(pptVideoResponse);
        }

        /**
         * 处理餐厅端视频幻灯片上传视频请求
         *
         * @param request
         * @param isWebReq
         * @param deviceId
         * @param deviceName
         * @return
         * @throws IOException
         * @throws ServletException
         */
        private String handleRstrVidUploadRequest(HttpServletRequest request, boolean isWebReq, String deviceId, String deviceName) throws IOException, ServletException {
            LogUtils.e("enter method listener.restaurant/vidUpload");
            MultipartConfigElement multipartConfigElement1 = new MultipartConfigElement((String) null);
            request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfigElement1);
            BaseResponse object = null;
            if (request.getParts() != null) {
                String fileName = null, pptName = null;
                String deviceIdDirPath = AppUtils.getFilePath(RemoteService.this, AppUtils.StorageFile.ppt) + deviceId + File.separator;

                Part partName = request.getPart("fileName");
                fileName = StringUtils.inputStreamToString(partName.getInputStream());
                partName.delete();

                Part partRange = request.getPart("range");
                String range = StringUtils.inputStreamToString(partRange.getInputStream());
                partRange.delete();

                long start = 0;
                boolean isFileEnd = false;
                if (!TextUtils.isEmpty(range) && range.contains("-")) {
                    if (range.endsWith("-")) {
                        isFileEnd = true;
                    }

                    String[] temp = range.split("-");
                    start = Long.parseLong(temp[0]);
//                    long end = Long.parseLong(temp[1]);
                }

                File videoFile = new File(deviceIdDirPath + fileName);

                for (Part part : request.getParts()) {
                    switch (part.getName()) {
                        case "fileUpload":
                            // 存文件
                            RandomAccessFile raf = new RandomAccessFile(videoFile, "rw");
                            raf.seek(start);
                            byte[] byteBuffer = new byte[1024];
                            int len = 0;
                            while ((len = part.getInputStream().read(byteBuffer)) > 0) {
                                raf.write(byteBuffer, 0, len);
                            }
                            raf.close();
                            part.delete();
                            break;
//                        case "fileName":
//                            fileName = StringUtils.inputStreamToString(part.getInputStream());
//                            part.delete();
//                            break;
                        case "pptName":
                            pptName = StringUtils.inputStreamToString(part.getInputStream());
                            part.delete();
                            break;
                        default:
                            part.delete();
                    }
                }

                if (!TextUtils.isEmpty(pptName) && !TextUtils.isEmpty(fileName) && isFileEnd) {
                    // 查找、读取幻灯片配置

                    String configJson = FileUtils.read(deviceIdDirPath + AppUtils.getMD5(pptName) + ".v-cfg");
                    PptVideoRequestVo reqVo = null;
                    if (!TextUtils.isEmpty(configJson)) {
                        reqVo = new Gson().fromJson(configJson, PptVideoRequestVo.class);
                    }

                    if (reqVo != null) {
                        boolean isAllExist = true;
                        for (PptVideo pptVideo : reqVo.getVideos()) {
                            if (fileName.equals(pptVideo.getName())) {
                                pptVideo.setExist(1);
                                FileUtils.write(deviceIdDirPath + AppUtils.getMD5(pptName) + ".v-cfg", new Gson().toJson(reqVo));
                            }

                            if (pptVideo.getExist() != 1) {
                                isAllExist = false;
                            }
                        }

                        object = new BaseResponse();
                        object.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

                        if (isAllExist) {
                            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                                    deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                                boolean isNewDevice = TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID);

                                // 通知上一个投屏者已被抢投
                                if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP) &&
                                        !GlobalValues.CURRENT_PROJECT_DEVICE_IP.equals(deviceId)) {
                                    AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                                }

                                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                                GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                                AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                                RemoteService.listener.showVideoPpt(deviceId, reqVo, isNewDevice);

                                object.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                            } else {
                                if (isWebReq || GlobalValues.IS_LOTTERY) {
                                    object.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                                    if (GlobalValues.IS_LOTTERY) {
                                        object.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                                    } else {
                                        object.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                                    }
                                } else {
                                    object.setResult(ConstantValues.SERVER_RESPONSE_CODE_ANOTHER_PROJECT);
                                    object.setInfo(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);
                                }
                            }
                        }
                    } else {
                        object = new BaseResponse();
                        object.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    }
                }
            }

            if (object == null) {
                // 请求格式错误
                object = new BaseResponse();
                object.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            }
            return new Gson().toJson(object);
        }

        /***
         * 处理餐厅端退出投屏请求
         * @param deviceId
         * @return
         */
        private String handleRstrStopRequest(String deviceId) {
            LogUtils.e("enter method listener.restaurant/stop");
            BaseResponse stopResponse = new BaseResponse();
            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                RemoteService.listener.rstrStop();
                stopResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);

                GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
            } else {
                stopResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
            }
            return new Gson().toJson(stopResponse);
        }

        private String handleStreamImageProjection(HttpServletRequest request, int imageType,
                                                   String deviceId, String deviceName, String isThumbnail,
                                                   String imageId, int rotation, String seriesId, int forceProject,
                                                   boolean isWebReq) throws IOException, ServletException {
            String respJson = "";
            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {

                BaseResponse object = null;

                boolean showImage = false;
                if ("1".equals(isThumbnail)) {
                    // 缩略图
                    GlobalValues.CURRENT_PROJECT_IMAGE_ID = imageId;
                    showImage = true;
                } else {
                    // 大图
                    if (!TextUtils.isEmpty(imageId) &&
                            imageId.equals(GlobalValues.CURRENT_PROJECT_IMAGE_ID)) {
                        showImage = true;
                    }
                }

                if (showImage) {
                    Bitmap bitmap = null;

                    MultipartConfigElement multipartConfigElement = new MultipartConfigElement((String) null);
                    request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfigElement);
                    if (request.getParts() != null) {
                        for (Part part : request.getParts()) {
                            switch (part.getName()) {
                                case "fileUpload":
                                    bitmap = BitmapFactory.decodeStream(part.getInputStream());
                                    part.delete();
                                    break;
                                default:
                                    part.delete();
                                    break;
                            }
                        }

//                        FileOutputStream outputStream = new FileOutputStream(AppUtils.getSDCardPath() + System.currentTimeMillis() + ".jpg");
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        // 显示图片
                        GlobalValues.CURRENT_PROJECT_BITMAP = bitmap;
                        object = RemoteService.listener.showImage(imageType, rotation, "1".equals(isThumbnail), seriesId, TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID));
                    } else {
                        // 请求格式错误
                        object = new BaseResponse();
                        object.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    }
                } else {
                    // 图片被忽略
                    object = new BaseResponse();
                    object.setResult(ConstantValues.SERVER_RESPONSE_CODE_IMAGE_ID_CHECK_FAILED);
                }
                if (object.getResult() == ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                    GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                    AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);
                }

                respJson = new Gson().toJson(object);
            } else {
                if (isWebReq || GlobalValues.IS_LOTTERY) {
                    BaseResponse vo = new BaseResponse();
                    vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    if (GlobalValues.IS_LOTTERY) {
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                    } else {
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                    }
                    respJson = new Gson().toJson(vo);
                } else {
                    if (forceProject == 1) {
                        BaseResponse object = null;

                        boolean showImage = false;
                        if ("1".equals(isThumbnail)) {
                            // 缩略图
                            GlobalValues.CURRENT_PROJECT_IMAGE_ID = imageId;
                            showImage = true;
                        } else {
                            // 大图
                            if (!TextUtils.isEmpty(imageId) &&
                                    imageId.equals(GlobalValues.CURRENT_PROJECT_IMAGE_ID)) {
                                showImage = true;
                            }
                        }

                        if (showImage) {
                            Bitmap bitmap = null;

                            MultipartConfigElement multipartConfigElement = new MultipartConfigElement((String) null);
                            request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfigElement);
                            if (request.getParts() != null) {
                                for (Part part : request.getParts()) {
                                    switch (part.getName()) {
                                        case "fileUpload":
                                            bitmap = BitmapFactory.decodeStream(part.getInputStream());
                                            part.delete();
                                            break;
                                        default:
                                            part.delete();
                                            break;
                                    }
                                }

//                        FileOutputStream outputStream = new FileOutputStream(AppUtils.getSDCardPath() + System.currentTimeMillis() + ".jpg");
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                                // 显示图片
                                GlobalValues.CURRENT_PROJECT_BITMAP = bitmap;
                                object = RemoteService.listener.showImage(imageType, rotation, "1".equals(isThumbnail), seriesId, true);
                            } else {
                                // 请求格式错误
                                object = new BaseResponse();
                                object.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                            }
                        } else {
                            // 图片被忽略
                            object = new BaseResponse();
                            object.setResult(ConstantValues.SERVER_RESPONSE_CODE_IMAGE_ID_CHECK_FAILED);
                        }
                        if (object.getResult() == ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                            // 通知上一个投屏者已被抢投
                            if (!TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_IP)) {
                                AppApi.notifyStop(RemoteService.this, this, 1, deviceName);
                            }

                            GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                            GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                            GlobalValues.CURRENT_PROJECT_DEVICE_IP = request.getRemoteHost();
                            AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);
                        }

                        respJson = new Gson().toJson(object);
                    } else if (forceProject == -1) {
                        BaseResponse vo = new BaseResponse();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                        if (GlobalValues.IS_LOTTERY) {
                            vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                        } else {
                            vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                        }
                        respJson = new Gson().toJson(vo);
                    } else {
                        BaseResponse vo = new BaseResponse();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_ANOTHER_PROJECT);
                        vo.setInfo(GlobalValues.CURRENT_PROJECT_DEVICE_NAME);

                        respJson = new Gson().toJson(vo);
                    }
                }
            }
            return respJson;
        }
        //endregion

        @Override
        public void onSuccess(AppApi.Action method, Object obj) {
            ProjectionManager.log("Notify stop success");
            LogUtils.d("Notify stop response: " + obj);
        }

        @Override
        public void onError(AppApi.Action method, Object obj) {
            ProjectionManager.log("Notify stop error");
        }

        @Override
        public void onNetworkFailed(AppApi.Action method) {
            ProjectionManager.log("Notify stop network failed");
        }
    }

    public class OperationBinder extends Binder {
        public OperationBinder() {
        }

        public RemoteService getController() {
            return RemoteService.this;
        }
    }

    private class ServerThread extends Thread {
        private ServerThread() {
        }

        public void run() {
            super.run();
            if (RemoteService.this.server != null) {
                try {
                    RemoteService.this.server.setHandler(new ControllHandler());
                    RemoteService.this.server.start();
                    RemoteService.this.server.join();
                } catch (Exception var2) {
                    var2.printStackTrace();
                }
            }

        }
    }
}
