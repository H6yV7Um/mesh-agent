package com.alibaba.mesh.remoting;

import com.alibaba.mesh.remoting.transport.AbstractChannelHandler;

/**
 * ChannelHandlerAdapter.
 */
public class ChannelHandlerAdapter extends AbstractChannelHandler implements ChannelHandler {

    protected ChannelHandlerAdapter(ChannelHandler handler) {
        super(handler);
    }
}
