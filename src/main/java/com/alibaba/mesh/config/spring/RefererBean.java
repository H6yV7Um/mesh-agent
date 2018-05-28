package com.alibaba.mesh.config.spring;

import com.alibaba.mesh.config.CodecConfig;
import com.alibaba.mesh.config.RefererConfig;
import com.alibaba.mesh.config.RegistryConfig;
import com.alibaba.mesh.config.annotation.Parameter;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReferenceFactoryBean
 *
 * @export
 */
public class RefererBean<T> extends RefererConfig<T> implements FactoryBean, ApplicationContextAware, InitializingBean, DisposableBean {

    private static final long serialVersionUID = 213195494150089726L;

    private transient ApplicationContext applicationContext;

    public RefererBean() {
        super();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        SpringExtensionFactory.addApplicationContext(applicationContext);
    }

    @Override
    public Object getObject() throws Exception {
        return get();
    }

    @Override
    public Class<?> getObjectType() {
        return getInterfaceClass();
    }

    @Override
    @Parameter(excluded = true)
    public boolean isSingleton() {
        return true;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void afterPropertiesSet() throws Exception {

        if (getCodecConfig() == null) {
            Map<String, CodecConfig> codecConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, CodecConfig.class, false, false);
            if (codecConfigMap != null && codecConfigMap.size() > 0) {
                CodecConfig codecConfig = null;
                for (CodecConfig config : codecConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        if (codecConfig != null) {
                            throw new IllegalStateException("Duplicate codec configs: " + codecConfig + " and " + config);
                        }
                        codecConfig = config;
                    }
                }
                if (codecConfig != null) {
                    setCodecConfig(codecConfig);
                }
            }
        }

        if (getRegistries() == null || getRegistries().isEmpty()) {
            Map<String, RegistryConfig> registryConfigMap = applicationContext == null ? null : BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, RegistryConfig.class, false, false);
            if (registryConfigMap != null && registryConfigMap.size() > 0) {
                List<RegistryConfig> registryConfigs = new ArrayList<RegistryConfig>();
                for (RegistryConfig config : registryConfigMap.values()) {
                    if (config.isDefault() == null || config.isDefault().booleanValue()) {
                        registryConfigs.add(config);
                    }
                }
                if (registryConfigs != null && !registryConfigs.isEmpty()) {
                    super.setRegistries(registryConfigs);
                }
            }
        }
        Boolean b = isInit();
        if (b != null && b.booleanValue()) {
            getObject();
        }
    }

    @Override
    public void destroy() {
        // do nothing
    }
}
