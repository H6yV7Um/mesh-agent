package com.alibaba.mesh.remoting.exchange;

import com.alibaba.mesh.remoting.Client;
import com.alibaba.mesh.remoting.RemotingException;

/**
 * ExchangeClient. (API/SPI, Prototype, ThreadSafe)
 *
 *
 */
public interface ExchangeClient extends Client {

    /**
     * send request.
     *
     * @param request
     * @return response future
     * @throws RemotingException
     */
    ResponseFuture request(Object request) throws RemotingException;

    /**
     * send request.
     *
     * @param request
     * @param timeout
     * @return response future
     * @throws RemotingException
     */
    ResponseFuture request(Object request, int timeout) throws RemotingException;

    /**
     * get message handler.
     *
     * @return message handler
     */
    ExchangeHandler getExchangeHandler();

}