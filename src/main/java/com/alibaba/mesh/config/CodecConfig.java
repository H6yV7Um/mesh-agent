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
 * CodecConfig
 *
 * @author yiji.github@hotmail.com
 *
 */
public class CodecConfig extends AbstractConfig {

    private static final long serialVersionUID = 527347798665232853L;

    // protocol codec name
    private String name;

    // serializer
    private String serialization;

    // deserializer
    private String deserialization;

    // thread pool
    private String threadpool;

    // thread pool size (fixed size)
    private Integer threads;

    // core thread pool size
    private Integer corethreads;

    // thread pool queue length
    private Integer queues;

    // used for serialization & deserialization ?
    private Boolean isActive;

    // if it's default
    private Boolean isDefault;

    public CodecConfig() {
    }

    public CodecConfig(String name) {
        setName(name);
    }

    public CodecConfig(String name, int port) {
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


    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        this.threadpool = threadpool;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getCorethreads() {
        return corethreads;
    }

    public void setCorethreads(Integer corethreads) {
        this.corethreads = corethreads;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public String getDeserialization() {
        return deserialization;
    }

    public void setDeserialization(String deserialization) {
        this.deserialization = deserialization;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}