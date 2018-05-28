package com.alibaba.mesh.container;

import com.alibaba.mesh.common.extension.SPI;

/**
 * Container. (SPI, Singleton, ThreadSafe)
 */
@SPI("spring")
public interface Container {

    /**
     * start.
     */
    void start();

    /**
     * stop.
     */
    void stop();

}