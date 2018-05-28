package com.alibaba.mesh.registry;

import com.alibaba.mesh.common.Node;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.registry.support.AbstractRegistry;

/**
 * Registry. (SPI, Prototype, ThreadSafe)
 *
 * @see RegistryFactory#getRegistry(URL)
 * @see AbstractRegistry
 */
public interface Registry extends Node, RegistryService {
}