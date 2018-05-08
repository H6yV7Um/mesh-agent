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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class Consumer {

    public static void main(String[] args) {

        // Prevent to get IPV6 address,this way only work in debug mode
        // But you can pass use -Djava.net.preferIPv4Stack=true,then it work well whether in debug mode or not
        System.setProperty("java.net.preferIPv4Stack", "true");

        // Enable shutdown gracefully feature
        System.setProperty(ContainerMain.SHUTDOWN_HOOK_KEY, "true");

        // Search provider definition path
        System.setProperty(SpringContainer.SPRING_CONFIG, "META-INF/spring/mesh-consumer.xml");

        // Refer service
        ContainerMain.main(args);
    }

    private final static class DemoServiceConsumer implements ApplicationContextAware {

        private ApplicationContext context;

        public void init(){

            IHelloService demoService = context.getBean(IHelloService.class);

            while (true) {
                try {
                    Thread.sleep(1000);
                    // call remote method
                    int hash = demoService.hash("world");
                    // get result
                    System.out.println(hash);

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.context = applicationContext;
        }
    }
}
