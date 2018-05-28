package com.alibaba.mesh.remoting;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.extension.Adaptive;
import com.alibaba.mesh.common.extension.SPI;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

@SPI
public interface Codec4 {

    @Adaptive({Constants.CODEC_KEY})
    void encode(ChannelHandlerContext ctx, ByteBuf buffer, Object message) throws IOException;

    @Adaptive({Constants.CODEC_KEY})
    Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws IOException;

    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_SOME_INPUT
    }

}

