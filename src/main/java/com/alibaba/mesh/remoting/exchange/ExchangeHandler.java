package com.alibaba.mesh.remoting.exchange;

import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.RemotingException;

import io.netty.channel.ChannelHandlerContext;

/**
 * ExchangeHandler. (API, Prototype, ThreadSafe)
 */
public interface ExchangeHandler extends ChannelHandler {

    /**
     * reply.
     *
     * @param channel
     * @param request
     * @return response
     * @throws RemotingException
     */
    Object reply(ChannelHandlerContext ctx, Object request) throws RemotingException;

}