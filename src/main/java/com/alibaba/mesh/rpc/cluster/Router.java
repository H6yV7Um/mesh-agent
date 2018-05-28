package com.alibaba.mesh.rpc.cluster;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.rpc.Invocation;
import com.alibaba.mesh.rpc.Invoker;
import com.alibaba.mesh.rpc.RpcException;

import java.util.List;

/**
 * Router. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * @see com.alibaba.mesh.rpc.cluster.Cluster#join(Directory)
 * @see com.alibaba.mesh.rpc.cluster.Directory#list(Invocation)
 */
public interface Router extends Comparable<Router> {

    /**
     * get the router url.
     *
     * @return url
     */
    URL getUrl();

    /**
     * route.
     *
     * @param invokers
     * @param url        refer url
     * @param invocation
     * @return routed invokers
     * @throws RpcException
     */
    <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;

}