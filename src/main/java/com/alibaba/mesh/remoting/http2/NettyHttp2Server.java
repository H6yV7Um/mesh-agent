package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.netty.NettyClient;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * @author yiji
 */
public class NettyHttp2Server {

    public static final String NAME = "http2";

    public static final int DEFAULT_PORT = 20000;

    URL serverUrl = URL.valueOf("http://localhost:20000");

    EventLoopGroup group = new NioEventLoopGroup(Math.min(4, Runtime.getRuntime().availableProcessors()));

    Channel ch;

    public void start(){
        String port = System.getProperty(Constants.BIND_PORT_KEY, "20000");
        if(StringUtils.isNotEmpty(port)){
            serverUrl.setPort(Integer.parseInt(port));
        }
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 1024);
        // reuse netty client worker group
        try {
            b.group(group, NettyClient.nioWorkerGroup)
                    .channel(EpollServerSocketChannel.class)
//                    .channel(NioServerSocketChannel.class)
                    // .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new NettyHttp2ServerInitializer(serverUrl));
            ch = b.bind(serverUrl.getPort()).awaitUninterruptibly().channel();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop(){
        group.shutdownGracefully();
    }
}
