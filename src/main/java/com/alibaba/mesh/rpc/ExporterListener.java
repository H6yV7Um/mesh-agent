package com.alibaba.mesh.rpc;

import com.alibaba.mesh.common.extension.SPI;

/**
 * ExporterListener. (SPI, Singleton, ThreadSafe)
 */
@SPI
public interface ExporterListener {

    /**
     * The exporter exported.
     *
     * @param exporter
     * @throws RpcException
     * @see com.alibaba.mesh.rpc.Protocol#export(Invoker)
     */
    void exported(Exporter<?> exporter) throws RpcException;

    /**
     * The exporter unexported.
     *
     * @param exporter
     * @throws RpcException
     * @see com.alibaba.mesh.rpc.Exporter#unexport()
     */
    void unexported(Exporter<?> exporter);

}