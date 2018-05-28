package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.Endpoint;

/**
 * AbstractPeer
 */
public abstract class AbstractPeer extends AbstractChannelHandler implements Endpoint, ChannelHandler {

    protected volatile URL url;

    // closing closed means the process is being closed and close is finished
    private volatile boolean closing;

    private volatile boolean closed;

    public AbstractPeer(URL url, ChannelHandler handler) {
        super(handler);
        this.url = url;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public void close(int timeout) {
        close();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return handler;
    }

    @Override
    public void startClose() {
        if (isClosed()) {
            return;
        }
        closing = true;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        this.url = url;
    }

    /**
     * Return the final handler (which may have been wrapped). This method should be distinguished with getChannelHandler() method
     *
     * @return ChannelHandler
     */
    public ChannelHandler getDelegateHandler() {
        return handler;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public boolean isClosing() {
        return closing && !closed;
    }

}
