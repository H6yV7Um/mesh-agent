package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.ChannelHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NettyClientHandler
 */
@io.netty.channel.ChannelHandler.Sharable
public class NettyServerStatisticHandler extends ChannelDuplexHandler {

    // channel -> channel
    private final Map<Channel, Channel> channels = new ConcurrentHashMap<Channel, Channel>();

    private final URL url;

    private final ChannelHandler handler;

    public NettyServerStatisticHandler(URL url, ChannelHandler handler) {
        this.url = url;
        this.handler = handler;
    }

    public Map<Channel, Channel> getChannels() {
        return channels;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if(!channels.containsKey(channel)){
            channels.put(channel, channel);
        }
        handler.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel());
        handler.channelInactive(ctx);
    }


    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise future)
            throws Exception {
        handler.disconnect(ctx, future);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        if(!ctx.channel().isActive()){
            channels.remove(channel);
        }
        handler.channelRead(ctx, msg);
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Channel channel = ctx.channel();
        if(!channel.isActive()){
            channels.remove(channel);
        }
        handler.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        Channel channel = ctx.channel();
        if(!channel.isActive()){
            channels.remove(channel);
        }
        handler.exceptionCaught(ctx, cause);
    }
}