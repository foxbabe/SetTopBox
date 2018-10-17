/**
 * Copyright (c) 2016, Stupid Bird and/or its affiliates. All rights reserved.
 * STUPID BIRD PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @Project : netty
 * @Package : net.lizhaoweb.netty
 * @author <a href="http://www.lizhaoweb.net">李召(John.Lee)</a>
 * @EMAIL 404644381@qq.com
 * @Time : 13:35
 */
package cn.savor.small.netty;

import android.content.Context;

import com.savor.ads.core.Session;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * @author bichao
 * @version 1.0.0.0.1
 * @notes Created on 2016年12月08日<br>
 * Revision of last commit:$Revision$<br>
 * Author of last commit:$Author$<br>
 * Date of last commit:$Date$<br>
 */

public class MiniProNettyClient {
    static int PORT;
    static String HOST;
    public static Channel miniChannel = null;
    private MiniNettyMsgCallback callback;
    private Context mContext;
    private static MiniProNettyClient instance;

    public static void init(int port, String host, MiniNettyMsgCallback callback, Context context) {
        instance = new MiniProNettyClient(port, host, callback, context);
    }

    /**
     * get前请务必确保init了
     * @return
     */
    public static MiniProNettyClient get() {
        return instance;
    }

    private MiniProNettyClient(int port, String host, MiniNettyMsgCallback c, Context context) {
        this.PORT = port;
        this.HOST = host;
        this.callback = c;
        mContext = context;
    }

    public void setServer(int port, String host) {
        this.PORT = port;
        this.HOST = host;
    }

    public Bootstrap configureBootstrap(Bootstrap b) {
        return configureBootstrap(b, new NioEventLoopGroup());
    }
    public Bootstrap configureBootstrap(Bootstrap b, EventLoopGroup g) {
        b.group(g)
                .channel(NioSocketChannel.class)
                .remoteAddress(HOST, PORT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        LogUtils.i("mini client SocketChannel....................................." + MiniProNettyClient.HOST + ':' + MiniProNettyClient.PORT);
//                        LogFileUtil.write("mini client SocketChannel....................................." + MiniProNettyClient.HOST + ':' + MiniProNettyClient.PORT);
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("ping",new IdleStateHandler(60, 60, 20, TimeUnit.SECONDS));
                        //添加POJO对象解码器 禁止缓存类加载器
                        pipeline.addLast(new ObjectDecoder(1024*5, ClassResolvers.cacheDisabled(this.getClass().getClassLoader())));
                        //设置发送消息编码器
                        pipeline.addLast(new ObjectEncoder());
                        pipeline.addLast(new MiniProNettyClientHandler(callback, mContext));
                    }
                });

        return b;
    }

    public void connect(Bootstrap b) {
        b.connect().addListener(channelFutureListener);
    }

    ChannelFutureListener channelFutureListener = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if (channelFuture.cause() != null) {
                LogUtils.i("Failed to connect: " + channelFuture.cause());
            }
            miniChannel = channelFuture.channel();
            if (channelFuture.isSuccess()) {
                Session.get(mContext).setHeartbeatMiniNetty(true);
            }else{
                LogUtils.i("Mini Reconnect");
                Session.get(mContext).setHeartbeatMiniNetty(false);
                channelFuture.channel().close().sync();
                final EventLoop loop = channelFuture.channel().eventLoop();
                loop.schedule(new Runnable() {
                    @Override
                    public void run() {
                        connect(configureBootstrap(new Bootstrap(), loop));
                    }
                }, 5L, TimeUnit.SECONDS);
            }
        }
    };

    //关闭连接
    public void disConnect() {
        if (miniChannel != null) {
            try {
                LogFileUtil.write("NettyClient disConnect");
                miniChannel.close().removeListener(channelFutureListener);
                miniChannel.close().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public interface MiniNettyMsgCallback {
        void onReceiveMiniServerMsg(String msg, String content);
        void onMiniConnected();
        void onMiniReconnect();
        void onMiniCloseIcon();
    }
}
