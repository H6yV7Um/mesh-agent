package com.alibaba.mesh.registry.etcd;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.registry.Registry;
import com.alibaba.mesh.registry.support.AbstractRegistryFactory;
import com.alibaba.mesh.remoting.etcd.EtcdTransporter;

/**
 *
 * Etcd3RegistryFactory
 *
 * @author yiji.github@hotmail.com
 */
public class EtcdRegistryFactory extends AbstractRegistryFactory {

    private EtcdTransporter etcdTransporter;

    @Override
    protected Registry createRegistry(URL url) {
        return new EtcdRegistry(url, etcdTransporter);
    }

    public void setEtcdTransporter(EtcdTransporter etcdTransporter) {
        this.etcdTransporter = etcdTransporter;
    }
}
