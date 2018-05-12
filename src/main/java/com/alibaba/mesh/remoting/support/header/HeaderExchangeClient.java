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
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.NamedThreadFactory;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.Client;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.WriteQueue;
import com.alibaba.mesh.remoting.exchange.DefaultFuture;
import com.alibaba.mesh.remoting.exchange.ExchangeClient;
import com.alibaba.mesh.remoting.exchange.ExchangeHandler;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.netty.SendRequestCommand;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * DefaultMessageClient
 */
public class HeaderExchangeClient implements ExchangeClient {

    private static final Logger logger = LoggerFactory.getLogger(HeaderExchangeClient.class);

    private static final ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(2, new NamedThreadFactory("dubbo-remoting-client-heartbeat", true));
    private final Client client;
    private final Channel channel;
    // heartbeat timer
    private ScheduledFuture<?> heartbeatTimer;
    // heartbeat(ms), default value is 0 , won't execute a heartbeat.
    private int heartbeat;
    private int heartbeatTimeout;

    private WriteQueue writeQueue;

    public HeaderExchangeClient(Client client, boolean needHeartbeat) {
        if (client == null) {
            throw new IllegalArgumentException("client == null");
        }
        this.client = client;
        this.channel = client.getChannel();
        this.writeQueue = new WriteQueue(channel);
        String dubbo = client.getUrl().getParameter(Constants.MESH_VERSION_KEY);
        this.heartbeat = client.getUrl().getParameter(Constants.HEARTBEAT_KEY, dubbo != null && dubbo.startsWith("1.0.") ? Constants.DEFAULT_HEARTBEAT : 0);
        this.heartbeatTimeout = client.getUrl().getParameter(Constants.HEARTBEAT_TIMEOUT_KEY, heartbeat * 3);
        if (heartbeatTimeout < heartbeat * 2) {
            throw new IllegalStateException("heartbeatTimeout < heartbeatInterval * 2");
        }
        if (needHeartbeat) {
            startHeartbeatTimer();
        }
    }

    @Override
    public DefaultFuture request(Object message) throws RemotingException {

        if (!channel.isActive()) {
            throw new RemotingException(this.channel.localAddress(), null, "Failed to send message " + message + ", cause: The channel " + this + " is closed!");
        }

        int timeout = channel.attr(Keys.URL_KEY).get()
                .getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);

        // create request.
        Request req = new Request();
        req.setVersion("2.0.0");
        req.setTwoWay(true);
        req.setData(message);
        DefaultFuture future = new DefaultFuture(channel, req, timeout);

        writeQueue.enqueue(new SendRequestCommand(req, channel.newPromise().addListener(
                new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture ft) throws Exception {
                        if (ft.isCancelled()) {
                            future.cancel();
                            return;
                        }

                        if (!ft.isSuccess()) {
                            logger.error("failed to request message " + message, ft.cause());
                            Response response = new Response(req.getId());
                            response.setStatus(Response.CLIENT_ERROR);
                            response.setResult(ft.cause());
                            response.setErrorMessage("failed to reqeuest message " + message);
                            DefaultFuture.received(channel, response);
                        }
                    }
                }
        )), true);

        return future;
    }

    @Override
    public DefaultFuture request(Object message, int timeout) throws RemotingException {

        if (!channel.isActive()) {
            throw new RemotingException(this.channel.localAddress(), null, "Failed to send request " + message + ", cause: The channel " + this + " is closed!");
        }

        long start = System.currentTimeMillis();
        // create request.
        Request req = new Request();
        req.setVersion("2.0.0");
        req.setTwoWay(true);
        req.setData(message);
        DefaultFuture future = new DefaultFuture(channel, req, timeout);

        writeQueue.enqueue(new SendRequestCommand(req, channel.newPromise().addListener(
                new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture ft) throws Exception {

                        if (ft.isCancelled()) {
                            future.cancel();
                            return;
                        }

                        long elapsed = System.currentTimeMillis() - start;
                        if (elapsed > timeout) {
                            Response response = new Response(req.getId());
                            response.setStatus(Response.SERVER_TIMEOUT);
                            response.setErrorMessage("server side timeout, elapsed: " + elapsed + " ms");
                            DefaultFuture.received(channel, response);
                        }

                    }
                }
        )), true);

        return future;
    }

    /**
     * get message handler.
     *
     * @return message handler
     */
    @Override
    public ExchangeHandler getExchangeHandler() {
        return null;
    }

    @Override
    public void write(Object message) throws RemotingException {

        if (!channel.isActive()) {
            throw new RemotingException(this.channel.localAddress(), null, "Failed to send message " + message + ", cause: The channel " + this + " is closed!");
        }

        int timeout = channel.attr(Keys.URL_KEY).get()
                .getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);

        // create request.
        Request req = new Request();
        req.setVersion("2.0.0");
        req.setTwoWay(false);
        req.setData(message);
        DefaultFuture future = new DefaultFuture(channel, req, timeout);

        writeQueue.enqueue(new SendRequestCommand(req, channel.newPromise().addListener(
                new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture ft) throws Exception {
                        if (!ft.isSuccess()) {
                            logger.error("failed to request message " + message, ft.cause());
                        }
                    }
                }
        )), true);
    }

    @Override
    public URL getUrl() {
        return client.getUrl();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) client.getRemoteAddress();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return client.getChannelHandler();
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public boolean isClosed() {
        return !channel.isActive();
    }

    @Override
    public void close() {
        doClose();
        channel.close();
    }

    @Override
    public void close(int timeout) {
        // Mark the client into the closure process
        startClose();
        doClose();
        client.close(timeout);
    }

    @Override
    public void startClose() {
        client.startClose();
    }

    @Override
    public void reset(URL url) {
        client.reset(url);
    }

    @Override
    public void reconnect() throws RemotingException {
        client.reconnect();
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    private void startHeartbeatTimer() {
        stopHeartbeatTimer();
        if (heartbeat > 0) {
            heartbeatTimer = scheduled.scheduleWithFixedDelay(
                    new HeartBeatTask(new HeartBeatTask.ChannelProvider() {
                        @Override
                        public Collection<Object> getChannelHolders() {
                            return Collections.<Object>singletonList(HeaderExchangeClient.this);
                        }
                    }, heartbeat, heartbeatTimeout),
                    heartbeat, heartbeat, TimeUnit.MILLISECONDS);
        }
    }

    private void stopHeartbeatTimer() {
        if (heartbeatTimer != null && !heartbeatTimer.isCancelled()) {
            try {
                heartbeatTimer.cancel(true);
                scheduled.purge();
            } catch (Throwable e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        heartbeatTimer = null;
    }

    private void doClose() {
        stopHeartbeatTimer();
    }

    @Override
    public String toString() {
        return "HeaderExchangeClient [channel=" + channel + "]";
    }
}
