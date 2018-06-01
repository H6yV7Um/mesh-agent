package com.alibaba.mesh.remoting.exchange;

/**
 * Future. (API/SPI, Prototype, ThreadSafe)
 *
 * @see com.alibaba.agent.remoting.exchange.ExchangeChannel#request(Object)
 * @see com.alibaba.agent.remoting.exchange.ExchangeChannel#request(Object, int)
 */
public interface ResponseFuture {

    /**
     * set callback.
     *
     * @param callback
     */
    void setCallback(ResponseCallback callback);

    /**
     * check is done.
     *
     * @return done or not.
     */
    boolean isDone();

}