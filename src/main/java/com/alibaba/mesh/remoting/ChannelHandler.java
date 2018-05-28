package com.alibaba.mesh.remoting;

import com.alibaba.mesh.common.extension.SPI;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;

import java.net.SocketAddress;


/**
 * ChannelHandler. (API, Prototype, ThreadSafe)
 *
 * @see com.alibaba.agent.remoting.Transporter#bind(com.alibaba.agent.common.URL, ChannelHandler)
 * @see com.alibaba.agent.remoting.Transporter#connect(com.alibaba.agent.common.URL, ChannelHandler)
 */
@SPI
public interface ChannelHandler extends ChannelInboundHandler, ChannelOutboundHandler {

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was registered with its {@link EventLoop}
     */
    @Override
    void channelRegistered(ChannelHandlerContext ctx) throws RemotingException;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was unregistered from its {@link EventLoop}
     */
    @Override
    void channelUnregistered(ChannelHandlerContext ctx) throws RemotingException;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} is now active
     */
    @Override
    void channelActive(ChannelHandlerContext ctx) throws RemotingException;

    /**
     * The {@link Channel} of the {@link ChannelHandlerContext} was registered is now inactive and reached its
     * end of lifetime.
     */
    @Override
    void channelInactive(ChannelHandlerContext ctx) throws RemotingException;

    /**
     * Invoked when the current {@link Channel} has read a message from the peer.
     */
    @Override
    void channelRead(ChannelHandlerContext ctx, Object msg) throws RemotingException;

    /**
     * Invoked when the last message read by the current read operation has been consumed by
     * {@link #channelRead(ChannelHandlerContext, Object)}.  If {@link ChannelOption#AUTO_READ} is off, no further
     * attempt to read an inbound data from the current {@link Channel} will be made until
     * {@link ChannelHandlerContext#read()} is called.
     */
    @Override
    void channelReadComplete(ChannelHandlerContext ctx) throws RemotingException;

    /**
     * Gets called if an user event was triggered.
     */
    @Override
    void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws RemotingException;

    /**
     * Gets called once the writable state of a {@link Channel} changed. You can check the state with
     * {@link Channel#isWritable()}.
     */
    @Override
    void channelWritabilityChanged(ChannelHandlerContext ctx) throws RemotingException;

    /**
     * Gets called if a {@link Throwable} was thrown.
     */
    @Override
    @SuppressWarnings("deprecation")
    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws RemotingException;

    /**
     * Called once a bind operation is made.
     *
     * @param ctx           the {@link ChannelHandlerContext} for which the bind operation is made
     * @param localAddress  the {@link SocketAddress} to which it should bound
     * @param promise       the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception    thrown if an error occurs
     */
    @Override
    void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws RemotingException;

    /**
     * Called once a connect operation is made.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the connect operation is made
     * @param remoteAddress     the {@link SocketAddress} to which it should connect
     * @param localAddress      the {@link SocketAddress} which is used as source on connect
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */
    @Override
    void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress,
                 SocketAddress localAddress, ChannelPromise promise) throws RemotingException;

    /**
     * Called once a disconnect operation is made.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the disconnect operation is made
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */
    @Override
    void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws RemotingException;

    /**
     * Called once a close operation is made.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the close operation is made
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */
    @Override
    void close(ChannelHandlerContext ctx, ChannelPromise promise) throws RemotingException;

    /**
     * Called once a deregister operation is made from the current registered {@link EventLoop}.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the close operation is made
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */
    @Override
    void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws RemotingException;

    /**
     * Intercepts {@link ChannelHandlerContext#read()}.
     */
    @Override
    void read(ChannelHandlerContext ctx) throws RemotingException;

    /**
     * Called once a write operation is made. The write operation will write the messages through the
     * {@link ChannelPipeline}. Those are then ready to be flushed to the actual {@link Channel} once
     * {@link Channel#flush()} is called
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the write operation is made
     * @param msg               the message to write
     * @param promise           the {@link ChannelPromise} to notify once the operation completes
     * @throws Exception        thrown if an error occurs
     */
    @Override
    void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws RemotingException;

    /**
     * Called once a flush operation is made. The flush operation will try to flush out all previous written messages
     * that are pending.
     *
     * @param ctx               the {@link ChannelHandlerContext} for which the flush operation is made
     * @throws Exception        thrown if an error occurs
     */
    @Override
    void flush(ChannelHandlerContext ctx) throws RemotingException;

}