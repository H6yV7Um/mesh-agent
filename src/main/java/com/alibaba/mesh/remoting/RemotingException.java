/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.mesh.remoting;

import io.netty.channel.Channel;

import java.net.SocketAddress;

/**
 * RemotingException. (API, Prototype, ThreadSafe)
 *
 * @export
 * @see com.alibaba.agent.remoting.exchange.ResponseFuture#get()
 * @see com.alibaba.agent.remoting.exchange.ResponseFuture#get(int)
 * @see Channel#send(Object, boolean)
 * @see com.alibaba.agent.remoting.exchange.ExchangeChannel#request(Object)
 * @see com.alibaba.agent.remoting.exchange.ExchangeChannel#request(Object, int)
 * @see com.alibaba.agent.remoting.Transporter#bind(com.alibaba.agent.common.URL, ChannelHandler)
 * @see com.alibaba.agent.remoting.Transporter#connect(com.alibaba.agent.common.URL, ChannelHandler)
 */
public class RemotingException extends Exception {

    private static final long serialVersionUID = -3160452149606778709L;

    private SocketAddress localAddress;

    private SocketAddress remoteAddress;

    public RemotingException(Channel channel, String msg) {
        this(channel == null ? null : channel.localAddress(), channel == null ? null : channel.remoteAddress(), msg);
    }

    public RemotingException(SocketAddress localAddress, SocketAddress remoteAddress, String message) {
        super(message);

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public RemotingException(Channel channel, Throwable cause) {
        this(channel == null ? null : channel.localAddress(), channel == null ? null : channel.remoteAddress(),
                cause);
    }

    public RemotingException(SocketAddress localAddress, SocketAddress remoteAddress, Throwable cause) {
        super(cause);

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public RemotingException(Channel channel, String message, Throwable cause) {
        this(channel == null ? null : channel.localAddress(), channel == null ? null : channel.remoteAddress(),
                message, cause);
    }

    public RemotingException(SocketAddress localAddress, SocketAddress remoteAddress, String message,
                             Throwable cause) {
        super(message, cause);

        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketAddress getRemoteAddress() {
        return remoteAddress;
    }
}