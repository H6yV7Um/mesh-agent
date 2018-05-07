package com.alibaba.mesh.rpc.protocol.mesh;

import com.alibaba.mesh.rpc.Exporter;
import com.alibaba.mesh.rpc.Invoker;
import com.alibaba.mesh.rpc.protocol.AbstractExporter;

import java.util.Map;

/**
 * MeshExporter
 */
public class MeshExporter extends AbstractExporter {

    private final String key;

    private final Map<String, Exporter<?>> exporterMap;

    public MeshExporter(Invoker invoker, String key, Map<String, Exporter<?>> exporterMap) {
        super(invoker);
        this.key = key;
        this.exporterMap = exporterMap;
    }

    @Override
    public void unexport() {
        super.unexport();
        exporterMap.remove(key);
    }

}