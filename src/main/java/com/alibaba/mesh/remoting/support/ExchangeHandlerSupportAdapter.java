package com.alibaba.mesh.remoting.support;

import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.exchange.ExchangeHandler;
import com.alibaba.mesh.remoting.transport.AbstractChannelHandlerSupport;

import io.netty.channel.ChannelHandlerContext;

/**
 * ExchangeHandlerSupportAdapter
 */
public abstract class ExchangeHandlerSupportAdapter extends AbstractChannelHandlerSupport implements ExchangeHandler {

    @Override
    public Object reply(ChannelHandlerContext ctx, Object msg) throws RemotingException {
        return null;
    }

}
