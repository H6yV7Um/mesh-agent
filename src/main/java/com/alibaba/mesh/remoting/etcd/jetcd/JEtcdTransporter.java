package com.alibaba.mesh.remoting.etcd.jetcd;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.remoting.etcd.EtcdClient;
import com.alibaba.mesh.remoting.etcd.EtcdTransporter;

/**
 * @author yiji.github@hotmail.com
 */
public class JEtcdTransporter implements EtcdTransporter {

    @Override
    public EtcdClient connect(URL url) {
        return new JEtcdClient(url);
    }

}
