package com.alibaba.mesh.rpc.proxy.javassist;

import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.bytecode.Proxy;
import com.alibaba.mesh.common.bytecode.Wrapper;
import com.alibaba.mesh.rpc.Invoker;
import com.alibaba.mesh.rpc.proxy.AbstractProxyFactory;
import com.alibaba.mesh.rpc.proxy.AbstractProxyInvoker;
import com.alibaba.mesh.rpc.proxy.InvokerInvocationHandler;

/**
 * JavaassistRpcProxyFactory
 */
public class JavassistProxyFactory extends AbstractProxyFactory {

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
    }

}