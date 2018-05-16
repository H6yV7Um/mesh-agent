package com.alibaba.mesh.remoting.transport.codec.dubbo;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.io.Bytes;
import com.alibaba.mesh.common.serialize.ObjectInput;
import com.alibaba.mesh.common.serialize.ObjectOutput;
import com.alibaba.mesh.common.serialize.Serialization;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.Codeable;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.buffer.ChannelBufferInputStream;
import com.alibaba.mesh.remoting.buffer.ChannelBufferOutputStream;
import com.alibaba.mesh.remoting.exchange.DefaultFuture;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.transport.AbstractCodec;
import com.alibaba.mesh.remoting.transport.CodecSupport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ByteProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yiji
 */
public abstract class DubboExchangeCodec extends AbstractCodec implements Codeable {

    // header length.
    protected static final int HEADER_LENGTH = 16;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    protected static final byte MAGIC_HIGH = Bytes.short2bytes(MAGIC)[0];
    protected static final byte MAGIC_LOW = Bytes.short2bytes(MAGIC)[1];
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;
    protected static final int SERIALIZATION_MASK = 0x1f;
    private static final Logger logger = LoggerFactory.getLogger(DubboExchangeCodec.class);

    public Short getMagicCode() {
        return MAGIC;
    }

    public void encode(ChannelHandlerContext ctx, ByteBuf buffer, Object msg) throws IOException {
        if (msg instanceof Request) {
            encodeRequest(ctx, buffer, (Request) msg);
        } else if (msg instanceof Response) {
            encodeResponse(ctx, buffer, (Response) msg);
        }
    }

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

            if (i > 0) {
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
        int len = header.getInt(12);
        // checkPayload(url, channel, len);

        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        ChannelBufferInputStream is = new ChannelBufferInputStream(buffer, len);

        try {
            return decodeBody(ctx, url, is, header);
        } finally {
            if (is.available() > 0) {
                try {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Skip input stream " + is.available());
                    }
                    is.skip(is.available());
                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
    }

    protected Object decodeBody(ChannelHandlerContext ctx, URL url, InputStream is, ByteBuf header) throws IOException {
        byte flag = header.getByte(2), proto = (byte) (flag & SERIALIZATION_MASK);
        Serialization s = CodecSupport.getSerialization(url, proto);
        ObjectInput in = s.deserialize(url, is);
        // get request id.
        long id = header.getLong(4);
        if ((flag & FLAG_REQUEST) == 0) {
            // decode response.
            Response res = new Response(id);
            if ((flag & FLAG_EVENT) != 0) {
                res.setEvent(Response.HEARTBEAT_EVENT);
            }
            // get status.
            byte status = header.getByte(3);
            res.setStatus(status);
            if (status == Response.OK) {
                try {
                    Object data;
                    if (res.isHeartbeat()) {
                        data = decodeHeartbeatData(ctx, in);
                    } else if (res.isEvent()) {
                        data = decodeEventData(ctx, in);
                    } else {
                        data = decodeResponseData(ctx, in, getRequestData(id));
                    }
                    res.setResult(data);
                } catch (Throwable t) {
                    res.setStatus(Response.CLIENT_ERROR);
                    res.setErrorMessage(StringUtils.toString(t));
                }
            } else {
                res.setErrorMessage(in.readUTF());
            }
            return res;
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
                if (req.isHeartbeat()) {
                    data = decodeHeartbeatData(ctx, in);
                } else if (req.isEvent()) {
                    data = decodeEventData(ctx, in);
                } else {
                    data = decodeRequestData(ctx, in);
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

        URL url = ctx.channel().attr(Keys.URL_KEY).get();

        Serialization serialization = getSerialization(url);
        // header.
        ByteBuf header = ctx.alloc().buffer(HEADER_LENGTH);
        // set magic number.
        header.writeShort(MAGIC);

        // set request and serialization flag.
        // slot [2]
        header.writeByte(FLAG_REQUEST | serialization.getContentTypeId());

        if (req.isTwoWay()) header.setByte(2, header.getByte(2) | FLAG_TWOWAY);
        if (req.isEvent()) header.setByte(2, header.getByte(2) | FLAG_EVENT);

        // set request id.
        // Bytes.long2bytes(req.getId(), header, 4);
        header.writerIndex(4);
        header.writeLong(req.getId());

        // encode request data.
        int savedWriteIndex = buffer.writerIndex();
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
        ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
        ObjectOutput out = serialization.serialize(url, bos);
        if (req.isEvent()) {
            encodeEventData(ctx, out, req.getData());
        } else {
            encodeRequestData(ctx, out, req.getData());
        }
        out.flushBuffer();
        bos.flush();
        bos.close();
        int len = bos.writtenBytes();
        // checkPayload(url, ctx.channel(), len);
        //Bytes.int2bytes(len, header, 12);
        // slot [12, 13, 14, 15]
        header.writeInt(len);

        // write
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);

        header.release();
    }

    protected void encodeResponse(ChannelHandlerContext ctx, ByteBuf buffer, Response response) throws IOException {
        int savedWriteIndex = buffer.writerIndex();
        URL url = ctx.channel().attr(Keys.URL_KEY).get();
        try {
            Serialization serialization = getSerialization(url);
            // header.
            ByteBuf header = ctx.alloc().buffer(HEADER_LENGTH);
            // set magic number.
            header.writeShort(MAGIC);
            // set request and serialization flag.
            header.writeByte(serialization.getContentTypeId());
            if (response.isHeartbeat()) header.setByte(2, header.getByte(2) | FLAG_EVENT);
            // set response status.
            byte status = response.getStatus();
            // slot [3]
            header.writeByte(status);
            // set request id.
            header.writeLong(response.getId());

            buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
            ChannelBufferOutputStream bos = new ChannelBufferOutputStream(buffer);
            ObjectOutput out = serialization.serialize(url, bos);
            // encode response data or error message.
            if (status == Response.OK) {
                if (response.isHeartbeat()) {
                    encodeHeartbeatData(ctx, out, response.getResult());
                } else {
                    encodeResponseData(ctx, out, response.getResult());
                }
            } else out.writeUTF(response.getErrorMessage());
            out.flushBuffer();
            bos.flush();
            bos.close();

            int len = bos.writtenBytes();
            // checkPayload(url, ctx.channel(), len);
            // slot [12, 13, 14, 15]
            header.writeInt(len);
            // write
            buffer.writerIndex(savedWriteIndex);
            buffer.writeBytes(header); // write header.
            buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);

            header.release();
        } catch (Throwable t) {
            // clear buffer
            buffer.writerIndex(savedWriteIndex);
            // send error message to MeshConsumer, otherwise, MeshConsumer will wait till timeout.
            if (!response.isEvent() && response.getStatus() != Response.BAD_RESPONSE) {
//                Response r = new Response(response.getId(), response.getVersion());
//                r.setStatus(Response.BAD_RESPONSE);

                // FIXME log error message in Codec and handle in caught() of IoHanndler?
                logger.warn("Fail to encode response: " + response + ", send bad_response info instead, cause: " + t.getMessage(), t);
//                try {
//                    r.setErrorMessage("Failed to send response: " + res + ", cause: " + StringUtils.toString(t));
//                    ctx.send(r);
//                    return;
//                } catch (RemotingException e) {
//                    logger.warn("Failed to send bad_response info back: " + res + ", cause: " + e.getMessage(), e);
//                }
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

    protected Object decodeData(ObjectInput in) throws IOException {
        return decodeRequestData(in);
    }

    @Deprecated
    protected Object decodeHeartbeatData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeRequestData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeResponseData(ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected void encodeData(ObjectOutput out, Object data) throws IOException {
        encodeRequestData(out, data);
    }

    private void encodeEventData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    @Deprecated
    protected void encodeHeartbeatData(ObjectOutput out, Object data) throws IOException {
        encodeEventData(out, data);
    }

    protected void encodeRequestData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    protected void encodeResponseData(ObjectOutput out, Object data) throws IOException {
        out.writeObject(data);
    }

    protected Object decodeData(ChannelHandlerContext ctx, ObjectInput in) throws IOException {
        return decodeRequestData(ctx, in);
    }

    protected Object decodeEventData(ChannelHandlerContext ctx, ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    @Deprecated
    protected Object decodeHeartbeatData(ChannelHandlerContext ctx, ObjectInput in) throws IOException {
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read object failed.", e));
        }
    }

    protected Object decodeRequestData(ChannelHandlerContext ctx, ObjectInput in) throws IOException {
        return decodeRequestData(in);
    }

    protected Object decodeResponseData(ChannelHandlerContext ctx, ObjectInput in) throws IOException {
        return decodeResponseData(in);
    }

    protected Object decodeResponseData(ChannelHandlerContext ctx, ObjectInput in, Object requestData) throws IOException {
        return decodeResponseData(ctx, in);
    }

    protected void encodeData(ChannelHandlerContext ctx, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(ctx, out, data);
    }

    private void encodeEventData(ChannelHandlerContext ctx, ObjectOutput out, Object data) throws IOException {
        encodeEventData(out, data);
    }

    @Deprecated
    protected void encodeHeartbeatData(ChannelHandlerContext ctx, ObjectOutput out, Object data) throws IOException {
        encodeHeartbeatData(out, data);
    }

    protected void encodeRequestData(ChannelHandlerContext ctx, ObjectOutput out, Object data) throws IOException {
        encodeRequestData(out, data);
    }

    protected void encodeResponseData(ChannelHandlerContext ctx, ObjectOutput out, Object data) throws IOException {
        encodeResponseData(out, data);
    }

    protected Object decode0(ChannelHandlerContext ctx, ByteBuf buffer, int readable, ByteBuf header) throws IOException {
        throw new IllegalArgumentException("failed to decode from channel " + ctx.channel()
                + ", sub codec may be override decode0"
                + " method to support this message type.");
    }
}
