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

package com.alibaba.mesh.remoting.support.header;

import com.alibaba.mesh.remoting.Client;
import com.alibaba.mesh.remoting.Keys;
import com.alibaba.mesh.remoting.exchange.Request;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

final class HeartBeatTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(HeartBeatTask.class);

    private ChannelProvider channelProvider;

    private int heartbeat;

    private int heartbeatTimeout;

    HeartBeatTask(ChannelProvider provider, int heartbeat, int heartbeatTimeout) {
        this.channelProvider = provider;
        this.heartbeat = heartbeat;
        this.heartbeatTimeout = heartbeatTimeout;
    }

    @Override
    public void run() {
//        try {
//            long now = System.currentTimeMillis();
//            for (Object holder : channelProvider.getChannelHolders()) {
//
//                Client client = null;
//                Channel channel = null;
//
//                if(holder instanceof Client) {
//                    client = (Client)holder;
//                    channel = client.getChannel();
//                }else{
//                    channel = (Channel)holder;
//                }
//
//                if (!channel.isActive()) {
//                    continue;
//                }
//                try {
//                    Long lastRead = (Long) channel.attr(Keys.READ_TIMESTAMP).get();
//                    Long lastWrite = (Long) channel.attr(Keys.WRITE_TIMESTAMP).get();
//                    if ((lastRead != null && now - lastRead > heartbeat)
//                            || (lastWrite != null && now - lastWrite > heartbeat)) {
//                        Request req = new Request();
//                        req.setVersion("2.0.0");
//                        req.setTwoWay(true);
//                        req.setEvent(Request.HEARTBEAT_EVENT);
//                        channel.writeAndFlush(req, channel.voidPromise());
//                        if (logger.isDebugEnabled()) {
//                            logger.debug("Send heartbeat to remote channel " + channel.remoteAddress()
//                                    + ", cause: The channel has no data-transmission exceeds a heartbeat period: " + heartbeat + "ms");
//                        }
//                    }
//                    if (lastRead != null && now - lastRead > heartbeatTimeout) {
//                        logger.warn("Close channel " + channel
//                                + ", because heartbeat read idle time out: " + heartbeatTimeout + "ms");
//                        if (client != null) {
//                            try {
//                                client.reconnect();
//                            } catch (Exception e) {
//                                //do nothing
//                            }
//                        } else {
//                            channel.close();
//                        }
//                    }
//                } catch (Throwable t) {
//                    logger.warn("Exception when heartbeat to remote channel " + channel.remoteAddress(), t);
//                }
//            }
//        } catch (Throwable t) {
//            logger.warn("Unhandled exception when heartbeat, cause: " + t.getMessage(), t);
//        }
    }

    interface ChannelProvider {
        Collection<Object> getChannelHolders();
    }

}

