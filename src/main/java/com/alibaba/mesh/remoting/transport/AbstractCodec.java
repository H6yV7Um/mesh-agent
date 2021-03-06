package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.serialize.Serialization;
import com.alibaba.mesh.common.utils.NetUtils;
import com.alibaba.mesh.remoting.Codec4;
import com.alibaba.mesh.remoting.Keys;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * AbstractCodec
 */
public abstract class AbstractCodec implements Codec4 {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCodec.class);

    protected static void checkPayload(URL url, Channel channel, long size) throws IOException {
        int payload = Constants.DEFAULT_PAYLOAD;
        if (url != null) {
            payload = url.getParameter(Constants.PAYLOAD_KEY, Constants.DEFAULT_PAYLOAD);
        }
        if (payload > 0 && size > payload) {
            // ignored
        }
    }

    protected boolean isClientSide(URL url, Channel channel) {
        String side = channel.attr(Keys.SIDE_KEY).get();
        if ("client".equals(side)) {
            return true;
        } else if ("server".equals(side)) {
            return false;
        } else {
            InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
            boolean client = url.getPort() == address.getPort()
                    && NetUtils.filterLocalHost(url.getIp()).equals(
                    NetUtils.filterLocalHost(address.getAddress()
                            .getHostAddress()));
            channel.attr(Keys.SIDE_KEY).set(client ? "client" : "server");
            return client;
        }
    }

    protected Serialization getSerialization(URL url) {
        return CodecSupport.getSerialization(url);
    }

}
