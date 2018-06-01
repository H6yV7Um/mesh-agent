package com.alibaba.mesh.remoting.etcd;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.extension.Adaptive;
import com.alibaba.mesh.common.extension.SPI;

/**
 * Provides client connecting to etcd.
 *
 * @author yiji.github@hotmail.com
 */
@SPI("jetcd")
public interface EtcdTransporter {

    @Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
    EtcdClient connect(URL url);

}
