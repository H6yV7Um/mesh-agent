package com.alibaba.mesh.common.serialize.support;

import java.util.Collection;

/**
 * This class can be replaced with the contents in config file, but for now I think the class is easier to write
 */
public interface SerializationOptimizer {

    Collection<Class> getSerializableClasses();
}
