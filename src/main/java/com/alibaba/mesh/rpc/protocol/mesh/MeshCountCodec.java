package com.alibaba.mesh.rpc.protocol.mesh;

import com.alibaba.mesh.remoting.Codec4;
import com.alibaba.mesh.remoting.CodecOutputList;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

public final class MeshCountCodec implements Codec4 {

    private MeshCodec codec = new MeshCodec();

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf buffer, Object msg) throws IOException {
        codec.encode(ctx, buffer, msg);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws IOException {
        int save = buffer.readerIndex();
        CodecOutputList out = CodecOutputList.newInstance();
        do {
            Object obj = codec.decode(ctx, buffer);
            if (DecodeResult.NEED_MORE_INPUT == obj) {
                buffer.readerIndex(save);
                buffer.discardSomeReadBytes();
                save = buffer.readerIndex();
                break;
            } else {
                out.add(obj);
                save = buffer.readerIndex();
            }
        } while (true);
        if (out.isEmpty()) {
            return DecodeResult.NEED_MORE_INPUT;
        }
        if (out.size() == 1) {
            return out.get(0);
        }
        return out;
    }

}
