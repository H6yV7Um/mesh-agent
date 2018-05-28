package com.alibaba.mesh.remoting;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.extension.Adaptive;
import com.alibaba.mesh.common.extension.SPI;
import com.alibaba.mesh.remoting.transport.dispatcher.DeliveryDispatcher;

/**
 * ChannelHandlerWrapper (SPI, Singleton, ThreadSafe)
 */
@SPI(DeliveryDispatcher.NAME)
public interface Dispatcher {

    /**
     * dispatch the message to threadpool.
     *
     * @param handler
     * @param url
     * @return channel handler
     */
    @Adaptive({Constants.DISPATCHER_KEY, "dispather", "channel.handler"})
    ChannelHandler dispatch(ChannelHandler handler, URL url);

}