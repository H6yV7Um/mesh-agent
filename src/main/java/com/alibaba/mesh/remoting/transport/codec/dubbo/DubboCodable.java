package com.alibaba.mesh.remoting.transport.codec.dubbo;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.Version;
import com.alibaba.mesh.common.io.UnsafeByteArrayInputStream;
import com.alibaba.mesh.common.serialize.ObjectInput;
import com.alibaba.mesh.common.serialize.ObjectOutput;
import com.alibaba.mesh.common.serialize.Serialization;
import com.alibaba.mesh.common.utils.RpcUtils;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.Codeable;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.http2.NettyHttp1ServerHandler;
import com.alibaba.mesh.remoting.transport.CodecSupport;
import com.alibaba.mesh.rpc.Invocation;
import com.alibaba.mesh.rpc.Result;
import com.alibaba.mesh.rpc.RpcInvocation;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ByteProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author yiji
 */
public class DubboCodable extends DubboExchangeCodec implements Codeable {

    public static final String NAME = "dubbo";
    public static final String DUBBO_VERSION = Version.getVersion(DubboCodable.class, Version.getVersion());
    public static final byte RESPONSE_WITH_EXCEPTION = 0;
    public static final byte RESPONSE_VALUE = 1;
    public static final byte RESPONSE_NULL_VALUE = 2;
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final Logger log = LoggerFactory.getLogger(DubboCodable.class);

    @Override
    protected Object decodeBody(ChannelHandlerContext ctx, URL url, InputStream is, ByteBuf header) throws IOException {
        byte flag = header.getByte(2), proto = (byte) (flag & SERIALIZATION_MASK);
        Serialization s = CodecSupport.getSerialization(url, proto);
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
                        data = decodeHeartbeatData(ctx, deserialize(s, url, is));
                    } else if (res.isEvent()) {
                        data = decodeEventData(ctx, deserialize(s, url, is));
                    } else {
                        DecodeableRpcResult result;
                        if (url.getParameter(
                                Constants.DECODE_IN_IO_THREAD_KEY,
                                Constants.DEFAULT_DECODE_IN_IO_THREAD)) {
                            result = new DecodeableRpcResult(ctx.channel(), res, is,
                                    (Invocation) getRequestData(id), proto);
                            result.decode();
                        } else {
                            result = new DecodeableRpcResult(ctx.channel(), res,
                                    new UnsafeByteArrayInputStream(readMessageData(is)),
                                    (Invocation) getRequestData(id), proto);
                        }
                        data = result;
                    }
                    res.setResult(data);
                } catch (Throwable t) {
                    if (log.isWarnEnabled()) {
                        log.warn("Decode response failed: " + t.getMessage(), t);
                    }
                    res.setStatus(Response.CLIENT_ERROR);
                    res.setErrorMessage(StringUtils.toString(t));
                }
            } else {
                res.setErrorMessage(deserialize(s, url, is).readUTF());
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
                    data = decodeHeartbeatData(ctx, deserialize(s, url, is));
                } else if (req.isEvent()) {
                    data = decodeEventData(ctx, deserialize(s, url, is));
                } else {
                    DecodeableRpcInvocation inv;
                    if (url.getParameter(
                            Constants.DECODE_IN_IO_THREAD_KEY,
                            Constants.DEFAULT_DECODE_IN_IO_THREAD)) {
                        inv = new DecodeableRpcInvocation(ctx.channel(), req, is, proto);
                        inv.decode();
                    } else {
                        inv = new DecodeableRpcInvocation(ctx.channel(), req,
                                new UnsafeByteArrayInputStream(readMessageData(is)), proto);
                    }
                    data = inv;
                }
                req.setData(data);
            } catch (Throwable t) {
                if (log.isWarnEnabled()) {
                    log.warn("Decode request failed: " + t.getMessage(), t);
                }
                // bad request
                req.setBroken(true);
                req.setData(t);
            }
            return req;
        }
    }

    private ObjectInput deserialize(Serialization serialization, URL url, InputStream is)
            throws IOException {
        return serialization.deserialize(url, is);
    }

    private byte[] readMessageData(InputStream is) throws IOException {
        if (is.available() > 0) {
            byte[] result = new byte[is.available()];
            is.read(result);
            return result;
        }
        return new byte[]{};
    }

    protected void encodeRequestData(ChannelHandlerContext ctx, ObjectOutput out, Object data) throws IOException {
        RpcInvocation inv = (RpcInvocation) data;

        out.writeUTF(inv.getAttachment(Constants.MESH_VERSION_KEY, DUBBO_VERSION));
        out.writeUTF(inv.getAttachment(Constants.PATH_KEY));
        out.writeUTF(inv.getAttachment(Constants.VERSION_KEY));

        out.writeUTF(RpcUtils.getMethodName(inv));
        // should be like Ljava.lang.String;
        out.writeUTF(((String[])inv.getArguments()[1])[0]);
        Object[] args = (Object[]) inv.getArguments()[2];
        if (args != null)
            for (int i = 0; i < args.length; i++) {
                out.writeObject(args[i]);
            }
        out.writeObject(inv.getAttachments());
    }

    protected void encodeResponseData(ChannelHandlerContext ctx, ObjectOutput out, Object data) throws IOException {
        Result result = (Result) data;

        Throwable th = result.getException();
        if (th == null) {
            Object ret = result.getValue();
            if (ret == null) {
                out.writeByte(RESPONSE_NULL_VALUE);
            } else {
                out.writeByte(RESPONSE_VALUE);
                out.writeObject(ret);
            }
        } else {
            out.writeByte(RESPONSE_WITH_EXCEPTION);
            out.writeObject(th);
        }
    }

    @Override
    public byte getCodecTypeId() {
        return 1;
    }

    /**
     * 获取解码的requestId，应该在fullDecode之后调用
     *
     * @param buffer
     */
    @Override
    public long getRequestId(ByteBuf payload) {
        return payload.getLong(4);
    }

    @Override
    public byte getStatus(ByteBuf payload) {
        return payload.getByte(3);
    }

    @Override
    public boolean isTwoWay(ByteBuf payload) {
        return (payload.getByte(2) & FLAG_TWOWAY) != 0;
    }

    @Override
    public boolean isEvent(ByteBuf payload) {
        return (payload.getByte(2) & FLAG_EVENT) != 0;
    }

    /**
     * 读取可以完整解码的字节报文
     *
     * @param ctx
     * @param buffer
     */
    @Override
    public Object decodeBytes(ChannelHandlerContext ctx, ByteBuf buffer) throws IOException {

        int readerIndex = buffer.readerIndex();

        int readable = buffer.readableBytes(),
                received = readable <= HEADER_LENGTH ? readable : HEADER_LENGTH;

        // maybe call retain() ??
        ByteBuf header = buffer.slice(buffer.readerIndex(), received);
        // set index to message body
        buffer.readerIndex(buffer.readerIndex() + received);

        // check magic number.
        if (readable > 0 && header.getByte(0) != MAGIC_HIGH
                || readable > 1 && header.getByte(1) != MAGIC_LOW) {
            int length = header.readableBytes();
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
                buffer.readerIndex(buffer.readerIndex() - received + i - 1);
                header = buffer.slice(buffer.readerIndex(), i);
            }

            return DecodeResult.NEED_MORE_INPUT;
        }

        // check length.
        if (readable < HEADER_LENGTH) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        Channel channel = ctx.channel();

        // URL url = channel.attr(Keys.URL_KEY).get();
        // get data length.
        int len = header.getInt(12);

        int tt = len + HEADER_LENGTH;
        if (readable < tt) {
            return DecodeResult.NEED_MORE_INPUT;
        }

        ByteBuf unresolvedBuffer = buffer.readerIndex(readerIndex).slice(readerIndex, tt).retain();

        // TODO 打印
        System.out.println("decode enpoint:" + NettyHttp1ServerHandler.decodeString(unresolvedBuffer, HEADER_LENGTH, unresolvedBuffer.readableBytes() - HEADER_LENGTH,
                Charset.forName("utf-8")));

        if(NettyHttp1ServerHandler.decodeString(unresolvedBuffer, HEADER_LENGTH, unresolvedBuffer.readableBytes() - HEADER_LENGTH,
                Charset.forName("utf-8")) == null) {
            System.out.println("...");
        }

        buffer.readerIndex(readerIndex + tt);
        return unresolvedBuffer;
    }
}
