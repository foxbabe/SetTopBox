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
//        LogFileUtil.write("MiniProNettyClientHandler Client Channel Active.................." + NettyClient.host + ':' + NettyClient.port);
        if (miniCallback != null) {
            miniCallback.onMiniConnected();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageBean msg) throws Exception {
        LogUtils.i(("mini Client received: " + msg.getCmd()));
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




    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        String channelId = ctx.channel().id().toString();
        if (evt instanceof IdleStateEvent) {

            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state().equals(IdleState.READER_IDLE)) {
                //未进行读操作
                LogUtils.i("READER_IDLE");
                LogFileUtil.write("MiniNettyClientHandler READER_IDLE");
                // 超时关闭channel
                ctx.close();

            } else if (event.state().equals(IdleState.WRITER_IDLE)) {

                LogUtils.i("WRITER_IDLE");
                LogFileUtil.write("MiniNettyClientHandler WRITER_IDLE");
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
                ArrayList<String> contList = new ArrayList<>();
                contList.add("I am a mini Heart Pakage...");
                contList.add(new Gson().toJson(bean));
                message.setContent(contList);
                ctx.writeAndFlush(message);
                LogUtils.v("小程序NETTY客户端向服务端发送====" + channelId + "====>>>>心跳包.....流水号:" + message.getSerialnumber());
//                LogFileUtil.write("MiniNettyClientHandler 客户端向服务端发送====" + channelId + "====>>>>心跳包.....流水号:" + message.getSerialnumber());
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
//        ctx.close();
        if (miniCallback!=null){
            miniCallback.onMiniCloseIcon();
        }
        Session.get(mContext).setHeartbeatMiniNetty(false);
        LogUtils.i("客户端出现异常，退出........" + ConstantValues.MINI_PROGRAM_NETTY_URL + ':' + ConstantValues.MINI_PROGRAM_NETTY_PORT);
//        LogFileUtil.write("NettyClientHandler 客户端出现异常，退出........" + ConstantValues.MINI_PROGRAM_NETTY_URL + ':' + ConstantValues.MINI_PROGRAM_NETTY_PORT);
//        reconnect(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
//        super.channelUnregistered(ctx);
        LogUtils.i("channelUnregistered......." + ConstantValues.MINI_PROGRAM_NETTY_URL + ':' + ConstantValues.MINI_PROGRAM_NETTY_PORT);
//        LogFileUtil.write("NettyClientHandler channelUnregistered......." + ConstantValues.MINI_PROGRAM_NETTY_URL + ':' + ConstantValues.MINI_PROGRAM_NETTY_PORT);
//        reconnect(ctx);
    }

    @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        LogUtils.i("channelInactive......." + ConstantValues.MINI_PROGRAM_NETTY_URL + ':' + ConstantValues.MINI_PROGRAM_NETTY_PORT);
//        LogFileUtil.write("MiniNettyClientHandler channelInactive......." + ConstantValues.MINI_PROGRAM_NETTY_URL + ':' + ConstantValues.MINI_PROGRAM_NETTY_PORT);
//        reconnect(ctx);
        Session.get(mContext).setHeartbeatMiniNetty(false);
        if (miniCallback!=null){
            miniCallback.onMiniCloseIcon();
        }
        reconnect(ctx);
    }

    public void reconnect(ChannelHandlerContext ctx) {
//        if (miniCallback != null) {
//            miniCallback.onMiniReconnect();
//        }
        try {
            Thread.sleep(3000);
            if (ctx != null) {
                ctx.close();
            }
            ctx.channel().close().sync();
            ctx.close().sync();
            final EventLoop loop = ctx.channel().eventLoop();
            loop.schedule(new Runnable() {
                @Override
                public void run() {
                    LogUtils.i("Reconnecting to: " + ConstantValues.MINI_PROGRAM_NETTY_URL + ':' + ConstantValues.MINI_PROGRAM_NETTY_PORT);
//                    LogFileUtil.write("NettyClientHandler Reconnecting to: " + ConstantValues.MINI_PROGRAM_NETTY_URL + ':' + ConstantValues.MINI_PROGRAM_NETTY_PORT);
                    Bootstrap bootstrap = MiniProNettyClient.get().configureBootstrap(new Bootstrap(), loop);
                    MiniProNettyClient.get().connect(bootstrap);
                }
            }, 5L, TimeUnit.SECONDS);
        } catch (Exception ex) {
            ex.toString();
        }
    }
}
