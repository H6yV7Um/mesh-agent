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
package com.alibaba.mesh.config.spring.schema;

import com.alibaba.mesh.common.Version;
import com.alibaba.mesh.config.CodecConfig;
import com.alibaba.mesh.config.EndPointConfig;
import com.alibaba.mesh.config.ProtocolConfig;
import com.alibaba.mesh.config.RegistryConfig;
import com.alibaba.mesh.config.spring.ExporterBean;
import com.alibaba.mesh.config.spring.RefererBean;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * MeshNamespaceHandler
 *
 * @export
 */
public class MeshNamespaceHandler extends NamespaceHandlerSupport {

    static {
        Version.checkDuplicate(MeshNamespaceHandler.class);
    }

    @Override
    public void init() {
        registerBeanDefinitionParser("registry", new MeshBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("protocol", new MeshBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("endpoint", new MeshBeanDefinitionParser(EndPointConfig.class, true));
        registerBeanDefinitionParser("codec"   , new MeshBeanDefinitionParser(CodecConfig.class, true));
        registerBeanDefinitionParser("exporter", new MeshBeanDefinitionParser(ExporterBean.class, true));
        registerBeanDefinitionParser("referer" , new MeshBeanDefinitionParser(RefererBean.class, false));
    }

}