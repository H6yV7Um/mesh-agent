package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.NamedThreadFactory;
import com.alibaba.mesh.remoting.ChannelHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WrappedChannelHandlerSupport extends AbstractChannelHandler {

    protected static final Logger logger = LoggerFactory.getLogger(WrappedChannelHandlerSupport.class);

    protected static final ExecutorService SHARED_EXECUTOR = Executors.newCachedThreadPool(new NamedThreadFactory("DubboSharedHandler", true));

    protected ExecutorService executor;

    protected URL url;

    public WrappedChannelHandlerSupport(ChannelHandler handler, URL url) {
        super(handler);
        this.url = url;

//        this.executor = (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);
//
//        String componentKey = Constants.EXECUTOR_SERVICE_COMPONENT_KEY;
//        if (Constants.CONSUMER_SIDE.equalsIgnoreCase(url.getParameter(Constants.SIDE_KEY))) {
//            componentKey = Constants.CONSUMER_SIDE;
//        }
//        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
//        dataStore.put(componentKey, Integer.toString(url.getPort()), executor);
    }

    public void close() {
        try {
            if (executor instanceof ExecutorService) {
                ((ExecutorService) executor).shutdown();
            }
        } catch (Throwable t) {
            logger.warn("fail to destroy thread pool of server: " + t.getMessage(), t);
        }
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public URL getUrl() {
        return url;
    }

}
