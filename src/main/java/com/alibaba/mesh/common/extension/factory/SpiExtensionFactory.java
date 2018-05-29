package com.alibaba.mesh.common.extension.factory;

import com.alibaba.mesh.common.extension.ExtensionFactory;
import com.alibaba.mesh.common.extension.ExtensionLoader;
import com.alibaba.mesh.common.extension.SPI;

/**
 * SpiExtensionFactory
 */
public class SpiExtensionFactory implements ExtensionFactory {

    public static final String NAME = "spi";

    @Override
    public <T> T getExtension(Class<T> type, String name) {
        if (type.isInterface() && type.isAnnotationPresent(SPI.class)) {
            ExtensionLoader<T> loader = ExtensionLoader.getExtensionLoader(type);
            if (!loader.getSupportedExtensions().isEmpty()) {
                return loader.getAdaptiveExtension();
            }
        }
        return null;
    }

}
