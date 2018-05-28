package com.alibaba.mesh.remoting;

import com.alibaba.mesh.common.URL;

/**
 * Resetable.
 */
public interface Resetable {

    /**
     * reset.
     *
     * @param url
     */
    void reset(URL url);

}