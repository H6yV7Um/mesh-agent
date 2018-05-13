package com.alibaba.mesh.remoting.http2;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author yiji
 */
public class BeanLookup implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }


    public static <T> T find(Class<T> type, String name){
        return context.getBean(name, type);
    }
}
