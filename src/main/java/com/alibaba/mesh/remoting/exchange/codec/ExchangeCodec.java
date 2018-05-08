package com.alibaba.mesh.remoting.exchange.codec;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.io.Bytes;
import com.alibaba.mesh.common.serialize.Serialization;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.Codeable;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.exchange.DefaultFuture;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.transport.AbstractCodec;
import com.alibaba.mesh.remoting.transport.CodecSupport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ByteProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * ExchangeCodec.
 *
 *
 *
 */
public abstract class ExchangeCodec extends AbstractCodec {

    // header length.
    protected static final int HEADER_LENGTH = 24;

    // magic header.
    protected static final short MAGIC = (short) 0xdaba;
    protected static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
    protected static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;
    protected static final int SERIALIZATION_MASK = 0x1f;
    private static final Logger logger = LoggerFactory.getLogger(ExchangeCodec.class);

    public Short getMagicCode() {
        return MAGIC;
    }

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf buffer, Object msg) throws IOException {
        if (msg instanceof Request) {
            encodeRequest(ctx, buffer, (Request) msg);
        } else if (msg instanceof Response) {
            encodeResponse(ctx, buffer, (Response) msg);
        }
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws IOException {
        int readable = buffer.readableBytes(),
        received = readable <= HEADER_LENGTH ? readable : HEADER_LENGTH;
        // set index to message body
        buffer.readerIndex(buffer.readerIndex() + received);
        // maybe call retain() ??
        ByteBuf header = buffer.slice(buffer.readerIndex(), received);
        return decode(ctx, buffer, readable, header);
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer, int readable, ByteBuf header) throws IOException {

        // check magic number.
        if (readable > 0 && header.getByte(0) != MAGIC_HIGH
                || readable > 1 && header.getByte(1) != MAGIC_LOW) {
            int length = header.readableBytes();
            if (length < readable) {
                header = buffer.slice(buffer.readerIndex(), readable);
            }

            int i = header.forEachByte(1, header.readableBytes() - 1, new ByteProcessor() {
                        Byte prev = null;
                        @Override
                        public boolean process(byte value) throws Exception {
                            if(prev == MAGIC_HIGH && value == MAGIC_LOW) return false;
                            prev = value;
                            return true;
                        }
                    });

            if(i != -1) {
                // set index to message head
                buffer.readerIndex(buffer.readerIndex() - header.readableBytes() + i - 1);
                header = buffer.slice(buffer.readerIndex(), i - 1);
            }

            return decode0(ctx, buffer, readable, header);
        }
        // check length.
        if (readable < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        Channel channel = ctx.channel();

        URL url = channel.attr(Keys.URL_KEY).get();

        // get data length.
        int len = buffer.getInt(20);
        checkPayload(url, channel, len);

        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        try {
            return decodeBody(ctx, url, buffer, header);
        } finally {
            int skipBytes = buffer.readableBytes();
            if (skipBytes > 0) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Skip input stream " + buffer);
                }
                buffer.readerIndex(buffer.readerIndex() + skipBytes);
            }
        }
    }

    protected Object decodeBody(ChannelHandlerContext ctx, URL url, ByteBuf buffer, ByteBuf header) throws IOException {

        // set request and serialization flag.
        // slot [2]
        byte flag = header.getByte(2),
        // set codec type, eg: mesh
        // slot[3]
             codec = header.getByte(3);

        Codeable codeable = CodecSupport.getCodeableById(codec);
        if(codeable == null) {
            throw new UnsupportedEncodingException("Not found extension '" + url.getParameter(Constants.CODEC_KEY, Constants.DEFAULT_REMOTING_CODEC));
        }

        // get request id.
        // slot[12, 13, 14, 15, 16, 17, 18, 19]
        long id = header.getLong(12);
        // get data length
        int  len = header.getInt(20);
        if ((flag & FLAG_REQUEST) == 0) {
            // decode response.
            Response response = new Response(id);
            if ((flag & FLAG_EVENT) != 0) {
                response.setEvent(Response.HEARTBEAT_EVENT);
            }
            // get status.
            // slot[8, 9, 10, 11]
            byte status = header.getByte(8);
            response.setStatus(status);
            if (status == Response.OK) {

                int weight = header.getInt(4);
                if(isClientSide(url, ctx.channel())){
                    // set current channel weight
                    ctx.channel().attr(Keys.WEIGHT_KEY).set(weight);
                }

                try {
                    Object data;
                    if (response.isEvent()) {
                        data = decodeEventData(ctx, buffer);
                    } else {
                        data = codeable.decode(ctx, buffer);
                    }
                    response.setResult(data);
                } catch (Throwable t) {
                    response.setStatus(Response.CLIENT_ERROR);
                    response.setErrorMessage(StringUtils.toString(t));
                }
            } else {
                // todo 待处理
                // response.setErrorMessage();
            }
            return response;
        } else {
            // decode request.
            Request req = new Request(id);
            req.setVersion("2.0.0");
            req.setTwoWay((flag & FLAG_TWOWAY) != 0);
            if ((flag & FLAG_EVENT) != 0) {
                req.setEvent(Request.HEARTBEAT_EVENT);
            }
            try {
                Object data;
                if (req.isEvent()) {
                    data = decodeEventData(ctx, buffer);
                } else {
                    // only copy client data
                    data = buffer.slice(buffer.readerIndex(), len);
                }
                req.setData(data);
            } catch (Throwable t) {
                // bad request
                req.setBroken(true);
                req.setData(t);
            }
            return req;
        }
    }

    protected Object getRequestData(long id) {
        DefaultFuture future = DefaultFuture.getFuture(id);
        if (future == null)
            return null;
        Request req = future.getRequest();
        if (req == null)
            return null;
        return req.getData();
    }

    protected void encodeRequest(ChannelHandlerContext ctx, ByteBuf buffer, Request req) throws IOException {

        Channel channel = ctx.channel();

        URL url = channel.attr(Keys.URL_KEY).get();

        Serialization serialization = CodecSupport.getSerialization(url);
        // header.
        ByteBuf header = ctx.alloc().buffer(HEADER_LENGTH);
        // set magic number.
        // slot [0, 1]
        header.writeShort(MAGIC);

        // set request and serialization flag.
        // slot [2]
        header.writeByte(FLAG_REQUEST | serialization.getContentTypeId());

        // update slot [2]
        if (req.isTwoWay()) header.setByte(2, header.getByte(2) | FLAG_TWOWAY);
        if (req.isEvent())  header.setByte(2, header.getByte(2) | FLAG_EVENT);

        Codeable codeable = CodecSupport.getCodeable(url);

        if(codeable == null) {
            throw new UnsupportedEncodingException("Not found extension '" + url.getParameter(Constants.CODEC_KEY, Constants.DEFAULT_REMOTING_CODEC));
        }

        // set codec type, eg: mesh
        // slot [3]
        header.writeByte(codeable.getCodecTypeId());

        // set timeout
        // slot [4, 5, 6, 7]
        header.writeInt(url.getParameter(Constants.TIMEOUT_KEY, 0));

        // skip status 4 byte
        // slot [8, 9, 10, 11] (header.writerIndex() + 4)
        header.writerIndex(12);

        // set request id.
        // slot [12, 13, 14, 15, 16, 17, 18, 19]
        header.writeLong(req.getId());

        // encode request data.
        int savedWriteIndex = buffer.writerIndex(),
            readyWriteIndex = savedWriteIndex + HEADER_LENGTH;
        buffer.writerIndex(readyWriteIndex);

        if (req.isEvent()) {
            encodeEventData(ctx, buffer, req.getData());
        } else {
            codeable.encode(ctx, buffer, req.getData());
        }

        int len = buffer.writerIndex() - readyWriteIndex;
        checkPayload(url, channel, len);
        header.setInt(20, len);
        // write
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
        // release resource
        header.release();
    }

    protected void encodeResponse(ChannelHandlerContext ctx, ByteBuf buffer, Response response) throws IOException {
        int savedWriteIndex = buffer.writerIndex();
        try {

            Channel channel = ctx.channel();
            URL url = channel.attr(Keys.URL_KEY).get();

            // header.
            ByteBuf header = ctx.alloc().buffer(HEADER_LENGTH);
            // set magic number.
            // slot [0, 1]
            header.writeShort(MAGIC);
            // set request.
            // slot [2]
            if (response.isHeartbeat()) header.writeByte(FLAG_EVENT | header.getByte(2));

            // skip codec type, eg: mesh
            // slot [3]
            header.writerIndex(4);

            int weight = 0;
            if(!isClientSide(url, channel)){
                // weight = ...
            }

            // todo set threadpool, cpu, memory, network
            // how to get performance parameter
            header.writeInt(weight);

            // set response status.
            // slot [8, 9, 10, 11]
            byte status = response.getStatus();
            header.writeInt(status);

            // set request id.
            // slot [12, 13, 14, 15, 16, 17, 18, 19]
            header.writeLong(response.getId());

            int readyWriteIndex = savedWriteIndex + HEADER_LENGTH;
            buffer.writerIndex(readyWriteIndex);

            // encode response data or error message.
            if (status == Response.OK) {
                if (response.isEvent()) {
                    encodeEventData(ctx, buffer, response.getResult());
                } else {
                    // set received bytes into current buffer
                    buffer.writeBytes((ByteBuf)response.getResult());
                }
            } else {
                // todo 待处理
                // out.writeUTF(response.getErrorMessage());
            }

            int len = buffer.writerIndex() - readyWriteIndex;
            checkPayload(url, channel, len);
            header.setInt(20, len);
            // write
            buffer.writerIndex(savedWriteIndex);
            buffer.writeBytes(header); // write header.
            buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
            // release resource
            header.release();
        } catch (Throwable t) {
            // clear buffer
            buffer.writerIndex(savedWriteIndex);
            // send error message to Consumer, otherwise, Consumer will wait till timeout.
            if (!response.isEvent() && response.getStatus() != Response.BAD_RESPONSE) {
                Response r = new Response(response.getId(), response.getVersion());
                r.setStatus(Response.BAD_RESPONSE);
                logger.warn("Fail to encode response: " + response + ", send bad_response info instead, cause: " + t.getMessage(), t);
                ctx.writeAndFlush(r).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            logger.warn("Failed to send bad_response info back: " + t.getMessage(), future.cause());
                        }
                    }
                });
                return;
            }

            // Rethrow exception
            if (t instanceof IOException) {
                throw (IOException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new RuntimeException(t.getMessage(), t);
            }
        }
    }

    protected Object decodeEventData(ChannelHandlerContext ctx, ByteBuf buffer) throws IOException {
        return buffer.readChar();
    }

    protected void encodeEventData(ChannelHandlerContext ctx, ByteBuf buffer, Object data) throws IOException {
        buffer.writeChar('H');
    }

    protected Object decode0(ChannelHandlerContext ctx, ByteBuf buffer, int readable, ByteBuf header) throws IOException{
        throw new IllegalArgumentException("failed to decode from channel " + ctx.channel()
                + ", sub codec may be override decode0"
                + " method to support this message type.");
    }

}
