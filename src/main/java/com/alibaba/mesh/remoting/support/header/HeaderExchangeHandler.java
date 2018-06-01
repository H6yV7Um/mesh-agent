package com.alibaba.mesh.remoting.support.header;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.NetUtils;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.RemotingException;
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

    protected static final Logger logger = LoggerFactory.getLogger(HeaderExchangeHandler.class);

    public HeaderExchangeHandler(ExchangeHandler handler) {
        super(handler);
    }

    private static boolean isClientSide(Channel channel) {
        InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
        URL url = channel.attr(Keys.URL_KEY).get();
        return url.getPort() == address.getPort() &&
                NetUtils.filterLocalHost(url.getIp())
                        .equals(NetUtils.filterLocalHost(address.getAddress().getHostAddress()));
    }

    void handleResponse(ChannelHandlerContext ctx, Response response) throws RemotingException {
        if (response != null && !response.isEvent()) {
            DefaultFuture responseFuture = DefaultFutureThreadLocal.getAndRemoveResponseFuture(response.getId());
            if (responseFuture != null) {
                responseFuture.doReceived(response);
            }
        }
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
            if (message instanceof Request) {
                if (message instanceof Request) {
                    // handle request.
                    Request request = (Request) message;
                    if (request.isTwoWay()) {
                        DefaultFutureThreadLocal.putResponseFuture(request.getId(), request.guard());
                    }
                }
            }
            channel.attr(Keys.WRITE_TIMESTAMP).set(System.currentTimeMillis());
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
                        ctx.writeAndFlush(response);
                    }
                } else {
                    handler.channelRead(ctx, request);
                }
            }
        } else if (message instanceof Response) {
            handleResponse(ctx, (Response) message);
        } else {
            handler.channelRead(ctx, message);
        }
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
//                    writeQueue.enqueue(new InvokeMethodCommand(res, ctx.voidPromise()), true);
//                    return;
//                }
//            }
//        }
        handler.exceptionCaught(ctx, exception);
    }
}
