package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.remoting.WriteQueue;
import com.alibaba.mesh.remoting.exchange.Response;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCounted;

import javax.annotation.Nonnull;

/**
 * @author yiji.github@hotmail.com
 */
public class SendResponseCommand implements WriteQueue.QueuedCommand {

    Response response;
    ChannelPromise promise;

    public SendResponseCommand(Response response, @Nonnull ChannelPromise promise) {
        this.response = response;
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
        channel.write(response, promise);
//        ReferenceCountUtil.release(response.getResult());
        ((ReferenceCounted) response.getResult()).release();
    }
}
