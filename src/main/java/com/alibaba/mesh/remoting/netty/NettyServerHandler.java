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
import com.alibaba.mesh.common.utils.NetUtils;
import com.alibaba.mesh.remoting.ChannelHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NettyClientHandler
 */
@io.netty.channel.ChannelHandler.Sharable
public class NettyServerHandler extends ChannelDuplexHandler {

    private final Map<String, Channel> channels = new ConcurrentHashMap<String, Channel>(); // <ip:port, channel>

    private final URL url;

    private final ChannelHandler handler;

    public NettyServerHandler(URL url, ChannelHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    public Map<String, Channel> getChannels() {
        return channels;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress());
        channels.put(remoteAddress, ctx.channel());
        try{
            handler.channelActive(ctx);

            // connect to local service



        }finally {
            if(!ctx.channel().isActive()){
                channels.remove(remoteAddress);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress());
        channels.remove(remoteAddress);
    }


    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise future)
            throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String remoteAddress = NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress());
        channels.put(remoteAddress, ctx.channel());
        try{
            handler.channelRead(ctx, msg);
        }finally {
            if(!ctx.channel().isActive()){
                channels.remove(remoteAddress);
            }
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        try{
            handler.write(ctx, msg, promise);
        }finally {
            if(!ctx.channel().isActive()){
                String remoteAddress = NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress());
                channels.remove(remoteAddress);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        try{
            handler.exceptionCaught(ctx, cause);
        }finally {
            if(!ctx.channel().isActive()){
                String remoteAddress = NetUtils.toAddressString((InetSocketAddress) ctx.channel().remoteAddress());
                channels.remove(remoteAddress);
            }
        }
    }
}