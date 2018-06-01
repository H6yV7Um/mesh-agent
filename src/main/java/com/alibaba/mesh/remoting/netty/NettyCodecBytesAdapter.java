package com.alibaba.mesh.remoting.netty;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.Codeable;
import com.alibaba.mesh.remoting.Codec4;
import com.alibaba.mesh.remoting.CodecOutputList;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.util.internal.StringUtil;

import java.util.List;

/**
 * NettyCodecAdapter.
 */
final class NettyCodecBytesAdapter {

    private final ChannelHandler decoder = new InternalDecoder0();

    private final Codeable codec;

    private final URL url;

    public NettyCodecBytesAdapter(Codeable codec, URL url) {
        this.codec = codec;
        this.url = url;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }


    protected class InternalDecoder0 extends ChannelInboundHandlerAdapter {

        ByteBuf cumulation;
        private boolean singleDecode;
        private boolean decodeWasNull;

        /**
         * If {@code true} then only one message is decoded on each
         * {@link #channelRead(ChannelHandlerContext, Object)} call.
         * <p>
         * Default is {@code false} as this has performance impacts.
         */
        public boolean isSingleDecode() {
            return singleDecode;
        }

        /**
         * If set then only one message is decoded on each {@link #channelRead(ChannelHandlerContext, Object)}
         * call. This may be useful if you need to do some protocol upgrade and want to make sure nothing is mixed up.
         * <p>
         * Default is {@code false} as this has performance impacts.
         */
        public void setSingleDecode(boolean singleDecode) {
            this.singleDecode = singleDecode;
        }

        /**
         * Returns the actual number of readable bytes in the internal cumulative
         * buffer of this decoder. You usually do not need to rely on this value
         * to write a decoder. Use it only when you must use it at your own risk.
         * This method is a shortcut to {@link #internalBuffer() internalBuffer().readableBytes()}.
         */
        protected int actualReadableBytes() {
            return internalBuffer().readableBytes();
        }

        /**
         * Returns the internal cumulative buffer of this decoder. You usually
         * do not need to access the internal buffer directly to write a decoder.
         * Use it only when you must use it at your own risk.
         */
        protected ByteBuf internalBuffer() {
            if (cumulation != null) {
                return cumulation;
            } else {
                return Unpooled.EMPTY_BUFFER;
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            CodecOutputList out = CodecOutputList.newInstance();
            try {
                if (msg instanceof ByteBuf) {
                    ByteBuf data = (ByteBuf) msg;
                    if (cumulation == null) {
                        cumulation = data;
                        try {
                            callDecode(ctx, cumulation, out);
                        } finally {
                            if (cumulation != null && !cumulation.isReadable()) {
                                cumulation.release();
                                cumulation = null;
                            }
                        }
                    } else {
                        try {
                            if (cumulation.writerIndex() > cumulation.maxCapacity() - data.readableBytes()) {
                                ByteBuf oldCumulation = cumulation;
                                cumulation = ctx.alloc().buffer(oldCumulation.readableBytes() + data.readableBytes());
                                cumulation.writeBytes(oldCumulation);
                                oldCumulation.release();
                            }
                            cumulation.writeBytes(data);
                            callDecode(ctx, cumulation, out);
                        } finally {
                            if (cumulation != null) {
                                if (!cumulation.isReadable() && cumulation.refCnt() == 1) {
                                    cumulation.release();
                                    cumulation = null;
                                } else {
                                    if (cumulation.refCnt() == 1) {
                                        cumulation.discardSomeReadBytes();
                                    }
                                }
                            }
                            data.release();
                        }
                    }
                } else {
                    out.add(msg);
                }
            } catch (DecoderException e) {
                throw e;
            } catch (Throwable t) {
                throw new DecoderException(t);
            } finally {
                if (out.isEmpty()) {
                    decodeWasNull = true;
                }

                ctx.fireChannelRead(out);

                out.recycle();
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            if (decodeWasNull) {
                decodeWasNull = false;
                if (!ctx.channel().config().isAutoRead()) {
                    ctx.read();
                }
            }
            ctx.fireChannelReadComplete();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            CodecOutputList out = CodecOutputList.newInstance();
            try {
                if (cumulation != null) {
                    callDecode(ctx, cumulation, out);
                    decodeLast(ctx, cumulation, out);
                } else {
                    decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
                }
            } catch (DecoderException e) {
                throw e;
            } catch (Exception e) {
                throw new DecoderException(e);
            } finally {
                if (cumulation != null) {
                    cumulation.release();
                    cumulation = null;
                }

                if (out.size() > 0) {
                    ctx.fireChannelRead(out);
                    out.clear();
                }

                ctx.fireChannelInactive();
            }
        }

        @Override
        public final void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            ByteBuf buf = internalBuffer();
            int readable = buf.readableBytes();
            if (buf.isReadable()) {
                ByteBuf bytes = buf.readBytes(readable);
                buf.release();
                ctx.fireChannelRead(bytes);
            }
            cumulation = null;
            ctx.fireChannelReadComplete();
        }

        /**
         * Called once data should be decoded from the given {@link ByteBuf}. This method will call
         * {@link #decode(ChannelHandlerContext, ByteBuf, List)} as long as decoding should take place.
         *
         * @param ctx the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
         * @param in  the {@link ByteBuf} from which to read data
         * @param out the {@link List} to which decoded messages should be added
         */
        protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, CodecOutputList out) {
            try {
                while (in.isReadable()) {
                    int outSize = out.size();

                    if (outSize > 0) {
                        ctx.fireChannelRead(out);
                        out.clear();

                        // Check if this handler was removed before continuing with decoding.
                        // If it was removed, it is not safe to continue to operate on the buffer.
                        //
                        // See:
                        // - https://github.com/netty/netty/issues/4635
                        if (ctx.isRemoved()) {
                            break;
                        }
                        outSize = 0;
                    }

                    int oldInputLength = in.readableBytes();
                    decode(ctx, in, out);

                    // Check if this handler was removed before try to continue the loop.
                    // If it was removed it is not safe to continue to operate on the buffer
                    //
                    // See https://github.com/netty/netty/issues/1664
                    if (ctx.isRemoved()) {
                        break;
                    }

                    if (outSize == out.size()) {
                        if (oldInputLength == in.readableBytes()) {
                            break;
                        } else {
                            continue;
                        }
                    }

                    if (oldInputLength == in.readableBytes()) {
                        throw new DecoderException(
                                StringUtil.simpleClassName(getClass()) +
                                        ".decode() did not read anything but decoded a message.");
                    }

                    if (isSingleDecode()) {
                        break;
                    }
                }
            } catch (DecoderException e) {
                throw e;
            } catch (Throwable cause) {
                throw new DecoderException(cause);
            }
        }

        protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
            int savedReaderIndex = buf.readerIndex();
            Object message = codec.decodeBytes(ctx, buf);

            if (message == Codec4.DecodeResult.NEED_MORE_INPUT) {
                buf.readerIndex(savedReaderIndex);
                return;
            }

            out.add(message);
        }

        /**
         * Is called one last time when the {@link ChannelHandlerContext} goes in-active. Which means the
         * {@link #channelInactive(ChannelHandlerContext)} was triggered.
         * <p>
         * By default this will just call {@link #decode(ChannelHandlerContext, ByteBuf, List)} but sub-classes may
         * override this for some special cleanup operation.
         */
        protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            decode(ctx, in, out);
        }
    }

//    protected class InternalDecoder extends ByteToMessageDecoder {
//
//        @Override
//        protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
//            Object msg;
//            int saveReaderIndex;
//
//            do {
//                saveReaderIndex = buffer.readerIndex();
//                try {
//                    msg = codec.decodeBytes(ctx, buffer);
//                } catch (IOException e) {
//                    throw e;
//                }
//                if (msg == Codeable.DecodeResult.NEED_MORE_INPUT) {
//                    buffer.readerIndex(saveReaderIndex);
//                    buffer.discardSomeReadBytes();
//                    saveReaderIndex = buffer.readerIndex();
//                    break;
//                } else {
//                    if (msg != null) {
//                        out.add(msg);
//                    }
//                }
//            } while (buffer.readableBytes() > 0);
//        }
//    }
}
