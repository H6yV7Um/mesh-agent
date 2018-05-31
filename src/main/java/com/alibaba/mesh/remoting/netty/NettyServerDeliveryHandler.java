package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.extension.ExtensionLoader;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.Codeable;
import com.alibaba.mesh.remoting.CodecOutputList;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.WriteQueue;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.rpc.protocol.mesh.ExchangeCodec;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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

    private Codeable codeable;

    public HashMap<Long, Request> requestIdMap = new HashMap<>(128 * 10);

    private WriteQueue writeQueue;

    private WriteQueue writeToEndpoint;

    private Thread monitor;

    private static final Logger logger = LoggerFactory.getLogger(NettyServerDeliveryHandler.class);

    Bootstrap bootstrap;

    int port;

    String host;

    public NettyServerDeliveryHandler(URL url, ChannelHandler handler) {
        this.url = url;
        this.timeout = url.getPositiveParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
        this.handler = handler;
        this.codeable = getChannelCodec(url);
        if (timeout < 3000) timeout = 3000;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        this.serverCtx = ctx;
        this.writeQueue = new WriteQueue(ctx.channel());
        handler.channelActive(ctx);

        ctx.channel().attr(Keys.URL_KEY).set(url);

        port = url.getParameter(Constants.ENDPOINT_PORT_KEY, -1);
        String dubboPort = System.getProperty(Constants.DUBBO_ENDPOINT_PORT_KEY);
        // read port from env.
        if (StringUtils.isNotEmpty(dubboPort)) {
            port = Integer.parseInt(dubboPort);
        }

        if (port < 0) throw new IllegalArgumentException("endpoint port is required, port '" + port + "'");
        host = url.getParameter(Constants.ENDPOINT_HOST_KEY, "127.0.0.1");

        // connect to local service
        bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
//                .channel(EpollSocketChannel.class);
                .channel(NioSocketChannel.class);

        bootstrap.handler(new RemoteChannelInitializer());

        future = bootstrap.connect(host, port);
        int finalPort = port;
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    Channel endpointChannel = future.channel();
                    NettyServerDeliveryHandler.this.endpointChannel = endpointChannel;
                } else {
                    if (future.cause() != null) {
                        throw new RemotingException(future.channel(), "mesh server(url: " + url + ") failed to connect to endpint "
                                + host + ":" + finalPort + ", error message is:" + future.cause().getMessage(), future.cause());
                    }
                }
            }
        });

        this.writeToEndpoint = new WriteQueue(future.channel());
    }

    /**
     * We will receive message from mesh consumer side.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Request request = (Request) msg;
        requestIdMap.put(request.getRemoteId(), request);

        // received message from mesh consumer
        writeToEndpoint.enqueue(new SendRequestCommand(request.getData(), future.channel().voidPromise()), false);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        writeToEndpoint.scheduleFlush();
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

    class RemoteChannelInitializer extends ChannelInitializer {

        @Override
        protected void initChannel(Channel ch) throws Exception {
            NettyDecodebytesAdapter adapter = new NettyDecodebytesAdapter(codeable, url);
            ch.pipeline()
                    .addLast("remote-decoder", adapter.getDecoder())
                    .addLast("remote-inbound", new RemoteInboundChannelHandler());
        }
    }

    class RemoteInboundChannelHandler extends ChannelInboundHandlerAdapter {
        /**
         * We will receive message response from remote enpont(eg dubbo)
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {

            CodecOutputList list = (CodecOutputList) message;

            int i = 0, size = list.size();
            if (size == 1) {
                // received message from endpoint
                ByteBuf payload = (ByteBuf) list.getUnsafe(i);
                long remoteId = codeable.getRequestId(payload);

                boolean isEvent = codeable.isEvent(payload);

                if (isEvent) {
                    payload.setByte(2, ExchangeCodec.FLAG_TWOWAY | ExchangeCodec.FLAG_EVENT | 6);
                    payload.setByte(3, Response.OK);
                    ctx.writeAndFlush(payload, ctx.voidPromise());
                    System.out.println("endpoint event received and responsed, id: " + remoteId);
                    return;
                }

                byte status = codeable.getStatus(payload);
                Request request = requestIdMap.remove(remoteId);
                if (request != null) {
                    if (status != Response.OK) {
                        System.out.println("endpoint response received, id: " + remoteId + ", status: " + status);
                        return;
                    }

                    Response response = new Response(request.getId());
                    response.setStatus(status);

                    response.setId(request.getId());
                    response.setResult(payload);

                    NettyServerDeliveryHandler.this.writeQueue.enqueue(new SendRequestCommand(response,
                            NettyServerDeliveryHandler.this.serverCtx.voidPromise()), true);
                }
                return;
            }

            for (; i < size; i++) {

                ByteBuf payload = (ByteBuf) list.getUnsafe(i);
                long remoteId = codeable.getRequestId(payload);

                boolean isEvent = codeable.isEvent(payload);

                if (isEvent) {
                    payload.setByte(2, ExchangeCodec.FLAG_TWOWAY | ExchangeCodec.FLAG_EVENT | 6);
                    payload.setByte(3, Response.OK);
                    ctx.writeAndFlush(payload, ctx.voidPromise());
                    System.out.println("endpoint event received and responsed, id: " + remoteId);
                    return;
                }

                byte status = codeable.getStatus(payload);
                Request request = requestIdMap.remove(remoteId);
                if (request != null) {
                    if (status != Response.OK) {
                        System.out.println("endpoint response received, id: " + remoteId + ", status: " + status);
                        return;
                    }

                    Response response = new Response(request.getId());
                    response.setStatus(status);

                    response.setId(request.getId());
                    response.setResult(payload);

                    NettyServerDeliveryHandler.this.writeQueue.enqueue(new SendRequestCommand(response,
                            NettyServerDeliveryHandler.this.serverCtx.voidPromise()), false);
                }
            }

            NettyServerDeliveryHandler.this.writeQueue.scheduleFlush();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            logger.error("endpoint disconnect from channel: " + ctx.channel());
        }
    }
}
