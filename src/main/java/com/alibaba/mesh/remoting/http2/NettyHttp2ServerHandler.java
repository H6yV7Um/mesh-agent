package com.alibaba.mesh.remoting.http2;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.StringUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionDecoder;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2FrameAdapter;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yiji
 */
public class NettyHttp2ServerHandler extends AbstractHttp2CodecHandler {

    private static final Logger logger = LoggerFactory.getLogger(NettyHttp2ServerHandler.class);

    Http2Connection.PropertyKey streamKey;

    public NettyHttp2ServerHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder,
                                   Http2Settings initialSettings, URL url) {
        super(decoder, encoder, initialSettings, url);
        this.streamKey = encoder.connection().newKey();
        this.decoder().frameListener(new FrameListener());
    }

    private void onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream)
            throws Http2Exception {
        try {
            Payload payload = serverStream(requireHttp2Stream(streamId));
            payload.data(data.retain())
                    .endOfStream(endOfStream);
            ctx.fireChannelRead(payload);
        } catch (Throwable e) {
            logger.warn("Exception in onDataRead()", e);
            throw Http2Exception.streamError(
                    streamId, Http2Error.INTERNAL_ERROR, e, StringUtils.nullToEmpty(e.getMessage()));
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof Payload) {

            Payload payload = (Payload)msg;

            int streamId = payload.stream().id();
            Http2Stream stream = connection().stream(streamId);
            if (stream == null) {
                resetStream(ctx, streamId, Http2Error.CANCEL.code(), promise);
                return;
            }

            if(payload.endOfStream()){
                stream.removeProperty(streamKey);
            }

            encoder().writeHeaders(ctx, streamId, payload.headers(), 0, payload.endOfStream(), promise);
            encoder().writeData(ctx, payload.stream().id(), payload.data(), 0, payload.endOfStream(), promise);

        }else{
            super.write(ctx, msg, promise);
        }
    }

    private void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers) throws Http2Exception {
        try {
            CharSequence path = headers.path();
            if (path == null) {
                respondWithHttpError(ctx, streamId, 404, Status.Code.UNIMPLEMENTED,
                        "Request path is missing, expect patten '/service/method'");
                return;
            }

            if (path.charAt(0) != '/') {
                respondWithHttpError(ctx, streamId, 404, Status.Code.UNIMPLEMENTED,
                        String.format("Expected path to start with /: %s", path));
                return;
            }

            String method = path.subSequence(1, path.length()).toString();

            // Verify that the Content-Type is correct in the request.
            CharSequence contentType = headers.get(CONTENT_TYPE_HEADER);
            if (contentType == null) {
                respondWithHttpError(
                        ctx, streamId, 415, Status.Code.INTERNAL, "Content-Type is missing from the request");
                return;
            }

            String contentTypeString = contentType.toString();
            if (!isMeshContentType(contentTypeString)) {
                respondWithHttpError(ctx, streamId, 415, Status.Code.INTERNAL,
                        String.format("Content-Type '%s' is not supported", contentTypeString));
                return;
            }

            if (!HTTP_METHOD.equals(headers.method())) {
                respondWithHttpError(ctx, streamId, 405, Status.Code.INTERNAL,
                        String.format("Method '%s' is not supported", headers.method()));
                return;
            }

            Http2Stream http2Stream = requireHttp2Stream(streamId);
            http2Stream.setProperty(streamKey, new Payload(http2Stream, headers));
        } catch (Exception e) {
            logger.warn( "Unexpected onHeaderRead.", e);
            throw Http2Exception.streamError(
                    streamId, Http2Error.INTERNAL_ERROR, e, StringUtils.nullToEmpty(e.getMessage()));
        }
    }

    private void onRstStreamRead(int streamId, long errorCode) throws Http2Exception {
        Payload payload = serverStream(connection().stream(streamId));
        if (payload != null) {

        }
    }

    private Payload serverStream(Http2Stream stream) {
        return stream == null ? null : (Payload) stream.getProperty(streamKey);
    }

    private void respondWithHttpError(
            ChannelHandlerContext ctx, int streamId, int code, Status.Code statusCode, String msg) {

        Http2Headers headers = new DefaultHttp2Headers(true, 2)
                .status("" + code)
                .set(CONTENT_TYPE_HEADER, "text/plain; encoding=utf-8");

        headers.add(new AsciiString("status".getBytes(), false), new AsciiString(statusCode.name().getBytes(), false));
        headers.add(new AsciiString("message".getBytes(), false), new AsciiString(msg.getBytes(), false));

        encoder().writeHeaders(ctx, streamId, headers, 0, false, ctx.newPromise());
        ByteBuf msgBuf = ByteBufUtil.writeUtf8(ctx.alloc(), msg);
        encoder().writeData(ctx, streamId, msgBuf, 0, true, ctx.newPromise());
    }

    private class FrameListener extends Http2FrameAdapter {

        @Override
        public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
            NettyHttp2ServerHandler.this.onDataRead(ctx, streamId, data, padding, endOfStream);
            return data.readableBytes() + padding;
        }

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency,
                                  short weight, boolean exclusive, int padding, boolean endStream) throws Http2Exception {
            NettyHttp2ServerHandler.this.onHeadersRead(ctx, streamId, headers);
        }

        @Override
        public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream) throws Http2Exception {
            NettyHttp2ServerHandler.this.onHeadersRead(ctx, streamId, headers);
        }

        @Override
        public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode)
                throws Http2Exception {
            NettyHttp2ServerHandler.this.onRstStreamRead(streamId, errorCode);
        }
    }

}
