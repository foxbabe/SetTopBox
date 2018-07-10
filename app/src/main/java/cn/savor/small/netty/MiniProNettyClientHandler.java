/**
 * Copyright (c) 2016, Stupid Bird and/or its affiliates. All rights reserved.
 * STUPID BIRD PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @Project : netty
 * @Package : net.lizhaoweb.netty
 * @author <a href="http://www.lizhaoweb.net">李召(John.Lee)</a>
 * @EMAIL 404644381@qq.com
 * @Time : 13:38
 */
package cn.savor.small.netty;


import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.jar.savor.box.vo.AdvResponseBean;
import com.jar.savor.box.vo.ResponseT1;
import com.jar.savor.box.vo.SpecialtyResponseBean;
import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.RstrSpecialty;
import com.savor.ads.callback.ProjectOperationListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.database.DBHelper;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.GlobalValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @author bichao</a>
 * @version 1.0.0.0.1
 * @notes Created on 2016年12月05日<br>
 * Revision of last commit:$Revision$<br>
 * Author of last commit:$Author$<br>
 * Date of last commit:$Date$<br>
 */
@ChannelHandler.Sharable
public class MiniProNettyClientHandler extends SimpleChannelInboundHandler<MessageBean> {
    private MiniProNettyClient.MiniNettyMsgCallback miniCallback;
    private Session session;
    private Context mContext;


    public MiniProNettyClientHandler(MiniProNettyClient.MiniNettyMsgCallback m, Context context) {
        this.miniCallback = m;
        this.mContext = context;
        this.session = Session.get(context);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LogUtils.i("Client  Channel Active.................." + NettyClient.host + ':' + NettyClient.port);
        LogUtils.i("miniCallback.................." + miniCallback);
        LogFileUtil.write("NettyClientHandler Client Channel Active.................." + NettyClient.host + ':' + NettyClient.port);
        if (miniCallback != null) {
            miniCallback.onMiniConnected();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageBean msg) throws Exception {
//        LogUtils.i(("Client received: " + ByteBufUtil.hexDump(msg.readBytes(msg.readableBytes())));
        if (msg == null) return;
        /**来自服务端的数据包请求*/
        MessageBean.Action Order = msg.getCmd();
        switch (Order) {
            case SERVER_HEART_RESP:
                List<String> contentMsgH = msg.getContent();
                for (String tmp : contentMsgH) {
                    LogUtils.v("SERVER_HEART_RESP： 收到来自服务端的...心跳回应." + tmp + "===>>接收到内容:" + msg.getContent());
                }
                break;
            case SERVER_ORDER_REQ:
                List<String> contentMsg = msg.getContent();

                MessageBean response = new MessageBean();
                response.setCmd(MessageBean.Action.CLIENT_ORDER_RESP);
                response.setSerialnumber(msg.getSerialnumber());
                response.setContent(new ArrayList<String>());

                if (contentMsg != null && contentMsg.size() > 1) {
                    String order = contentMsg.get(0);
                    String params = contentMsg.get(1);
                    response.getContent().add(order);

                    if (ConstantValues.NETTY_MINI_PROGRAM_COMMAND.equals(order)){
                        LogUtils.d("Netty command: show mini program " + params);
                        if (miniCallback!=null&&contentMsg.size()>2){
                            miniCallback.onReceiveMiniServerMsg(order,contentMsg.get(2));
                        }
                    }
                }
                ctx.writeAndFlush(response);
                break;
            default:
                break;
        }
    }

    private void handleSpecialty(String json, MessageBean response) {
        try {
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(json);
            String deviceId = jsonObject.get("deviceId").getAsString();
            String deviceName = jsonObject.get("deviceName").getAsString();
            String specialtyIds = jsonObject.get("specialtyId").getAsString();
            int interval = jsonObject.get("interval").getAsInt();

            ResponseT1<SpecialtyResponseBean> resp = new ResponseT1<>();

            if (TextUtils.isEmpty(specialtyIds)) {
                resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                resp.setInfo("未选择任何特色菜");
            } else {

                ArrayList<String> paths = new ArrayList<>();
                String failedIds = null;
                if ("-1".equals(specialtyIds)) {
                    List<RstrSpecialty> specialties = DBHelper.get(mContext).findSpecialtyByWhere(null, null);

                    if (specialties != null && specialties.size() > 0) {
                        for (RstrSpecialty specialty : specialties) {
                            paths.add(specialty.getMedia_path());
                        }
                    }
                } else {
                    String[] ids = specialtyIds.split(",");
                    failedIds = "";
                    for (int i = 0; i < ids.length; i++) {
                        String id = ids[i].trim();

                        String selection = DBHelper.MediaDBInfo.FieldName.FOOD_ID + "=?";
                        String[] selectionArgs = new String[]{id};
                        List<RstrSpecialty> specialties = DBHelper.get(mContext).findSpecialtyByWhere(selection, selectionArgs);

                        if (specialties != null && specialties.size() > 0) {
                            paths.add(specialties.get(0).getMedia_path());
                        } else {
                            failedIds += id + ",";
                        }
                    }
                    if (!TextUtils.isEmpty(failedIds)) {
                        failedIds = failedIds.substring(0, failedIds.length() - 1);
                    }
                }

                if (paths.size() > 0) {
                    if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                            deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                            GlobalValues.IS_RSTR_PROJECTION) {
                        boolean isNewDevice = TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID);

                        GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                        GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                        GlobalValues.IS_RSTR_PROJECTION = true;
                        GlobalValues.CURRENT_PROJECT_DEVICE_IP = NettyClient.host;
                        AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                        SpecialtyResponseBean specialtyResponseBean = new SpecialtyResponseBean();
                        specialtyResponseBean.setFounded_count(paths.size());
                        specialtyResponseBean.setFailed_ids(failedIds);
                        resp.setContent(specialtyResponseBean);
                        if (TextUtils.isEmpty(failedIds)) {
                            resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                            resp.setInfo("投屏成功");
                        } else {
                            resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_SPECIALTY_INCOMPLETE);
                            resp.setInfo(failedIds);
                        }
                        ProjectOperationListener.getInstance(mContext).showSpecialty(paths, interval, isNewDevice);
                    } else {
                        resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                        if (GlobalValues.IS_LOTTERY) {
                            resp.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                        } else {
                            resp.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                        }
                    }
                } else {
                    resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    resp.setInfo("未发现任何对应的特色菜");
                }
            }

            response.getContent().add(new Gson().toJson(resp));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleGreeting(String json, MessageBean response) {
        try {
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(json);
            String deviceId = jsonObject.get("deviceId").getAsString();
            String deviceName = jsonObject.get("deviceName").getAsString();
            String words = jsonObject.get("word").getAsString();
            int template = jsonObject.get("templateId").getAsInt();

            ResponseT1 resp = new ResponseT1();
            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    GlobalValues.IS_RSTR_PROJECTION) {
                boolean isNewDevice = TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID);

                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                GlobalValues.IS_RSTR_PROJECTION = true;
                GlobalValues.CURRENT_PROJECT_DEVICE_IP = NettyClient.host;
                AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                resp.setInfo("投屏成功");

                ProjectOperationListener.getInstance(mContext).showGreeting(words, template, 1000 * 60 * 5, isNewDevice);

//                Intent intent = new Intent(mContext, GreetingService.class);
//                intent.putExtra(GreetingService.EXTRA_DEVICE_ID, deviceId);
//                intent.putExtra(GreetingService.EXTRA_DEVICE_NAME, deviceName);
//                intent.putExtra(GreetingService.EXTRA_WORDS, words);
//                intent.putExtra(GreetingService.EXTRA_TEMPLATE, template);
//                try {
//                    mContext.unbindService(connection);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                mContext.bindService(intent, connection, Service.BIND_AUTO_CREATE);
            } else {
                resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                if (GlobalValues.IS_LOTTERY) {
                    resp.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                } else {
                    resp.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                }
            }

            response.getContent().add(new Gson().toJson(resp));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleAdv(String json, MessageBean response) {
        try {
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(json);
            String deviceId = jsonObject.get("deviceId").getAsString();
            String deviceName = jsonObject.get("deviceName").getAsString();
            String videoIds = jsonObject.get("vid").getAsString();

            String[] ids = videoIds.split(",");
            String failedIds = "";
            ArrayList<String> paths = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                String id = ids[i].trim();

                String selection = null;
                String[] selectionArgs = null;
                if (!"-1".equals(id)) {
                    selection = DBHelper.MediaDBInfo.FieldName.VID + "=?";
                    selectionArgs = new String[]{id};
                }
                List<MediaLibBean> videos = DBHelper.get(mContext).findPlayListByWhere(selection, selectionArgs);

                if (videos != null && videos.size() > 0) {
                    paths.add(videos.get(0).getMediaPath());
                } else {
                    failedIds += id + ",";
                }
            }

            if (!TextUtils.isEmpty(failedIds)) {
                failedIds = failedIds.substring(0, failedIds.length() - 1);
            }

            ResponseT1<AdvResponseBean> resp = new ResponseT1();
            if (paths.size() > 0) {
                if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                        deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                        GlobalValues.IS_RSTR_PROJECTION) {
                    boolean isNewDevice = TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID);

                    GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                    GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                    GlobalValues.IS_RSTR_PROJECTION = true;
                    GlobalValues.CURRENT_PROJECT_DEVICE_IP = NettyClient.host;
                    AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                    AdvResponseBean advResponseBean = new AdvResponseBean();
                    advResponseBean.setFailed_ids(failedIds);
                    resp.setContent(advResponseBean);
                    if (TextUtils.isEmpty(failedIds)) {
                        resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                        resp.setInfo("投屏成功");
                    } else {
                        resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_SPECIALTY_INCOMPLETE);
                        resp.setInfo(failedIds);
                    }
                    ProjectOperationListener.getInstance(mContext).showAdv(paths, isNewDevice);
                } else {
                    resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                    if (GlobalValues.IS_LOTTERY) {
                        resp.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                    } else {
                        resp.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                    }
                }
            } else {
                resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                resp.setInfo("未发现任何对应的宣传片");
            }

            response.getContent().add(new Gson().toJson(resp));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleGreetingThenSpecialty(String json, MessageBean response) {
        try {
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(json);
            String deviceId = jsonObject.get("deviceId").getAsString();
            String deviceName = jsonObject.get("deviceName").getAsString();
            String words = jsonObject.get("word").getAsString();
            int template = jsonObject.get("templateId").getAsInt();

            ResponseT1<SpecialtyResponseBean> resp = new ResponseT1<>();
            if (TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID) ||
                    GlobalValues.IS_RSTR_PROJECTION) {
                boolean isNewDevice = TextUtils.isEmpty(GlobalValues.CURRENT_PROJECT_DEVICE_ID);

                GlobalValues.CURRENT_PROJECT_DEVICE_ID = deviceId;
                GlobalValues.CURRENT_PROJECT_DEVICE_NAME = deviceName;
                GlobalValues.IS_RSTR_PROJECTION = true;
                GlobalValues.CURRENT_PROJECT_DEVICE_IP = NettyClient.host;
                AppApi.resetPhoneInterface(GlobalValues.CURRENT_PROJECT_DEVICE_IP);

                ArrayList<String> paths = new ArrayList<>();
                int interval;
                List<RstrSpecialty> specialties = DBHelper.get(mContext).findSpecialtyByWhere(null, null);

                if (specialties != null && specialties.size() > 0) {
                    for (RstrSpecialty specialty : specialties) {
                        paths.add(specialty.getMedia_path());
                    }
                }
                if (paths.size() > 1) {
                    interval = 10;
                } else {
                    interval = 20;
                }

                resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                SpecialtyResponseBean specialtyResponseBean = new SpecialtyResponseBean();
                specialtyResponseBean.setFailed_ids("");
                specialtyResponseBean.setFounded_count(paths.size());
                resp.setContent(specialtyResponseBean);
                resp.setInfo("投屏成功");

                ProjectOperationListener.getInstance(mContext).showGreetingThenSpecialty(words, template, 1000 * 60 * 5, paths, interval, isNewDevice);

            } else {
                resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                if (GlobalValues.IS_LOTTERY) {
                    resp.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在砸蛋");
                } else {
                    resp.setInfo("请稍等，" + GlobalValues.CURRENT_PROJECT_DEVICE_NAME + " 正在投屏");
                }
            }

            response.getContent().add(new Gson().toJson(resp));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleStopProjection(String json, MessageBean response) {
        try {
            JsonObject jsonObject = (JsonObject) new JsonParser().parse(json);
            String deviceId = jsonObject.get("deviceId").getAsString();

            ResponseT1 resp = new ResponseT1();
            if (!TextUtils.isEmpty(deviceId) && deviceId.equals(GlobalValues.CURRENT_PROJECT_DEVICE_ID) && GlobalValues.IS_RSTR_PROJECTION) {
                ProjectOperationListener.getInstance(mContext).rstrStop();
                resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_SUCCESS);
                resp.setInfo("停止成功");

                GlobalValues.IS_RSTR_PROJECTION = false;
                GlobalValues.CURRENT_PROJECT_IMAGE_ID = null;
            } else {
                resp.setResult(ConstantValues.SERVER_RESPONSE_CODE_FAILED);
                resp.setInfo("您的投屏已退出，无需再次停止");
            }

            response.getContent().add(new Gson().toJson(resp));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        String channelId = ctx.channel().id().toString();
        if (evt instanceof IdleStateEvent) {

            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state().equals(IdleState.READER_IDLE)) {
                //未进行读操作
                LogUtils.i("READER_IDLE");
                LogFileUtil.write("NettyClientHandler READER_IDLE");
                // 超时关闭channel
                ctx.close();

            } else if (event.state().equals(IdleState.WRITER_IDLE)) {

                LogUtils.i("WRITER_IDLE");
                LogFileUtil.write("NettyClientHandler WRITER_IDLE");
                // 超时关闭channel
                ctx.close();
            } else if (event.state().equals(IdleState.ALL_IDLE)) {
                //未进行读写
                //客户端发起REQ心跳查询==========
                LogUtils.i("ALL_IDLE");
                // 发送心跳消息
                MessageBean message = new MessageBean();
                message.setCmd(MessageBean.Action.CLIENT_HEART_REQ);
                String number = channelId + System.currentTimeMillis();
                message.setSerialnumber(number);
                message.setIp(AppUtils.getLocalIPAddress());
                message.setMac(session.getEthernetMac());
                InnerBean bean = new InnerBean();
                bean.setHotelId(session.getBoiteId());
                bean.setRoomId(session.getRoomId());
                bean.setSsid(AppUtils.getShowingSSID(mContext));
                bean.setBoxId(session.getBoxId());
                ArrayList<String> contList = new ArrayList<String>();
                contList.add("I am a Heart Pakage...");
                contList.add(new Gson().toJson(bean));
                message.setContent(contList);
                ctx.writeAndFlush(message);
                LogUtils.v("客户端向服务端发送====" + channelId + "====>>>>心跳包.....流水号:" + message.getSerialnumber());
                LogFileUtil.write("NettyClientHandler 客户端向服务端发送====" + channelId + "====>>>>心跳包.....流水号:" + message.getSerialnumber());
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
//        ctx.close();
        LogUtils.i("客户端出现异常，退出........" + NettyClient.host + ':' + NettyClient.port);
        LogFileUtil.write("NettyClientHandler 客户端出现异常，退出........" + NettyClient.host + ':' + NettyClient.port);
//        reconnect(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
//        super.channelUnregistered(ctx);
        LogUtils.i("channelUnregistered......." + NettyClient.host + ':' + NettyClient.port);
        LogFileUtil.write("NettyClientHandler channelUnregistered......." + NettyClient.host + ':' + NettyClient.port);
        reconnect(ctx);
    }

    public void reconnect(ChannelHandlerContext ctx) {
        if (miniCallback != null) {
            miniCallback.onMiniReconnect();
        }
        try {
            Thread.sleep(3000);
            if (ctx != null) {
                ctx.close();
            }
            final EventLoop loop = ctx.channel().eventLoop();
            loop.schedule(new Runnable() {
                @Override
                public void run() {
                    LogUtils.i("Reconnecting to: " + NettyClient.host + ':' + NettyClient.port);
                    LogFileUtil.write("NettyClientHandler Reconnecting to: " + NettyClient.host + ':' + NettyClient.port);
                    NettyClient.get().connect(NettyClient.get().configureBootstrap(new Bootstrap(), loop));
                }
            }, 5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            ex.toString();
        }
    }
}
