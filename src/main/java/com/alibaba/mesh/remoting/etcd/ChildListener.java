package com.alibaba.mesh.remoting.etcd;

import java.util.List;

public interface ChildListener {

    void childChanged(String path, List<String> children);

}
