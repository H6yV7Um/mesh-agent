package com.alibaba.mesh.config;

import com.alibaba.mesh.config.annotation.Parameter;

/**
 * EndPointConfig
 *
 * @author yiji.github@hotmail.com
 *
 */
public class EndPointConfig extends AbstractConfig {

    private static final long serialVersionUID = 5540582682412663487L;
    // protocol codec name
    private String name;

    // service IP address (when there are multiple network cards available)
    private String host;

    private int port;

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

    public Boolean isDefault() {
        return isDefault;
    }

    public void setDefault(Boolean isDefault) {
        isDefault = isDefault;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}