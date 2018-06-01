package com.alibaba.mesh.remoting.etcd.support;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.utils.ConcurrentHashSet;
import com.alibaba.mesh.common.utils.StringUtils;
import com.alibaba.mesh.remoting.etcd.ChildListener;
import com.alibaba.mesh.remoting.etcd.EtcdClient;
import com.alibaba.mesh.remoting.etcd.StateListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author yiji.github@hotmail.com
 */
public abstract class AbstractEtcdClient<WatcherListener> implements EtcdClient {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractEtcdClient.class);

    private final URL url;

    private final Set<StateListener> stateListeners = new ConcurrentHashSet<>();

    private final ConcurrentMap<String, ConcurrentMap<ChildListener, WatcherListener>> childListeners = new ConcurrentHashMap<String, ConcurrentMap<ChildListener, WatcherListener>>();
    private final List<String> categroies = Arrays.asList(Constants.PROVIDERS_CATEGORY
            , Constants.CONSUMERS_CATEGORY
            , Constants.ROUTERS_CATEGORY
            , Constants.CONFIGURATORS_CATEGORY);
    private volatile boolean closed = false;

    public AbstractEtcdClient(URL url) {
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }

    public void create(String path) {
        String fixedPath = fixNamespace(path);
        createParentIfAbsent(fixedPath);
        doCreatePersistent(fixedPath);
    }

    public long createEphemeral(String path) {
        String fixedPath = fixNamespace(path);
        createParentIfAbsent(fixedPath);
        return doCreateEphemeral(path);
    }

    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    public Set<StateListener> getSessionListeners() {
        return stateListeners;
    }

    public List<String> addChildListener(String path, final ChildListener listener) {
        ConcurrentMap<ChildListener, WatcherListener> listeners = childListeners.get(path);
        if (listeners == null) {
            childListeners.putIfAbsent(path, new ConcurrentHashMap<ChildListener, WatcherListener>());
            listeners = childListeners.get(path);
        }
        WatcherListener targetListener = listeners.get(listener);
        if (targetListener == null) {
            listeners.putIfAbsent(listener, createChildWatcherListener(path, listener));
            targetListener = listeners.get(listener);
        }
        return addChildWatcherListener(path, targetListener);
    }

    public WatcherListener getChildListener(String path, ChildListener listener) {
        ConcurrentMap<ChildListener, WatcherListener> listeners = childListeners.get(path);
        if (listeners == null) {
            return null;
        }
        WatcherListener targetListener = listeners.get(listener);
        if (targetListener == null) {
            listeners.putIfAbsent(listener, createChildWatcherListener(path, listener));
            targetListener = listeners.get(listener);
        }
        return targetListener;
    }

    public void removeChildListener(String path, ChildListener listener) {
        ConcurrentMap<ChildListener, WatcherListener> listeners = childListeners.get(path);
        if (listeners != null) {
            WatcherListener targetListener = listeners.remove(listener);
            if (targetListener != null) {
                removeChildWatcherListener(path, targetListener);
            }
        }
    }

    protected void stateChanged(int state) {
        for (StateListener sessionListener : getSessionListeners()) {
            sessionListener.stateChanged(state);
        }
    }

    protected String fixNamespace(String path) {
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("path is required, actual null or ''");
        }
        return (path.charAt(0) != '/') ? (Constants.PATH_SEPARATOR + path) : path;
    }

    protected void createParentIfAbsent(String fixedPath) {
        int i = fixedPath.lastIndexOf('/');
        if (i > 0) {
            String parentPath = fixedPath.substring(0, i);
            if (categroies.stream().anyMatch(c -> fixedPath.endsWith(c))) {
                if (!checkExists(parentPath)) {
                    this.doCreatePersistent(parentPath);
                }
            } else if (categroies.stream().anyMatch(c -> parentPath.endsWith(c))) {
                String grandfather = parentPath.substring(0, parentPath.lastIndexOf('/'));
                if (!checkExists(grandfather)) {
                    this.doCreatePersistent(grandfather);
                }
            }
        }
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            doClose();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    public abstract void doClose();

    public abstract void doCreatePersistent(String path);

    public abstract long doCreateEphemeral(String path);

    public abstract void delete(String path);

    public abstract boolean checkExists(String path);

    public abstract WatcherListener createChildWatcherListener(String path, ChildListener listener);

    public abstract List<String> addChildWatcherListener(String path, WatcherListener listener);

    public abstract void removeChildWatcherListener(String path, WatcherListener listener);

}
