package com.alibaba.mesh.remoting.transport.codec.dubbo;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.serialize.ObjectInput;
import com.alibaba.mesh.common.utils.ReflectUtils;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.Decodeable;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.transport.CodecSupport;
import com.alibaba.mesh.rpc.RpcInvocation;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DecodeableRpcInvocation extends RpcInvocation implements Decodeable {

    private static final Logger log = LoggerFactory.getLogger(DecodeableRpcInvocation.class);

    private Channel channel;

    private byte serializationType;

    private ByteBuf input;

    private Request request;

    private volatile boolean hasDecoded;

    public DecodeableRpcInvocation(Channel channel, Request request, ByteBuf input, byte id) {
        this.channel = channel;
        this.request = request;
        this.input = input;
        this.serializationType = id;
    }

    @Override
    public void decode() throws Exception {
        if (!hasDecoded && channel != null && input != null) {
            try {
                decode(channel, input);
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn("Decode rpc invocation failed: " + e.getMessage(), e);
                }
                request.setBroken(true);
                request.setData(e);
            } finally {
                hasDecoded = true;
            }
        }
    }

    public Object decode(Channel channel, ByteBuf input) throws IOException {

        URL url = channel.attr(Keys.URL_KEY).get();

        ObjectInput in;
        in = CodecSupport.getSerialization(url)
                .deserialize(url, input);

        setAttachment(Constants.MESH_VERSION_KEY, in.readUTF());
        setAttachment(Constants.PATH_KEY, in.readUTF());
        setAttachment(Constants.VERSION_KEY, in.readUTF());

        setMethodName(in.readUTF());
        try {
            Object[] args;
            Class<?>[] pts;
            String desc = in.readUTF();
            if (desc.length() == 0) {
                pts = DubboCodable.EMPTY_CLASS_ARRAY;
                args = DubboCodable.EMPTY_OBJECT_ARRAY;
            } else {
                pts = ReflectUtils.desc2classArray(desc);
                args = new Object[pts.length];
                for (int i = 0; i < args.length; i++) {
                    try {
                        args[i] = in.readObject(pts[i]);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Decode argument failed: " + e.getMessage(), e);
                        }
                    }
                }
            }
            setParameterTypes(pts);

            Map<String, String> map = (Map<String, String>) in.readObject(Map.class);
            if (map != null && map.size() > 0) {
                Map<String, String> attachment = getAttachments();
                if (attachment == null) {
                    attachment = new HashMap<String, String>();
                }
                attachment.putAll(map);
                setAttachments(attachment);
            }
            setArguments(args);

        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read invocation data failed.", e));
        }
        return this;
    }

}
