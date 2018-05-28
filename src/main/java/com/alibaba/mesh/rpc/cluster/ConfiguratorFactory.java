package com.alibaba.mesh.rpc.cluster;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.extension.Adaptive;
import com.alibaba.mesh.common.extension.SPI;

/**
 * ConfiguratorFactory. (SPI, Singleton, ThreadSafe)
 *
 */
@SPI
public interface ConfiguratorFactory {

    /**
     * get the configurator instance.
     *
     * @param url - configurator url.
     * @return configurator instance.
     */
    @Adaptive("protocol")
    Configurator getConfigurator(URL url);

}
