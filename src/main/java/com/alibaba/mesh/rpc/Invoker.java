package com.alibaba.mesh.rpc;

import com.alibaba.mesh.common.Node;

/**
 * Invoker. (API/SPI, Prototype, ThreadSafe)
 *
 * @see com.alibaba.mesh.rpc.Protocol#refer(Class, com.alibaba.mesh.common.URL)
 * @see com.alibaba.mesh.rpc.InvokerListener
 * @see com.alibaba.mesh.rpc.protocol.AbstractInvoker
 */
public interface Invoker<T> extends Node {

    /**
     * get service interface.
     *
     * @return service interface.
     */
    Class<T> getInterface();

    /**
     * invoke.
     *
     * @param invocation
     * @return result
     * @throws RpcException
     */
    Result invoke(Invocation invocation) throws RpcException;

}