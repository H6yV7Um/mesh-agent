package com.alibaba.mesh.rpc.cluster;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.extension.Adaptive;
import com.alibaba.mesh.common.extension.SPI;
import com.alibaba.mesh.rpc.Invocation;

/**
 * RouterFactory. (SPI, Singleton, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * @see com.alibaba.mesh.rpc.cluster.Cluster#join(Directory)
 * @see com.alibaba.mesh.rpc.cluster.Directory#list(Invocation)
 */
@SPI
public interface RouterFactory {

    /**
     * Create router.
     *
     * @param url
     * @return router
     */
    @Adaptive("protocol")
    Router getRouter(URL url);

}