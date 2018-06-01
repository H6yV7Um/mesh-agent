package com.alibaba.mesh.remoting.support.header;

import com.alibaba.mesh.remoting.exchange.DefaultFuture;
import com.alibaba.mesh.remoting.exchange.InternalLongObjectHashMap;
import com.alibaba.mesh.remoting.exchange.Response;

import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yiji.github@hotmail.com
 */
public class DefaultFutureThreadLocal {

    protected static final Logger logger = LoggerFactory.getLogger(HeaderExchangeHandler.class);

    private static final FastThreadLocal<InternalLongObjectHashMap<DefaultFuture>> LOCAL = new FastThreadLocal<InternalLongObjectHashMap<DefaultFuture>>() {
        @Override
        protected InternalLongObjectHashMap<DefaultFuture> initialValue() {
            return new InternalLongObjectHashMap<DefaultFuture>(4096);
        }
    };

    public static DefaultFuture getResponseFuture(long id) {
        return LOCAL.get().get(id);
    }

    public static void putResponseFuture(long id, DefaultFuture guard) {
        LOCAL.get().put(id, guard);
    }

    public static DefaultFuture getAndRemoveResponseFuture(long id) {
        return LOCAL.get().remove(id);
    }


    private static class RemotingInvocationTimeoutScan implements Runnable {

        @Override
        public void run() {
            try {
                for (DefaultFuture future : LOCAL.get().values()) {
                    if (future == null || future.isDone()) {
                        continue;
                    }
                    if (System.currentTimeMillis() - future.getStartTimestamp() > future.getTimeout()) {
                        // create exception response.
                        Response timeoutResponse = new Response(future.getId());
                        // set timeout status.
                        timeoutResponse.setStatus(future.isSent() ? Response.SERVER_TIMEOUT : Response.CLIENT_TIMEOUT);
                        String message = future.getTimeoutMessage(true);
                        timeoutResponse.setErrorMessage(message);
                        // handle response.
                        future.doReceived(timeoutResponse);

                        if (logger.isDebugEnabled()) {
                            logger.debug(message);
                        }
                    }
                }
            } catch (Throwable e) {
                logger.error("Exception when scan the timeout invocation of remoting.", e);
            }
        }
    }


}
