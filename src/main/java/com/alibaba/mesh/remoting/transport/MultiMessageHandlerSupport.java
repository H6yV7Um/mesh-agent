package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.exchange.MultiMessage;

import io.netty.channel.ChannelHandlerContext;

/**
 *
 * @see MultiMessage
 */
public class MultiMessageHandlerSupport extends AbstractChannelHandler {

    public MultiMessageHandlerSupport(ChannelHandler handler) {
        super(handler);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {
        if (message instanceof MultiMessage) {
            MultiMessage list = (MultiMessage) message;
            for (Object obj : list) {
                handler.channelRead(ctx, obj);
            }
        } else {
            handler.channelRead(ctx, message);
        }
    }
}
