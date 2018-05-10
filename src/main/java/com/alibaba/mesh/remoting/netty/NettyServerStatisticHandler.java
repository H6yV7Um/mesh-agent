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
package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.ChannelHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NettyClientHandler
 */
@io.netty.channel.ChannelHandler.Sharable
public class NettyServerStatisticHandler extends ChannelDuplexHandler {

    // channel -> channel
    private final Map<Channel, Channel> channels = new ConcurrentHashMap<Channel, Channel>();

    private final URL url;

    private final ChannelHandler handler;

    public NettyServerStatisticHandler(URL url, ChannelHandler handler) {
        this.url = url;
        this.handler = handler;
    }

    public Map<Channel, Channel> getChannels() {
        return channels;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if(!channels.containsKey(channel)){
            channels.put(channel, channel);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel());
    }


    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise future)
            throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        if(!ctx.channel().isActive()){
            channels.remove(channel);
            return;
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Channel channel = ctx.channel();
        if(!channel.isActive()){
            channels.remove(channel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        Channel channel = ctx.channel();
        if(!channel.isActive()){
            channels.remove(channel);
        }
    }
}