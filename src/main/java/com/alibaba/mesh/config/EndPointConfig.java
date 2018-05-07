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
package com.alibaba.mesh.config;

import com.alibaba.mesh.config.annotation.Parameter;

/**
 * EndPointConfig
 *
 * @author yiji.github@hotmail.com
 *
 */
public class EndPointConfig extends AbstractConfig {

    private static final long serialVersionUID = 2209242247544070740L;

    // protocol codec name
    private String name;

    // service IP address (when there are multiple network cards available)
    private String host;

    // if it's default
    private Boolean isDefault;

    public EndPointConfig() {
    }

    public EndPointConfig(String name) {
        setName(name);
    }

    public EndPointConfig(String name, int port) {
        setName(name);
    }

    @Parameter(excluded = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        checkName("name", name);
        this.name = name;
        if (id == null || id.length() == 0) {
            id = name;
        }
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}