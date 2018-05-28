package com.alibaba.mesh.remoting;


import com.alibaba.mesh.remoting.transport.AbstractChannelHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * ChannelListenerDispatcher
 */
public class ChannelHandlerDispatcher extends AbstractChannelHandler implements ChannelHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChannelHandlerDispatcher.class);

    private final Collection<ChannelHandler> channelHandlers = new CopyOnWriteArraySet<ChannelHandler>();

    public ChannelHandlerDispatcher(@Nonnull ChannelHandler... handlers) {
        this(Arrays.asList(handlers));
    }

    public ChannelHandlerDispatcher(@Nonnull List<ChannelHandler> handlers) {
        super(handlers.get(0));
        this.channelHandlers.addAll(handlers);
    }

    public Collection<ChannelHandler> getChannelHandlers() {
        return channelHandlers;
    }

    public ChannelHandlerDispatcher addChannelHandler(ChannelHandler handler) {
        this.channelHandlers.add(handler);
        return this;
    }

    public ChannelHandlerDispatcher removeChannelHandler(ChannelHandler handler) {
        this.channelHandlers.remove(handler);
        return this;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.channelActive(ctx);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.channelInactive(ctx);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object message, ChannelPromise promise) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.write(ctx, message, promise);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.channelRead(ctx, message);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable exception) {
        for (ChannelHandler listener : channelHandlers) {
            try {
                listener.exceptionCaught(ctx, exception);
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
            }
        }
    }

}
