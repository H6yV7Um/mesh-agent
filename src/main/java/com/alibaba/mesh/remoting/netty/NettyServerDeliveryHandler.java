package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.extension.ExtensionLoader;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.Codeable;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author yiji
 */
public class NettyServerDeliveryHandler extends ChannelDuplexHandler {

    private final URL url;
    private final ChannelHandler handler;
    private int timeout;

    private Channel endpointChannel;
    private ChannelHandlerContext serverCtx;

    private ChannelFuture future;

    private Codeable codec;

    private HashMap<Long, Request> requestIdMap = new HashMap<>(128 * 10);

    private static final Logger logger = LoggerFactory.getLogger(NettyServerDeliveryHandler.class);

    public NettyServerDeliveryHandler(URL url, ChannelHandler handler) {
        this.url = url;
        this.timeout = url.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        this.handler = handler;
        this.codec = getChannelCodec(url);
        if(timeout < 3000) timeout = 3000;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        this.serverCtx = ctx;
        handler.channelActive(ctx);

        int port = url.getParameter(Constants.ENDPOINT_PORT_KEY, -1);
        if(port < 0) throw new IllegalArgumentException("endpoint port is required, port '" + port + "'");
        String host = url.getParameter(Constants.ENDPOINT_HOST_KEY, "127.0.0.1");

        // connect to local service
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                .channel(NioSocketChannel.class);

        bootstrap.handler(new ChannelInitializer() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                NettyDecodebytesAdapter adapter = new NettyDecodebytesAdapter(codec, url);;
                ch.pipeline()
                        .addLast("decoder", adapter.getDecoder())
                        .addLast( "handler" , handler);
            }

            @Override
            public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {

                // received message from endpoint
                Response response = (Response)message;
                Request request = requestIdMap.remove(response.getRemoteId());

                if(request != null) {
                    response.setId(request.getId());
                    NettyServerDeliveryHandler.this.serverCtx.write(response);
                    handler.write(NettyServerDeliveryHandler.this.serverCtx, response, ctx.voidPromise());
                }

            }
        });

        future = bootstrap.connect(host, port);
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()) {
                    Channel endpointChannel = future.channel();
                    NettyServerDeliveryHandler.this.endpointChannel = endpointChannel;
                }else {
                    if(future.cause() != null){
                        throw new RemotingException(future.channel(), "mesh server(url: " + url + ") failed to connect to endpint "
                                + host + ":" + port + ", error message is:" + future.cause().getMessage(), future.cause());
                    }
                }
            }
        });
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Request request = (Request)msg;
        requestIdMap.put(request.getRemoteId(), request);
        // received message from mesh consumer
        if(future.channel().isActive()) {
            future.channel().writeAndFlush(request.getData());
        }else {
            if(future.awaitUninterruptibly(timeout) && future.isSuccess()){
                future.channel().writeAndFlush(request.getData());
            }else {
                logger.warn("received message from mesh client but failed to connnect endpoint " + future.channel());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        handler.exceptionCaught(ctx, cause);
    }

    protected static Codeable getChannelCodec(URL url) {
        String codecName = url.getParameter(Constants.ENDPOINT_NAME_KEY, "dubbo");
        return ExtensionLoader.getExtensionLoader(Codeable.class).getExtension(codecName);
    }
}
