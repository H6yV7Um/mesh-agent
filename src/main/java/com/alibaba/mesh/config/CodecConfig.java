package com.alibaba.mesh.config;

import com.alibaba.mesh.config.annotation.Parameter;

/**
 * CodecConfig
 *
 * @author yiji.github@hotmail.com
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

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        isDefault = isDefault;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

}