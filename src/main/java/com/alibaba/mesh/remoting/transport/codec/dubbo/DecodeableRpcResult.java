package com.alibaba.mesh.remoting.transport.codec.dubbo;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.serialize.ObjectInput;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.Decodeable;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.transport.CodecSupport;
import com.alibaba.mesh.rpc.Invocation;
import com.alibaba.mesh.rpc.RpcResult;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DecodeableRpcResult extends RpcResult implements Decodeable {

    private static final Logger log = LoggerFactory.getLogger(DecodeableRpcResult.class);

    private Channel channel;

    private byte serializationType;

    private ByteBuf input;

    private Response response;

    private Invocation invocation;

    private volatile boolean hasDecoded;

    public DecodeableRpcResult(Channel channel, Response response, ByteBuf input, Invocation invocation, byte id) {
        this.channel = channel;
        this.response = response;
        this.input = input;
        this.invocation = invocation;
        this.serializationType = id;
    }

    public Object decode(Channel channel, ByteBuf input) throws IOException {

        URL url = channel.attr(Keys.URL_KEY).get();

        ObjectInput in = CodecSupport.getSerialization(url)
                .deserialize(url, input);

        byte flag = in.readByte();
        switch (flag) {
            case DubboCodable.RESPONSE_NULL_VALUE50:
            case DubboCodable.RESPONSE_NULL_VALUE:
                break;
            case DubboCodable.RESPONSE_VALUE49:
            case DubboCodable.RESPONSE_VALUE:
                try {
                    setValue(in.readObject());
                } catch (ClassNotFoundException e) {
                    throw new IOException(StringUtils.toString("Read response data failed.", e));
                }
                break;
            case DubboCodable.RESPONSE_WITH_EXCEPTION48:
            case DubboCodable.RESPONSE_WITH_EXCEPTION:
                try {
                    Object obj = in.readObject();
                    if (obj instanceof Throwable == false)
                        throw new IOException("Response data error, expect Throwable, but get " + obj);
                    setException((Throwable) obj);
                } catch (ClassNotFoundException e) {
                    throw new IOException(StringUtils.toString("Read response data failed.", e));
                }
                break;
            default:
                throw new IOException("Unknown result flag, expect '0' '1' '2', get " + flag);
        }
        return this;
    }

    @Override
    public void decode() throws Exception {
        if (!hasDecoded && channel != null && input != null) {
            try {
                decode(channel, input);
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn("Decode rpc result failed: " + e.getMessage(), e);
                }
                response.setStatus(Response.CLIENT_ERROR);
                response.setErrorMessage(StringUtils.toString(e));
            } finally {
                hasDecoded = true;
            }
        }
    }

}
