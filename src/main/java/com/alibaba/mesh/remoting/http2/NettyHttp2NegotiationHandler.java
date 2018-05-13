package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

/**
 * @author yiji
 */
public class NettyHttp2NegotiationHandler extends ApplicationProtocolNegotiationHandler {

    private URL url;

    protected NettyHttp2NegotiationHandler(URL url) {
        super(ApplicationProtocolNames.HTTP_1_1);
        this.url = url;
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {

        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
            ctx.pipeline().addLast(new NettyHttp2ServerHandlerBuilder().withURL(url).build());
            return;
        }

        if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
            ctx.pipeline().addLast(new HttpServerCodec(),
                    new HttpObjectAggregator(url.getParameter(Constants.MAX_HTTP_CONTENT_BYTES_KEY, Constants.MAX_HTTP_CONTENT_BYTES)),
                    new NettyHttp1ServerHandler());
            return;
        }

        throw new IllegalStateException("unknown protocol: " + protocol);
    }

}
