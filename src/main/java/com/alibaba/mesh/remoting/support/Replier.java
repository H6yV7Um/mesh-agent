package com.alibaba.mesh.remoting.support;

import com.alibaba.mesh.remoting.RemotingException;

import io.netty.channel.ChannelHandlerContext;

/**
 * Replier. (API, Prototype, ThreadSafe)
 */
public interface Replier<T> {

    /**
     * reply.
     *
     * @param channel
     * @param request
     * @return response
     * @throws RemotingException
     */
    Object reply(ChannelHandlerContext ctx, T request) throws RemotingException;

}