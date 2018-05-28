package com.alibaba.mesh.rpc;

/**
 * Exporter. (API/SPI, Prototype, ThreadSafe)
 *
 * @see com.alibaba.mesh.rpc.Protocol#export(Invoker)
 * @see com.alibaba.mesh.rpc.ExporterListener
 * @see com.alibaba.mesh.rpc.protocol.AbstractExporter
 */
public interface Exporter<T> {

    /**
     * get invoker.
     *
     * @return invoker
     */
    Invoker<T> getInvoker();

    /**
     * unexport.
     * <p>
     * <code>
     * getInvoker().destroy();
     * </code>
     */
    void unexport();

}