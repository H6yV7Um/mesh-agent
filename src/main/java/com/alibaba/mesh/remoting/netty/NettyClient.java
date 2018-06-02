package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.Version;
import com.alibaba.mesh.common.utils.NetUtils;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.transport.AbstractClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * NettyClient.
 */
public class NettyClient extends AbstractClient {

    public static final NioEventLoopGroup nioWorkerGroup = new NioEventLoopGroup(Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyClientWorker", true));
    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);
    //    public static final NioEventLoopGroup nioWorkerGroup = new NioEventLoopGroup(Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyClientWorker", true));
//    public static final EpollEventLoopGroup nioWorkerGroup = new EpollEventLoopGroup(Constants.DEFAULT_IO_THREADS, new DefaultThreadFactory("NettyClientWorker", true));
    private static volatile boolean startedHttpServer = false;
    private Bootstrap bootstrap;
    private volatile Channel channel; // volatile, please copy reference to use

    public NettyClient(final URL url, final ChannelHandler handler) throws RemotingException {
        super(url, wrapChannelHandler(url, handler));
    }

    @Override
    protected void doOpen() throws Throwable {
        logger.warn("available processors: " + Runtime.getRuntime().availableProcessors());

        nioWorkerGroup.setIoRatio(Integer.parseInt(System.getProperty("mesh.rpc.io.ratio", "100")));

        bootstrap = new Bootstrap();
        bootstrap.group(nioWorkerGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout())
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .channel(NioSocketChannel.class);
//                .channel(EpollSocketChannel.class);

        if (getTimeout() < 3000) {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
        } else {
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getTimeout());
        }

        bootstrap.handler(new ChannelInitializer() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                logger.warn("create client channel: " + ch);
                NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyClient.this);
                NettyClientHandler nettyClientHandler = new NettyClientHandler(getUrl(), NettyClient.this);
                ch.pipeline()
                        .addLast("decoder", adapter.getDecoder())
                        .addLast("encoder", adapter.getEncoder())
                        .addLast("handler", nettyClientHandler);
            }
        });
    }

    @Override
    protected void doConnect() throws Throwable {
        long start = System.currentTimeMillis();
        ChannelFuture future = bootstrap.connect(getConnectAddress());
        try {
            boolean ret = future.awaitUninterruptibly(getTimeout(), TimeUnit.MILLISECONDS);

            if (ret && future.isSuccess()) {
                Channel newChannel = future.channel();
                try {
                    // Close old channel
                    Channel oldChannel = NettyClient.this.channel; // copy reference
                    if (oldChannel != null) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Close old netty channel " + oldChannel + " on create new netty channel " + newChannel);
                        }
                        oldChannel.close();
                    }
                } finally {
                    if (NettyClient.this.isClosed()) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Close new netty channel " + newChannel + ", because the client closed.");
                        }
                        newChannel.close();
                    } else {
                        NettyClient.this.channel = newChannel;
                    }
                }
            } else if (future.cause() != null) {
                throw new RemotingException(this.channel, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + ", error message is:" + future.cause().getMessage(), future.cause());
            } else {
                throw new RemotingException(this.channel, "client(url: " + getUrl() + ") failed to connect to server "
                        + getRemoteAddress() + " client-side timeout "
                        + getConnectTimeout() + "ms (elapsed: " + (System.currentTimeMillis() - start) + "ms) from netty client "
                        + NetUtils.getLocalHost() + " using mesh version " + Version.getVersion());
            }
        } finally {
            if (!isConnected()) {
                future.cancel(true);
            }
        }
    }

    @Override
    protected void doDisConnect() throws Throwable {

    }

    @Override
    protected void doClose() throws Throwable {
        //can't shutdown nioWorkerGroup
        //nioWorkerGroup.shutdownGracefully();

    }

    @Override
    public Channel getChannel() {
        return channel;
    }

}
