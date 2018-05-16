/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
