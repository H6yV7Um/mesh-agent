//package com.alibaba.mesh.remoting.netty;
//
//import com.alibaba.mesh.remoting.CodecOutputList;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.handler.codec.ByteToMessageDecoder;
//import io.netty.handler.codec.DecoderException;
//
///**
// * @author yiji.github@hotmail.com
// */
//public abstract class AbstractMessageToByteDecoder extends ByteToMessageDecoder {
//
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        if (msg instanceof ByteBuf) {
//            CodecOutputList out = CodecOutputList.newInstance();
//            try {
//                ByteBuf data = (ByteBuf) msg;
//                first = cumulation == null;
//                if (first) {
//                    cumulation = data;
//                } else {
//                    cumulation = cumulator.cumulate(ctx.alloc(), cumulation, data);
//                }
//                callDecode(ctx, cumulation, out);
//            } catch (DecoderException e) {
//                throw e;
//            } catch (Exception e) {
//                throw new DecoderException(e);
//            } finally {
//                if (cumulation != null && !cumulation.isReadable()) {
//                    numReads = 0;
//                    cumulation.release();
//                    cumulation = null;
//                } else if (++ numReads >= discardAfterReads) {
//                    // We did enough reads already try to discard some bytes so we not risk to see a OOME.
//                    // See https://github.com/netty/netty/issues/4275
//                    numReads = 0;
//                    discardSomeReadBytes();
//                }
//
//                int size = out.size();
//                decodeWasNull = !out.insertSinceRecycled();
//                fireChannelRead(ctx, out, size);
//                out.recycle();
//            }
//        } else {
//            ctx.fireChannelRead(msg);
//        }
//    }
//
//}
