package com.alibaba.mesh.common.serialize.fastjson;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.serialize.ObjectInput;
import com.alibaba.mesh.common.serialize.ObjectOutput;
import com.alibaba.mesh.common.serialize.Serialization;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

public class FastJsonSerialization implements Serialization {

    public static final String NAME = "json";

    @Override
    public byte getContentTypeId() {
        return 6;
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public ObjectOutput serialize(URL url, ByteBuf output) throws IOException {
        return new FastJsonObjectOutput(output);
    }

    @Override
    public ObjectInput deserialize(URL url, ByteBuf input) throws IOException {
        return new FastJsonObjectInput(input);
    }

}
