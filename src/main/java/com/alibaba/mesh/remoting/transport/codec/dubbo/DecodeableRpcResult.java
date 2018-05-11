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
package com.alibaba.mesh.remoting.transport.codec.dubbo;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.serialize.ObjectInput;
import com.alibaba.mesh.common.utils.RpcUtils;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.Decodeable;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.exchange.Response;
import com.alibaba.mesh.remoting.transport.CodecSupport;
import com.alibaba.mesh.rpc.Invocation;
import com.alibaba.mesh.rpc.RpcResult;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public class DecodeableRpcResult extends RpcResult implements Decodeable {

    private static final Logger log = LoggerFactory.getLogger(DecodeableRpcResult.class);

    private Channel channel;

    private byte serializationType;

    private InputStream inputStream;

    private Response response;

    private Invocation invocation;

    private volatile boolean hasDecoded;

    public DecodeableRpcResult(Channel channel, Response response, InputStream is, Invocation invocation, byte id) {
        this.channel = channel;
        this.response = response;
        this.inputStream = is;
        this.invocation = invocation;
        this.serializationType = id;
    }

    public Object decode(Channel channel, InputStream input) throws IOException {

        URL url = channel.attr(Keys.URL_KEY).get();

        ObjectInput in = CodecSupport.getSerialization(url, serializationType)
                .deserialize(url, input);
        
        byte flag = in.readByte();
        switch (flag) {
            case DubboCodable.RESPONSE_NULL_VALUE:
                break;
            case DubboCodable.RESPONSE_VALUE:
                try {
                    Type[] returnType = RpcUtils.getReturnTypes(invocation);
                    setValue(returnType == null || returnType.length == 0 ? in.readObject() :
                            (returnType.length == 1 ? in.readObject((Class<?>) returnType[0])
                                    : in.readObject((Class<?>) returnType[0], returnType[1])));
                } catch (ClassNotFoundException e) {
                    throw new IOException(StringUtils.toString("Read response data failed.", e));
                }
                break;
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
        if (!hasDecoded && channel != null && inputStream != null) {
            try {
                decode(channel, inputStream);
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
