package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.remoting.WriteQueue;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;

import javax.annotation.Nonnull;

/**
 * @author yiji.github@hotmail.com
 */
public class SendRpcBufferCommand implements WriteQueue.QueuedCommand {

    ByteBuf buffer;
    ChannelPromise promise;

    public SendRpcBufferCommand(ByteBuf buffer, @Nonnull ChannelPromise promise) {
        this.buffer = buffer;
        this.promise = promise;
    }

    @Override
    public ChannelPromise promise() {
        return promise;
    }

    /**
     * Sets the promise.
     *
     * @param promise
     */
    @Override
    public void promise(ChannelPromise promise) {
        this.promise = promise;
    }

    @Override
    public void run(Channel channel) {
        channel.write(buffer, promise);
        // ReferenceCountUtil.release(buffer);
    }

}
