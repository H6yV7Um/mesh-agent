package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.CodecOutputList;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.exchange.MultiMessage;

import io.netty.channel.ChannelHandlerContext;

/**
 * @see MultiMessage
 */
public class MultiMessageHandlerSupport extends AbstractChannelHandler {

    public MultiMessageHandlerSupport(ChannelHandler handler) {
        super(handler);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {
        CodecOutputList list = (CodecOutputList) message;

        int i = 0, size = list.size();
        if (size == 1) {
            handler.channelRead(ctx, list.getUnsafe(i));
            return;
        }

        for (; i < size; i++) {
            handler.channelRead(ctx, list.getUnsafe(i));
        }
    }
}
