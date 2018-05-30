package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.Codec4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.List;

/**
 * NettyCodecAdapter.
 */
final class NettyCodecAdapter {

    private final ChannelHandler encoder = new InternalEncoder();

    private final ChannelHandler decoder = new InternalDecoder();

    private final Codec4 codec;

    private final URL url;

    private final ChannelHandler handler;

    public NettyCodecAdapter(Codec4 codec, URL url, ChannelHandler handler) {
        this.codec = codec;
        this.url = url;
        this.handler = handler;
    }

    public ChannelHandler getEncoder() {
        return encoder;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }

    protected class InternalEncoder extends AbstractMessageToByteEncoder {

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
            codec.encode(ctx, buffer, msg);
        }
    }

    protected class InternalDecoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {

            Object msg;
            int saveReaderIndex;

            do {
                saveReaderIndex = buffer.readerIndex();
                try {
                    msg = codec.decode(ctx, buffer);
                } catch (IOException e) {
                    throw e;
                }
                if (msg == Codec4.DecodeResult.NEED_MORE_INPUT) {
                    buffer.readerIndex(saveReaderIndex);
                    break;
                } else {
                    if (msg != null) {
                        out.add(msg);
                    }
                }
            } while (buffer.readableBytes() > 0);
        }
    }
}
