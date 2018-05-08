package com.alibaba.mesh.remoting.transport.dispatcher;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.transport.WrappedChannelHandlerSupport;

import java.util.concurrent.ExecutorService;

/**
 * @author yiji
 */
public class DeliveryDispatcher extends WrappedChannelHandlerSupport {

    public static final String NAME = "delivery";

    public DeliveryDispatcher(ChannelHandler handler, URL url) {
        super(handler, url);
    }

    private ExecutorService getExecutorService() {
        ExecutorService cexecutor = executor;
        if (cexecutor == null || cexecutor.isShutdown()) {
            cexecutor = SHARED_EXECUTOR;
        }
        return cexecutor;
    }

}
