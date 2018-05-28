package com.alibaba.mesh.rpc;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.extension.SPI;

/**
 * InvokerListener. (SPI, Singleton, ThreadSafe)
 */
@SPI
public interface InvokerListener {

    /**
     * The invoker referred
     *
     * @param invoker
     * @throws RpcException
     * @see com.alibaba.mesh.rpc.Protocol#refer(Class, URL)
     */
    void referred(Invoker<?> invoker) throws RpcException;

    /**
     * The invoker destroyed.
     *
     * @param invoker
     * @see com.alibaba.mesh.rpc.Invoker#destroy()
     */
    void destroyed(Invoker<?> invoker);

}