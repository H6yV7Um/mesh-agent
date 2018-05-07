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
package com.alibaba.mesh.remoting.support;

import com.alibaba.mesh.remoting.AbstractChannelHandler;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.ChannelHandlerDispatcher;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.exchange.ExchangeHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * ExchangeHandlerDispatcher
 */
public class ExchangeHandlerDispatcher extends AbstractChannelHandler implements ExchangeHandler {

    private final ReplierDispatcher replierDispatcher;

    private final ChannelHandlerDispatcher handlerDispatcher;

    public ExchangeHandlerDispatcher() {
        replierDispatcher = new ReplierDispatcher();
        handlerDispatcher = new ChannelHandlerDispatcher();
    }

    public ExchangeHandlerDispatcher(Replier<?> replier) {
        replierDispatcher = new ReplierDispatcher(replier);
        handlerDispatcher = new ChannelHandlerDispatcher();
    }

    public ExchangeHandlerDispatcher(ChannelHandler... handlers) {
        replierDispatcher = new ReplierDispatcher();
        handlerDispatcher = new ChannelHandlerDispatcher(handlers);
    }

    public ExchangeHandlerDispatcher(Replier<?> replier, ChannelHandler... handlers) {
        replierDispatcher = new ReplierDispatcher(replier);
        handlerDispatcher = new ChannelHandlerDispatcher(handlers);
    }

    public ExchangeHandlerDispatcher addChannelHandler(ChannelHandler handler) {
        handlerDispatcher.addChannelHandler(handler);
        return this;
    }

    public ExchangeHandlerDispatcher removeChannelHandler(ChannelHandler handler) {
        handlerDispatcher.removeChannelHandler(handler);
        return this;
    }

    public <T> ExchangeHandlerDispatcher addReplier(Class<T> type, Replier<T> replier) {
        replierDispatcher.addReplier(type, replier);
        return this;
    }

    public <T> ExchangeHandlerDispatcher removeReplier(Class<T> type) {
        replierDispatcher.removeReplier(type);
        return this;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handlerDispatcher.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        handlerDispatcher.channelInactive(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
        handlerDispatcher.write(ctx, message, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        handlerDispatcher.channelRead(ctx, message);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable exception) {
        handlerDispatcher.exceptionCaught(ctx, exception);
    }

    @Override
    public Object reply(ChannelHandlerContext ctx, Object request) throws RemotingException {
        return ((Replier) replierDispatcher).reply(ctx, request);
    }

}
