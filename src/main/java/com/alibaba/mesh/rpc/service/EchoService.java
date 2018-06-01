package com.alibaba.mesh.rpc.service;

/**
 * Echo service.
 *
 * @export
 */
public interface EchoService {

    /**
     * echo test.
     *
     * @param message message.
     * @return message.
     */
    Object $echo(Object message);

}