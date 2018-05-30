package com.alibaba.mesh.rpc.protocol.mesh;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.remoting.Codec4;
import com.alibaba.mesh.remoting.CodecOutputList;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.rpc.RpcInvocation;
import com.alibaba.mesh.rpc.RpcResult;

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
                // logMessageLength(obj, buffer.readerIndex() - save);
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

    private void logMessageLength(Object result, int bytes) {
        if (bytes <= 0) {
            return;
        }
        if (result instanceof Request) {
            try {
                ((RpcInvocation) ((Request) result).getData()).setAttachment(
                        Constants.INPUT_KEY, String.valueOf(bytes));
            } catch (Throwable e) {
                /* ignore */
            }
        } else if (result instanceof Response) {
            try {
                ((RpcResult) ((Response) result).getResult()).setAttachment(
                        Constants.OUTPUT_KEY, String.valueOf(bytes));
            } catch (Throwable e) {
                /* ignore */
            }
        }
    }

}
