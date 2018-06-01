package com.alibaba.mesh.main;

import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.demo.MeshConsumer;
import com.alibaba.mesh.demo.MeshProvider;

/**
 * @author yiji
 */
public class Main {

    public static void main(String[] args) {

        String side = System.getProperty("type");

        if (StringUtils.isEmpty(side)) {
            throw new IllegalArgumentException("type should be 'provider' or 'consumer', actual '" + StringUtils.nullToEmpty(side) + "'");
        }

        if ("consumer".equals(side)) {
            MeshConsumer.main(args);
        } else if ("provider".equals(side)) {
            MeshProvider.main(args);
        }

    }

}
