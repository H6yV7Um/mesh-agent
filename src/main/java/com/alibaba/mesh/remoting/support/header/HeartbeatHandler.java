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

package com.alibaba.mesh.remoting.support.header;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.transport.AbstractChannelHandlerDelegate;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatHandler extends AbstractChannelHandlerDelegate {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatHandler.class);

    public HeartbeatHandler(ChannelHandler handler) {
        super(handler);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws RemotingException {
        Channel channel = ctx.channel();
        setReadTimestamp(channel);
        setWriteTimestamp(channel);
        handler.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws RemotingException {
        Channel channel = ctx.channel();
        clearReadTimestamp(channel);
        clearWriteTimestamp(channel);
        handler.channelInactive(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) throws RemotingException {
        setWriteTimestamp(ctx.channel());
        handler.write(ctx, message, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {
        Channel channel = ctx.channel();
        setReadTimestamp(channel);
        if (isHeartbeatRequest(message)) {
            Request req = (Request) message;
            if (req.isTwoWay()) {
                Response response = new Response(req.getId(), req.getVersion());
                response.setEvent(Response.HEARTBEAT_EVENT);
                channel.writeAndFlush(response);
                if (logger.isInfoEnabled()) {
                    int heartbeat = channel.attr(Keys.URL_KEY).get().getParameter(Constants.HEARTBEAT_KEY, 0);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Received heartbeat from remote channel " + channel.remoteAddress()
                                + ", cause: The channel has no data-transmission exceeds a heartbeat period"
                                + (heartbeat > 0 ? ": " + heartbeat + "ms" : ""));
                    }
                }
            }
            return;
        }
        if (isHeartbeatResponse(message)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Receive heartbeat response in thread " + Thread.currentThread().getName());
            }
            return;
        }
        handler.channelRead(ctx, message);
    }

    private void setReadTimestamp(Channel channel) {
        channel.attr(Keys.READ_TIMESTAMP).set(System.currentTimeMillis());
    }

    private void setWriteTimestamp(Channel channel) {
        channel.attr(Keys.WRITE_TIMESTAMP).set(System.currentTimeMillis());
    }

    private void clearReadTimestamp(Channel channel) {
        channel.attr(Keys.READ_TIMESTAMP).set(null);
    }

    private void clearWriteTimestamp(Channel channel) {
        channel.attr(Keys.WRITE_TIMESTAMP).set(null);
    }

    private boolean isHeartbeatRequest(Object message) {
        return message instanceof Request && ((Request) message).isHeartbeat();
    }

    private boolean isHeartbeatResponse(Object message) {
        return message instanceof Response && ((Response) message).isHeartbeat();
    }
}
