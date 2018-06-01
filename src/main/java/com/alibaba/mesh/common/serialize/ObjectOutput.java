package com.alibaba.mesh.common.serialize;

import java.io.IOException;

/**
 * Object output.
 */
public interface ObjectOutput extends DataOutput {

    /**
     * write object.
     *
     * @param obj object.
     */
    void writeObject(Object obj) throws IOException;

    // void writeObject(List<Object> obj) throws IOException;

}