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
        registerBeanDefinitionParser("codec", new MeshBeanDefinitionParser(CodecConfig.class, true));
        registerBeanDefinitionParser("exporter", new MeshBeanDefinitionParser(ExporterBean.class, true));
        registerBeanDefinitionParser("referer", new MeshBeanDefinitionParser(RefererBean.class, false));
    }

}
