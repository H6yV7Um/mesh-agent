package com.alibaba.mesh.remoting;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;

/**
 * @author yiji
 */
public interface Codeable extends Codec4 {

    byte getCodecTypeId();

    /**
     *
     * 获取解码的requestId，应该在fullDecode之后调用
     *
     */
    long getRequestId(ByteBuf buffer);

    /**
     * 读取可以完整解码的字节报文
     */
    Object decodeBytes(ChannelHandlerContext ctx, ByteBuf buffer) throws IOException;

}
