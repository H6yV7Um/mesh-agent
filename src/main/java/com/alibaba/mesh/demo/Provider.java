/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.mesh.demo;

import com.alibaba.mesh.container.ContainerMain;
import com.alibaba.mesh.container.spring.SpringContainer;

public class Provider {

    public static void main(String[] args) throws Exception {

        // Prevent to get IPV6 address,this way only work in debug mode
        // But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");

        // Search provider definition path
        System.setProperty(SpringContainer.SPRING_CONFIG, "META-INF/spring/mesh-provider.xml");

        // Export service
        ContainerMain.main(args);
    }

}
