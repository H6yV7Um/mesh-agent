package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.Codeable;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.IOException;
import java.util.List;

/**
 * NettyCodecAdapter.
 */
final class NettyDecodebytesAdapter {

    private final ChannelHandler decoder = new InternalDecoder();

    private final Codeable codec;

    private final URL url;

    public NettyDecodebytesAdapter(Codeable codec, URL url) {
        this.codec = codec;
        this.url = url;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }

    protected class InternalDecoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {

            Object msg;
            int saveReaderIndex;

            do {
                saveReaderIndex = buffer.readerIndex();
                try {
                    msg = codec.decodeBytes(ctx, buffer);
                } catch (IOException e) {
                    throw e;
                }
                if (msg == Codeable.DecodeResult.NEED_MORE_INPUT) {
                    buffer.readerIndex(saveReaderIndex);
                    buffer.discardSomeReadBytes();
                    saveReaderIndex = buffer.readerIndex();
                    break;
                } else {
                    //is it possible to go here ?
                    if (saveReaderIndex == buffer.readerIndex()) {
                        throw new IOException("Decode without read data.");
                    }
                    if (msg != null) {
                        out.add(msg);
                    }
                }
            } while (buffer.readableBytes() > 0);
        }
    }
}
