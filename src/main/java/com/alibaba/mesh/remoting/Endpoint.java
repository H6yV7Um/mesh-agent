package com.alibaba.mesh.remoting;

import com.alibaba.mesh.common.URL;

import java.net.SocketAddress;

/**
 * Endpoint. (API/SPI, Prototype, ThreadSafe)
 *
 * @see Channel
 * @see com.alibaba.agent.remoting.Client
 * @see Server
 */
public interface Endpoint {

    /**
     * get url.
     *
     * @return url
     */
    URL getUrl();

    /**
     * get channel handler.
     *
     * @return channel handler
     */
    ChannelHandler getChannelHandler();

    /**
     * get local address.
     *
     * @return local address.
     */
    SocketAddress getLocalAddress();

    SocketAddress getRemoteAddress();

    boolean isConnected();

    /**
     * send message then waiting for result.
     *
     * @param message
     * @throws RemotingException
     */
    void write(Object message) throws RemotingException;

    /**
     * close the channel.
     */
    void close();

    /**
     * Graceful close the channel.
     */
    void close(int timeout);

    void startClose();

    /**
     * is closed.
     *
     * @return closed
     */
    boolean isClosed();

}