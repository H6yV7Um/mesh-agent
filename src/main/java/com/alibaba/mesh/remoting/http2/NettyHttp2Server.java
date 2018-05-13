package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.netty.NettyClient;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author yiji
 */
public class NettyHttp2Server {

    public static final String NAME = "http2";

    public static final int DEFAULT_PORT = 20000;

    static URL serverUrl = URL.valueOf("http://localhost:20000");

    public static void main(String[] args) throws Exception {

        String port = System.getProperty("server.port", "20000");
        if(StringUtils.isNotEmpty(port)){
            serverUrl.setPort(Integer.parseInt(port));
        }

        EventLoopGroup group = new NioEventLoopGroup(Math.min(4, Runtime.getRuntime().availableProcessors()));
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 1024);
            // reuse netty client worker group
            b.group(group, NettyClient.nioWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    // .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new NettyHttp2ServerInitializer(serverUrl));

            Channel ch = b.bind(DEFAULT_PORT).sync().channel();

            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}
