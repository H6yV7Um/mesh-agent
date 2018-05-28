package com.alibaba.mesh.rpc.cluster.support;

import com.alibaba.mesh.rpc.Invoker;
import com.alibaba.mesh.rpc.RpcException;
import com.alibaba.mesh.rpc.cluster.Cluster;
import com.alibaba.mesh.rpc.cluster.Directory;

/**
 * {@link FailoverClusterInvoker}
 *
 */
public class FailoverCluster implements Cluster {

    public final static String NAME = "failover";

    @Override
    public <T> Invoker<T> join(Directory<T> directory) throws RpcException {
        return new FailoverClusterInvoker<T>(directory);
    }

}
