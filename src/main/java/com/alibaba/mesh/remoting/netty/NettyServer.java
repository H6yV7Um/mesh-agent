package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.ExecutorUtil;
import com.alibaba.mesh.common.utils.NetUtils;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.Server;
import com.alibaba.mesh.remoting.transport.AbstractServer;
import com.alibaba.mesh.remoting.transport.dispatcher.ChannelHandlers;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * NettyServer
 */
public class NettyServer extends AbstractServer implements Server {

    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    private Map<Channel, Channel> channels; // <ip:port, channel>

    private ServerBootstrap bootstrap;

    private Channel channel;

//    private EpollEventLoopGroup bossGroup;
//    private EpollEventLoopGroup workerGroup;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    public NettyServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, ChannelHandlers.wrap(handler, ExecutorUtil.setThreadName(url, SERVER_THREAD_POOL_NAME)));
    }

    @Override
    protected void doOpen() throws Throwable {
        bootstrap = new ServerBootstrap();

//        bossGroup = new NioEventLoopGroup(Math.max(4, Runtime.getRuntime().availableProcessors()),
//                new DefaultThreadFactory("NettyServerBoss", true));
        logger.warn("available processors: " + Runtime.getRuntime().availableProcessors());
        bossGroup = new NioEventLoopGroup(Math.max(4, Runtime.getRuntime().availableProcessors()),
                new DefaultThreadFactory("NettyServerBoss", true));
        workerGroup = new NioEventLoopGroup(getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS),
                new DefaultThreadFactory("NettyServerWorker", true));

        final NettyServerStatisticHandler statisticHandler = new NettyServerStatisticHandler(getUrl(), this);
        channels = statisticHandler.getChannels();

//        bootstrap.group(bossGroup, bossGroup)
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
//                .channel(EpollServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
                        NettyServerDeliveryHandler deliveryHandler = new NettyServerDeliveryHandler(getUrl(), NettyServer.this);
                        ch.pipeline()
                                // .addLast("logging"  , new LoggingHandler(LogLevel.INFO))
                                .addLast("decoder"  , adapter.getDecoder())
                                .addLast("encoder"  , adapter.getEncoder())
                                .addLast("statistic", statisticHandler)
                                .addLast("handler"  , deliveryHandler);
                    }
                });
        // bind
        ChannelFuture channelFuture = bootstrap.bind(getBindAddress());
        channelFuture.syncUninterruptibly();
        channel = channelFuture.channel();

    }

    @Override
    protected void doClose() throws Throwable {
        try {
            if (channel != null) {
                // unbind.
                channel.close();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            Collection<Channel> channels = getChannels();
            if (channels != null && channels.size() > 0) {
                for (Channel channel : channels) {
                    try {
                        channel.close();
                    } catch (Throwable e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (bootstrap != null) {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
        try {
            if (channels != null) {
                channels.clear();
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public Collection<Channel> getChannels() {
        Collection<Channel> chs = new HashSet<Channel>();
        for (Channel channel : this.channels.values()) {
            if (channel.isActive()) {
                chs.add(channel);
            } else {
                channels.remove(NetUtils.toAddressString((InetSocketAddress) channel.remoteAddress()));
            }
        }
        return chs;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        return channels.get(NetUtils.toAddressString(remoteAddress));
    }

    @Override
    public boolean isBound() {
        return channel.isActive();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public boolean isConnected() {
        return channel.isActive();
    }
}
