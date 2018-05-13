package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.common.URL;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.AsciiString;

import static io.netty.handler.codec.http2.Http2CodecUtil.getEmbeddedHttp2Exception;

/**
 * @author yiji
 */
public abstract class AbstractHttp2CodecHandler extends Http2ConnectionHandler {

    public static final AsciiString STATUS_OK = AsciiString.of("200");
    public static final AsciiString HTTP_METHOD = AsciiString.of("POST");
    public static final AsciiString HTTP_GET_METHOD = AsciiString.of("GET");
    public static final AsciiString HTTPS = AsciiString.of("https");
    public static final AsciiString HTTP = AsciiString.of("http");
    public static final AsciiString CONTENT_TYPE_HEADER = AsciiString.of("content-type");
    public static final AsciiString CONTENT_TYPE_DUBBO_HTTP2 = AsciiString.of("application/mesh-http2");
    public static final AsciiString TE_HEADER = AsciiString.of("te");
    public static final AsciiString TE_TRAILERS = AsciiString.of("trailers");
    public static final AsciiString USER_AGENT = AsciiString.of("user-agent");

    protected URL url;
    private int initialConnectionWindow;
    private ChannelHandlerContext ctx;

    public AbstractHttp2CodecHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                     Http2Settings initialSettings, URL url) {
        super(decoder, encoder, initialSettings);
        this.url = url;
        this.initialConnectionWindow = initialSettings.initialWindowSize() == null ? -1 :
                initialSettings.initialWindowSize();
    }

    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        // Sends the connection preface if we haven't already.
        super.handlerAdded(ctx);
        sendInitialConnectionWindow();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Sends connection preface if we haven't already.
        super.channelActive(ctx);
        sendInitialConnectionWindow();
    }

    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Http2Exception embedded = getEmbeddedHttp2Exception(cause);
        if (embedded == null) {
            // There was no embedded Http2Exception, assume it's a connection error. Subclasses are
            // responsible for storing the appropriate status and shutting down the connection.
            onError(ctx, false, cause);
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }

    protected final ChannelHandlerContext ctx() {
        return ctx;
    }

    /**
     * Sends initial connection window to the remote endpoint if necessary.
     */
    private void sendInitialConnectionWindow() throws Http2Exception {
        if (ctx.channel().isActive() && initialConnectionWindow > 0) {
            Http2Stream connectionStream = connection().connectionStream();
            int currentSize = connection().local().flowController().windowSize(connectionStream);
            int delta = initialConnectionWindow - currentSize;
            decoder().flowController().incrementWindowSize(connectionStream, delta);
            initialConnectionWindow = -1;
            ctx.flush();
        }
    }

    protected Http2Stream requireHttp2Stream(int streamId) {
        Http2Stream stream = connection().stream(streamId);
        if (stream == null) {
            // This should never happen.
            throw new AssertionError("Stream does not exist: " + streamId);
        }
        return stream;
    }

    protected boolean isMeshContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        contentType = contentType.toLowerCase();
        // if (!contentType.startsWith(CONTENT_TYPE_DUBBO_HTTP2.toString())) {
        //    return false;
        // }

        if (contentType.length() == CONTENT_TYPE_DUBBO_HTTP2.length()) {
            return true;
        }

        char nextChar = contentType.charAt(CONTENT_TYPE_DUBBO_HTTP2.length());
        return nextChar == '+' || nextChar == ';';
    }

}
