package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.common.URL;

import io.netty.handler.codec.http2.AbstractHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.Http2Settings;

import static io.netty.handler.logging.LogLevel.INFO;

/**
 * @author yiji
 */
public class NettyHttp2ServerHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<NettyHttp2ServerHandler, NettyHttp2ServerHandlerBuilder>  {

    private static final Http2FrameLogger logger = new Http2FrameLogger(INFO, NettyHttp2ServerHandler.class);

    private URL url;

    public NettyHttp2ServerHandlerBuilder() {
        frameLogger(logger);
    }

    @Override
    public NettyHttp2ServerHandler build() {
        return super.build();
    }

    @Override
    protected NettyHttp2ServerHandler build(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                           Http2Settings initialSettings) {
        NettyHttp2ServerHandler handler = new NettyHttp2ServerHandler(decoder, encoder, initialSettings, url);
        return handler;
    }

    public NettyHttp2ServerHandlerBuilder withURL(URL url){
        this.url = url;
        return this;
    }

}
