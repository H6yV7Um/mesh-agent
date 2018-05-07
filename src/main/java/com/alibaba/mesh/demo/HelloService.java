package com.alibaba.mesh.demo;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloService implements IHelloService {

    private long count;

    private Logger logger = LoggerFactory.getLogger(HelloService.class);

    public HelloService() {

    }

    @Override
    public int hash(String str) throws Exception {
        int hashCode = str.hashCode();
        logger.info(++count + "_" + hashCode);
        sleep(50);

        return hashCode;
    }

    private void sleep(long duration) throws Exception {
        Thread.sleep(duration);
    }
}
