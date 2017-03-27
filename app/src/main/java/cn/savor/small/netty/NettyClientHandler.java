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


import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
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
public class NettyClientHandler extends SimpleChannelInboundHandler<MessageBean> {
    private NettyClient.NettyMessageCallback callback;
    private Session session;
    private Context mContext;

    public NettyClientHandler(NettyClient.NettyMessageCallback m, Context context) {
        this.callback = m;
        this.mContext = context;
        this.session = Session.get(context);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LogUtils.i("Client  Channel Active.................." + NettyClient.host + ':' + NettyClient.port);
        LogFileUtil.write("NettyClientHandler Client Channel Active.................." + NettyClient.host + ':' + NettyClient.port);
        if (callback != null) {
            callback.onConnected();
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
                    LogUtils.i("SERVER_HEART_RESP： 收到来自服务端的...心跳回应." + tmp + "===>>接收到内容:" + msg.getContent());
                }
                break;
            case SERVER_ORDER_REQ:
                List<String> contentMsg = msg.getContent();
                String order = "";
                String connectCode = "";
                for (int i = 0; i < contentMsg.size(); i++) {
                    String tmp = contentMsg.get(i);
                    if (i == 0) {
                        order = tmp;
                    } else if (i == 1) {
                        try {
                            InnerBean bean = new Gson().fromJson(tmp, new TypeToken<InnerBean>(){}.getType());
                            connectCode = bean.getConnectCode();
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (callback != null) {
                    callback.onReceiveServerMessage(order, connectCode);
                }
                MessageBean message = new MessageBean();
                message.setCmd(MessageBean.Action.CLIENT_ORDER_RESP);
                ArrayList<String> contList = new ArrayList<String>();
                String xContent = "我收到了.数据包..回应下";
                contList.add(xContent);
                message.setContent(contList);
                message.setIp(AppUtils.getLocalIPAddress());
                message.setMac(session.getEthernetMac());
                ctx.writeAndFlush(message);
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
                LogUtils.i("客户端向服务端发送====" + channelId + "====>>>>心跳包.....流水号:" + message.getSerialnumber());
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
        if (callback != null) {
            callback.onReconnect();
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
