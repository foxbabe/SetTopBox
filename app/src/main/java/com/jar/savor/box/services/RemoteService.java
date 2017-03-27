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
import com.jar.savor.box.vo.BaseRequestVo;
import com.jar.savor.box.vo.BaseResponse;
import com.jar.savor.box.vo.PlayRequestVo;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.PrepareRequestVo;
import com.jar.savor.box.vo.PrepareResponseVo;
import com.jar.savor.box.vo.QueryPosBySessionIdResponseVo;
import com.jar.savor.box.vo.QueryRequestVo;
import com.jar.savor.box.vo.ResponseT;
import com.jar.savor.box.vo.RotateRequestVo;
import com.jar.savor.box.vo.RotateResponseVo;
import com.jar.savor.box.vo.SeekRequestVo;
import com.jar.savor.box.vo.SeekResponseVo;
import com.jar.savor.box.vo.StopRequestVo;
import com.jar.savor.box.vo.StopResponseVo;
import com.jar.savor.box.vo.VideoPrepareRequestVo;
import com.jar.savor.box.vo.VolumeRequestVo;
import com.jar.savor.box.vo.VolumeResponseVo;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogUtils;
import com.savor.ads.utils.StringUtils;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private class ControllHandler extends AbstractHandler {

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

                String version = request.getHeader("version");
                if ("1.0".equals(version)) {
                    handleRequestV10(request, response);
                } else {
                    handleRequestOld(request, response);
                }
                LogUtils.w("***********一次请求处理结束...***********");
            }
        }

        private void handleRequestOld(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            if (request.getContentType().contains("application/json")) {

                //region 普通Json请求
                String reqJson = getBodyString(request);

                LogUtils.d("ServerName = " + request.getServerName() + " 客户端请求的内容 = " + reqJson);
                BaseRequestVo fromJson = (BaseRequestVo) (new Gson()).fromJson(reqJson, BaseRequestVo.class);
                LogUtils.d(fromJson != null ? "客户端请求功能 = " + fromJson.getFunction() : "无法解析请求");
                String resJson = "";
                if ("prepare".equalsIgnoreCase(fromJson.getFunction())) {
                    LogUtils.e("enter method listener.prepare");
                    PrepareRequestVo prepareRequest = (PrepareRequestVo) (new Gson()).fromJson(reqJson, PrepareRequestVo.class);
                    if (!TextUtils.isEmpty(prepareRequest.getDeviceId()) &&
                            (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                                    prepareRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID))) {
                        GlobalValues.CURRENT_PROJECT_DEVICE_ID = prepareRequest.getDeviceId();
                        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = prepareRequest.getDeviceName();
                        PrepareResponseVo object = RemoteService.listener.prepare(prepareRequest);
                        if (object.getResult() != ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                            GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
                            GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
                        }
                        resJson = new Gson().toJson(object);
                    } else {
                        PrepareResponseVo vo = new PrepareResponseVo();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                        vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                        resJson = new Gson().toJson(vo);
                    }
                } else if ("play".equalsIgnoreCase(fromJson.getFunction())) {
                    LogUtils.d("enter method listener.play");
                    PlayRequestVo playRequst = (PlayRequestVo) (new Gson()).fromJson(reqJson, PlayRequestVo.class);
                    if (!TextUtils.isEmpty(playRequst.getDeviceId()) && playRequst.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                        PlayResponseVo object = RemoteService.listener.play(playRequst.getRate());
                        resJson = new Gson().toJson(object);
                    }
                } else if ("rotate".equalsIgnoreCase(fromJson.getFunction())) {
                    LogUtils.d("enter method listener.rotate");
                    RotateRequestVo rotateRequest = (RotateRequestVo) (new Gson()).fromJson(reqJson, RotateRequestVo.class);
                    if (!TextUtils.isEmpty(rotateRequest.getDeviceId()) && rotateRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                        RotateResponseVo object = RemoteService.listener.rotate(rotateRequest.getRotatevalue());
                        resJson = new Gson().toJson(object);
                    }
                } else if ("seek_to".equalsIgnoreCase(fromJson.getFunction())) {
                    LogUtils.d("enter method listener.seek");
                    SeekRequestVo seekRequest = (SeekRequestVo) (new Gson()).fromJson(reqJson, SeekRequestVo.class);
                    if (!TextUtils.isEmpty(seekRequest.getDeviceId()) && seekRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                        SeekResponseVo object = RemoteService.listener.seek(seekRequest.getAbsolutepos());
                        resJson = new Gson().toJson(object);
                    }
                } else if ("stop".equalsIgnoreCase(fromJson.getFunction())) {
                    LogUtils.e("enter method listener.stop");
                    StopRequestVo stopRequest = (StopRequestVo) (new Gson()).fromJson(reqJson, StopRequestVo.class);
                    if (!TextUtils.isEmpty(stopRequest.getDeviceId()) && stopRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                        StopResponseVo object = RemoteService.listener.stop();
                        resJson = new Gson().toJson(object);

                        GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
                        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
                        GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
                    }
                } else if ("volume".equalsIgnoreCase(fromJson.getFunction())) {
                    LogUtils.d("enter method listener.volume");
                    VolumeRequestVo volumeRequest = (VolumeRequestVo) (new Gson()).fromJson(reqJson, VolumeRequestVo.class);
                    if (!TextUtils.isEmpty(volumeRequest.getDeviceId()) && volumeRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                        VolumeResponseVo object = RemoteService.listener.volume(volumeRequest.getAction());
                        resJson = new Gson().toJson(object);
                    }
                } else if ("query".equalsIgnoreCase(fromJson.getFunction())) {
                    LogUtils.d("enter method listener.query");
                    QueryRequestVo queryRequest = (QueryRequestVo) (new Gson()).fromJson(reqJson, QueryRequestVo.class);
                    if (!TextUtils.isEmpty(queryRequest.getDeviceId()) && queryRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                        Object object = RemoteService.listener.query();
                        resJson = new Gson().toJson(object);
                    } else {
                        QueryPosBySessionIdResponseVo vo = new QueryPosBySessionIdResponseVo();
                        vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                        resJson = new Gson().toJson(vo);
                    }
                } else {
                    LogUtils.d(" not enter any method");
                    BaseResponse baseResponse9 = new BaseResponse();
                    baseResponse9.setInfo("错误的功能");
                    baseResponse9.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    resJson = new Gson().toJson(baseResponse9);
                }

                LogUtils.d("返回结果:" + resJson);
                response.getWriter().println(resJson);
                //endregion
            } else if (request.getContentType().contains("multipart/form-data;")) {

                // 图片流投屏处理
                handleStreamImageProjection(request, response);
            }
        }

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
         * @param request
         * @param response
         * @throws IOException
         * @throws ServletException
         */
        private void handleRequestV10(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            String resJson = "";
            String path = request.getPathInfo();
            LogUtils.d("request:--" + request.toString());
            if (TextUtils.isEmpty(path)) {
                BaseResponse baseResponse = new BaseResponse();
                baseResponse.setInfo("错误的功能");
                baseResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                resJson = new Gson().toJson(baseResponse);
            } else {
                String[] dirs = path.split("/");
                if (dirs.length <= 1) {
                    BaseResponse baseResponse = new BaseResponse();
                    baseResponse.setInfo("错误的功能");
                    baseResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    resJson = new Gson().toJson(baseResponse);
                } else {

                    String action = dirs[1];
                    String deviceId = request.getParameter("deviceId");
                    String deviceName = request.getParameter("deviceName");
                    switch (action) {
                        case "vod":
                            String type = request.getParameter("type");
                            String mediaName = request.getParameter("name");
                            int position = 0;
                            try {
                                position = Integer.parseInt(request.getParameter("position"));
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            if (!TextUtils.isEmpty(deviceId) &&
                                    (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                                            deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID))) {
                                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                                BaseResponse object = RemoteService.listener.showVod(mediaName, type, position);
                                if (object.getResult() != ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
                                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
                                }
                                resJson = new Gson().toJson(object);
                            } else {
                                PrepareResponseVo vo = new PrepareResponseVo();
                                vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                                vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                                resJson = new Gson().toJson(vo);
                            }
                            break;
                        case "video":
                            if (!TextUtils.isEmpty(deviceId) &&
                                    (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                                            deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID))) {
                                String reqJson = getBodyString(request);
                                VideoPrepareRequestVo req = (new Gson()).fromJson(reqJson, VideoPrepareRequestVo.class);
                                if (!TextUtils.isEmpty(req.getMediaPath())) {
                                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                                    BaseResponse object = RemoteService.listener.showVideo(req.getMediaPath(), req.getPosition());
                                    if (object.getResult() != ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                                        GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
                                        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
                                    }
                                    resJson = new Gson().toJson(object);
                                } else {
                                    PrepareResponseVo vo = new PrepareResponseVo();
                                    vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                                    vo.setInfo("缺少视频路径");
                                    resJson = new Gson().toJson(vo);
                                }
                            } else {
                                PrepareResponseVo vo = new PrepareResponseVo();
                                vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                                vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                                resJson = new Gson().toJson(vo);
                            }
                            break;
                        case "pic":
                            String isThumbnail = request.getParameter("isThumbnail");
                            String imageId = request.getParameter("imageId");
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
                            if (request.getContentType().contains("multipart/form-data;")) {

                                // 图片流投屏处理
                                resJson = handleStreamImageProjection(request, imageType, deviceId, deviceName,
                                        isThumbnail, imageId, rotation);
                            }
                            break;
                        case "stop":
                            LogUtils.e("enter method listener.stop");
                            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                                String projectId = request.getParameter("projectId");
                                StopResponseVo object = RemoteService.listener.stop(projectId);
                                resJson = new Gson().toJson(object);

                                GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
                            }
                            break;
                        case "rotate":
                            LogUtils.d("enter method listener.rotate");
                            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                                String projectId = request.getParameter("projectId");
                                RotateResponseVo object = RemoteService.listener.rotate(90, projectId);
                                resJson = new Gson().toJson(object);
                            }
                            break;
                        case "resume":
                            LogUtils.d("enter method listener.play");
                            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                                String projectId = request.getParameter("projectId");
                                PlayResponseVo object = RemoteService.listener.play(1, projectId);
                                resJson = new Gson().toJson(object);
                            }
                            break;
                        case "pause":
                            LogUtils.d("enter method listener.play");
                            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                                String projectId = request.getParameter("projectId");
                                PlayResponseVo object = RemoteService.listener.play(0, projectId);
                                resJson = new Gson().toJson(object);
                            }
                            break;
                        case "seek":
                            LogUtils.d("enter method listener.play");
                            int positionSeek = Integer.parseInt(request.getParameter("position"));
                            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                                String projectId = request.getParameter("projectId");
                                SeekResponseVo object = RemoteService.listener.seek(positionSeek, projectId);
                                resJson = new Gson().toJson(object);
                            }
                            break;
                        case "volume":
                            LogUtils.d("enter method listener.volume");
                            int volumeAction = Integer.parseInt(request.getParameter("action"));
                            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID)) {
                                String projectId = request.getParameter("projectId");
                                VolumeResponseVo object = RemoteService.listener.volume(volumeAction, projectId);
                                resJson = new Gson().toJson(object);
                            }
                            break;
                        case "query":
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
                            break;
                        case "showCode":
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
                            break;
                        case "verify":
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
                            break;
                        default:
                            LogUtils.d(" not enter any method");
                            BaseResponse baseResponse = new BaseResponse();
                            baseResponse.setInfo("错误的功能");
                            baseResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                            resJson = new Gson().toJson(baseResponse);
                            break;
                    }
                }
            }

            if (TextUtils.isEmpty(resJson)) {
                BaseResponse baseResponse = new BaseResponse();
                baseResponse.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                baseResponse.setInfo("操作失败");
                resJson = new Gson().toJson(baseResponse);
            }
            LogUtils.d("返回结果:" + resJson);
            response.getWriter().println(resJson);
        }

        private void handleStreamImageProjection(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
            String responseJson = "";

            MultipartConfigElement multipartConfigElement = new MultipartConfigElement((String) null);
            request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, multipartConfigElement);
            if (request.getParts() != null) {
                PrepareRequestVo prepareRequest = new PrepareRequestVo();
                Bitmap bitmap = null;
                for (Part part : request.getParts()) {
                    switch (part.getName()) {
                        case "fileUpload":
                            bitmap = BitmapFactory.decodeStream(part.getInputStream());
                            break;
                        case "deviceId":
                            prepareRequest.setDeviceId(StringUtils.inputStreamToString(part.getInputStream()));
                            break;
                        case "deviceName":
                            prepareRequest.setDeviceName(StringUtils.inputStreamToString(part.getInputStream()));
                            break;
                        case "isThumbnail":
                            int isThumbnail = Integer.parseInt(StringUtils.inputStreamToString(part.getInputStream()));
                            prepareRequest.setIsThumbnail(isThumbnail);
                            break;
                        case "imageId":
                            prepareRequest.setImageId(StringUtils.inputStreamToString(part.getInputStream()));
                            break;
                    }
                    part.delete();
                }

                if (!TextUtils.isEmpty(prepareRequest.getDeviceId()) &&
                        (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                                prepareRequest.getDeviceId().equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID))) {
                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = prepareRequest.getDeviceId();
                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = prepareRequest.getDeviceName();
                    BaseResponse object = null;
                    if (bitmap != null) {
                        boolean showImage = false;
                        if (prepareRequest.getIsThumbnail() == 1) {
                            // 缩略图
                            GlobalValues.CURRENT_PROJECT_IMAGE_ID = prepareRequest.getImageId();
                            showImage = true;
                        } else {
                            // 大图
                            if (!TextUtils.isEmpty(prepareRequest.getImageId()) &&
                                    prepareRequest.getImageId().equals(GlobalValues.CURRENT_PROJECT_IMAGE_ID)) {
                                showImage = true;
                            }
                        }
                        FileOutputStream outputStream = new FileOutputStream(AppUtils.getSDCardPath() + System.currentTimeMillis() + ".jpg");
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

                        if (showImage) {
                            // 显示图片
                            GlobalValues.CURRENT_PROJECT_BITMAP = bitmap;
                            object = RemoteService.listener.showImage(1, 0, prepareRequest.getIsThumbnail() == 1);
                            if (object.getResult() != ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                                GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
                                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
                            }
                        } else {
                            // 图片被忽略
                            object = new BaseResponse();
                            object.setResult(ConstantValues.SERVER_RESPONSE_CODE_IMAGE_ID_CHECK_FAILED);
                        }
                    }
                    responseJson = new Gson().toJson(object);
                } else {
                    PrepareResponseVo vo = new PrepareResponseVo();
                    vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                    responseJson = new Gson().toJson(vo);
                }
            }

            LogUtils.d("返回结果:" + responseJson);
            response.getWriter().println(responseJson);
        }

        private String handleStreamImageProjection(HttpServletRequest request, int imageType,
                                                 String deviceId, String deviceName, String isThumbnail,
                                                 String imageId, int rotation) throws IOException, ServletException {
            String respJson = "";
            if (!TextUtils.isEmpty(deviceId) &&
                    (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                            deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID))) {
                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
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
                                    break;
                            }
                            part.delete();
                        }

//                        FileOutputStream outputStream = new FileOutputStream(AppUtils.getSDCardPath() + System.currentTimeMillis() + ".jpg");
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        // 显示图片
                        GlobalValues.CURRENT_PROJECT_BITMAP = bitmap;
                        object = RemoteService.listener.showImage(imageType, rotation, "1".equals(isThumbnail));
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
                if (object.getResult() != ConstantValues.SERVER_RESPONSE_CODE_SUCCESS) {
                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = null;
                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = null;
                }

                respJson = new Gson().toJson(object);
            } else {
                BaseResponse vo = new BaseResponse();
                vo.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                vo.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                respJson = new Gson().toJson(vo);
            }
            return respJson;
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
                    RemoteService.this.server.setHandler(RemoteService.this.new ControllHandler());
                    RemoteService.this.server.start();
                    RemoteService.this.server.join();
                } catch (Exception var2) {
                    var2.printStackTrace();
                }
            }

        }
    }
}
