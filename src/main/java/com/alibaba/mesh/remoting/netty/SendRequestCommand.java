package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.remoting.WriteQueue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;

import javax.annotation.Nonnull;

/**
 * @author yiji
 */
public class SendRequestCommand implements WriteQueue.QueuedCommand {

    Object msg;
    ChannelPromise promise;

    public SendRequestCommand(Object msg, @Nonnull ChannelPromise promise){
        this.msg = msg;
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
        channel.write(msg, promise);
    }
}
