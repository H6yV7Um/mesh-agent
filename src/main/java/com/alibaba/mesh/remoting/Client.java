package com.alibaba.mesh.remoting;

import io.netty.channel.Channel;

/**
 * Remoting Client. (API/SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Client%E2%80%93server_model">Client/Server</a>
 *
 * @see Transporter#connect(com.alibaba.agent.common.URL, ChannelHandler)
 */
public interface Client extends Endpoint, Resetable {

    /**
     * reconnect.
     */
    void reconnect() throws RemotingException;

    Channel getChannel();
}