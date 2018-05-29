package com.alibaba.mesh.container;

import com.alibaba.mesh.remoting.http2.NettyHttp2Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yiji
 */
public class HttpContainer implements Container {

    public final static String NAME = "http";

    private static final Logger logger = LoggerFactory.getLogger(HttpContainer.class);

    NettyHttp2Server http2Server = new NettyHttp2Server();

    /**
     * start.
     */
    @Override
    public void start() {
        try{
            http2Server.start();
            logger.info("Http server started.");
        }catch (Exception e){
            logger.error("Failed to start http server", e);
        }
    }

    /**
     * stop.
     */
    @Override
    public void stop() {
        http2Server.stop();
        logger.info("Http server stoped.");
    }
}
