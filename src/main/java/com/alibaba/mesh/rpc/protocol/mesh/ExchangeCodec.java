package com.alibaba.mesh.rpc.protocol.mesh;

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
import com.alibaba.mesh.remoting.http2.NettyHttp1ServerHandler;
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
import java.nio.charset.Charset;

/**
 * ExchangeCodec.
 *
 *
 *
 */
public abstract class ExchangeCodec extends AbstractCodec {

    // header length.
    protected static final int HEADER_LENGTH = 21;

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
        // maybe call retain() ??
        ByteBuf header = buffer.slice(buffer.readerIndex(), received);
        // set index to message body
        buffer.readerIndex(buffer.readerIndex() + received);
        return decode(ctx, buffer, readable, header);
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer, int readable, ByteBuf header) throws IOException {

        // check magic number.
        if (readable > 0 && header.getByte(0) != MAGIC_HIGH
                || readable > 1 && header.getByte(1) != MAGIC_LOW) {
            int length = header.readableBytes(), received = readable <= HEADER_LENGTH ? readable : HEADER_LENGTH;
            if (length < readable) {
                header = buffer.slice(buffer.readerIndex() - received, readable);
            }

            int i = header.forEachByte(1, header.readableBytes() - 1, new ByteProcessor() {
                byte prev;

                @Override
                public boolean process(byte value) throws Exception {
                    if (prev == MAGIC_HIGH && value == MAGIC_LOW) return false;
                    prev = value;
                    return true;
                }
            });

            if(i > 0) {
                // set index to message head
                buffer.readerIndex(buffer.readerIndex() - received + i);
                header = buffer.slice(buffer.readerIndex(), i);
            }

            return DecodeResult.NEED_MORE_INPUT;
        }
        // check length.
        if (readable < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        Channel channel = ctx.channel();

        URL url = channel.attr(Keys.URL_KEY).get();

        // get data length.
        int len = header.getInt(17);
        // checkPayload(url, channel, len);

        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        Object data = decodeBody(ctx, url, buffer, header);

        if(data == DecodeResult.NEED_MORE_INPUT) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        return data;
    }

    protected Object decodeBody(ChannelHandlerContext ctx, URL url, ByteBuf buffer, ByteBuf header) throws IOException {

        // set request and serialization flag.
        // slot [2]
        byte flag = header.getByte(2),
        // set codec type, eg: mesh
        // slot[3]
             codec = header.getByte(3);

        Codeable codeable = CodecSupport.getCodeable(url);// .getCodeableById(codec);
        if(codeable == null) {
            throw new UnsupportedEncodingException("Not found extension " + url.getParameter(Constants.CODEABLE_KEY, Constants.DEFAULT_REMOTING_CODEC));
        }

        // get request id.
        // slot [9, 10, 11, 12, 13, 14, 15, 16]
        long id = header.getLong(9);
        // get data length
        // slot [17]
        int  len = header.getInt(17);
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

                boolean isClientSide = isClientSide(url, ctx.channel());

                int weight = header.getInt(4);
                if(isClientSide){
                    // set current channel weight
                    ctx.channel().attr(Keys.WEIGHT_KEY).set(weight);
                }

                try {
                    Object data;
                    if (response.isEvent()) {
                        data = decodeEventData(ctx, buffer);
                    } else {
                        if(isClientSide){
                            data = codeable.decode(ctx, buffer);
                            System.out.println("client receive:" + data);
//                            if(data == DecodeResult.NEED_MORE_INPUT){
//                                return DecodeResult.NEED_MORE_INPUT;
//                            }
                        }else {
                            // mesh server
                            // never happen if close heartbeat
                            Object payload = DecodeResult.NEED_MORE_INPUT;
                            while ((payload = codeable.decodeBytes(ctx, buffer)) != DecodeResult.NEED_MORE_INPUT){
                                response.setRemoteId(codeable.getRequestId((ByteBuf) payload));
                            }
                            data = payload;
                        }
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
            Request request = new Request(id);
            request.setVersion("2.0.0");
            request.setTwoWay((flag & FLAG_TWOWAY) != 0);
            if ((flag & FLAG_EVENT) != 0) {
                request.setEvent(Request.HEARTBEAT_EVENT);
            }
            try {
                Object data;
                if (request.isEvent()) {
                    data = decodeEventData(ctx, buffer);
                } else {
                    // only copy client data
                    ByteBuf payload = buffer.slice(buffer.readerIndex(), len).retain();
                    // int savedReaderIndex = payload.readerIndex();
                    request.setRemoteId(codeable.getRequestId(payload));
                    data = payload;
                    // TODO 打印
                    // System.out.println("provider:" + NettyHttp1ServerHandler.decodeString(payload, payload.readerIndex(), payload.readableBytes(), Charset.forName("utf-8")));
                    // eat resolved payload
                    buffer.readerIndex(buffer.readerIndex() + payload.readableBytes());
                }
                request.setData(data);
            } catch (Throwable t) {
                // bad request
                request.setBroken(true);
                request.setData(t);
            }
            return request;
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
        // slot [8] (header.writerIndex() + 1)
        header.writerIndex(9);

        // set request id.
        // slot [9, 10, 11, 12, 13, 14, 15, 16]
        header.writeLong(req.getId());

        // encode request data.
        int savedWriteIndex = buffer.writerIndex(),
            readyWriteIndex = savedWriteIndex + HEADER_LENGTH;
        buffer.writerIndex(readyWriteIndex);

        if (req.isEvent()) {
            encodeEventData(ctx, buffer, req.getData());
        } else {
            // consumer side request contains same payload
            codeable.encode(ctx, buffer, req);
        }

        int len = buffer.writerIndex() - readyWriteIndex;
        // checkPayload(url, channel, len);
        // slot [17, 18, 19 , 20]
        header.writeInt(len);

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

            int weight = 100;
            if(!isClientSide(url, channel)){
                // weight = ...
            }

            // todo set threadpool, cpu, memory, network
            // how to get performance parameter
            header.writeInt(weight);

            // set response status.
            // slot [8]
            byte status = response.getStatus();
            header.writeByte(status);

            // set request id.
            // slot [9, 10, 11, 12, 13, 14, 15, 16]
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
            // checkPayload(url, channel, len);
            // slot [17, 18, 19 , 20]
            header.writeInt(len);
            // write
            buffer.writerIndex(savedWriteIndex);
            buffer.writeBytes(header); // write header.
            buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
            // release resource
            header.release();
        } catch (Throwable t) {
            // clear buffer
            buffer.writerIndex(savedWriteIndex);
            // send error message to MeshConsumer, otherwise, MeshConsumer will wait till timeout.
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
