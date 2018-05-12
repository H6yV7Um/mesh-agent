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

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.NetUtils;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.WriteQueue;
import com.alibaba.mesh.remoting.exchange.DefaultFuture;
import com.alibaba.mesh.remoting.exchange.ExchangeHandler;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.transport.AbstractChannelHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * ExchangeReceiver
 */
public class HeaderExchangeHandler extends AbstractChannelHandler {

    public WriteQueue writeQueue;

    protected static final Logger logger = LoggerFactory.getLogger(HeaderExchangeHandler.class);

    public HeaderExchangeHandler(ExchangeHandler handler) {
        super(handler);
    }

    static void handleResponse(Channel channel, Response response) throws RemotingException {
        if (response != null && !response.isHeartbeat()) {
            DefaultFuture.received(channel, response);
        }
    }

    private static boolean isClientSide(Channel channel) {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        URL url = channel.attr(Keys.URL_KEY).get();
        return url.getPort() == address.getPort() &&
                NetUtils.filterLocalHost(url.getIp())
                        .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }

    void handlerEvent(Channel channel, Request req) throws RemotingException {
        if (req.getData() != null && req.getData().equals(Request.READONLY_EVENT)) {
            channel.attr(Keys.READONLY_KEY).set(Boolean.TRUE);
        }
    }

    Response handleRequest(ChannelHandlerContext ctx, Request req) throws RemotingException {
        if (req.isBroken()) {
            Response response = new Response(req.getId(), req.getVersion());
            Object data = req.getData();

            String msg;
            if (data == null) msg = null;
            else if (data instanceof Throwable) msg = StringUtils.toString((Throwable) data);
            else msg = data.toString();
            response.setErrorMessage("Fail to decode request due to: " + msg);
            response.setStatus(Response.BAD_REQUEST);

            return response;
        }
        // find handler by message class.
        try {
            // handle data.
            handler.channelRead(ctx, req);
        } catch (Throwable e) {
            logger.error("Respone.SERVICE_ERROR", e);
            // response.setStatus(Response.SERVICE_ERROR);
            // response.setErrorMessage(StringUtils.toString(e));
        }
        return null;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws RemotingException {
        this.writeQueue = new WriteQueue(ctx.channel());
        Channel channel = ctx.channel();
        channel.attr(Keys.READ_TIMESTAMP).set(System.currentTimeMillis());
        channel.attr(Keys.WRITE_TIMESTAMP).set(System.currentTimeMillis());
        handler.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws RemotingException {
        Channel channel = ctx.channel();
        channel.attr(Keys.READ_TIMESTAMP).set(System.currentTimeMillis());
        channel.attr(Keys.WRITE_TIMESTAMP).set(System.currentTimeMillis());
        handler.channelInactive(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) throws RemotingException {
        Throwable exception = null;
        Channel channel = ctx.channel();
        try {
            channel.attr(Keys.WRITE_TIMESTAMP).set(System.currentTimeMillis());
//            writeQueue.enqueue(new SendRequestCommand(message,
//                    promise.addListener(new ChannelFutureListener() {
//                        @Override
//                        public void operationComplete(ChannelFuture future) throws Exception {
//                            if (message instanceof Request) {
//                                Request request = (Request) message;
//                                DefaultFuture.sent(channel, request);
//                            }
//                        }
//                    })), false);
            handler.write(ctx, message, promise);
        } catch (Throwable t) {
            exception = t;
        }
        if (exception != null) {
            if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else if (exception instanceof RemotingException) {
                throw (RemotingException) exception;
            } else {
                throw new RemotingException(channel.localAddress(), channel.remoteAddress(),
                        exception.getMessage(), exception);
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {
        Channel channel = ctx.channel();
        channel.attr(Keys.READ_TIMESTAMP).set(System.currentTimeMillis());
        if (message instanceof Request) {
            // handle request.
            Request request = (Request) message;
            if (request.isEvent()) {
                handlerEvent(channel, request);
            } else {
                if (request.isTwoWay()) {
                    Response response = handleRequest(ctx, request);
                    if (response != null) {
                        ctx.write(response);
                    }
                } else {
                    handler.channelRead(ctx, request);
                }
            }
        } else if (message instanceof Response) {
            handleResponse(channel, (Response) message);
        } else {
            handler.channelRead(ctx, message);
        }

        writeQueue.scheduleFlush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable exception) throws RemotingException {
//        if (exception instanceof ExecutionException) {
//            ExecutionException e = (ExecutionException) exception;
//            Object msg = e.getRequest();
//            if (msg instanceof Request) {
//                Request req = (Request) msg;
//                if (req.isTwoWay() && !req.isHeartbeat()) {
//                    Response res = new Response(req.getId(), req.getVersion());
//                    res.setStatus(Response.SERVER_ERROR);
//                    res.setErrorMessage(StringUtils.toString(e));
//                    writeQueue.enqueue(new SendRequestCommand(res, ctx.voidPromise()), true);
//                    return;
//                }
//            }
//        }
        handler.exceptionCaught(ctx, exception);
    }
}
