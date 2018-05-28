package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.RemotingException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public abstract class AbstractChannelHandler extends AbstractChannelHandlerSupport {

    protected ChannelHandler handler;

    protected AbstractChannelHandler(ChannelHandler handler) {
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws RemotingException {
        handler.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws RemotingException {
        handler.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {
        handler.channelRead(ctx, message);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws RemotingException {
        handler.write(ctx, msg, promise);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws RemotingException {
        handler.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable exception) throws RemotingException {
        handler.exceptionCaught(ctx, exception);
    }
}
