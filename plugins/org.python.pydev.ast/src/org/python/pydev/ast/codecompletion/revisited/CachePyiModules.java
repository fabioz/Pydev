package org.python.pydev.ast.codecompletion.revisited;

import java.util.Map;

import org.python.pydev.shared_core.cache.LRUMap;

public class CachePyiModules {

    protected final Object cachePyiModulesLock = new Object();
    protected final Map<Object, Object> cache = new LRUMap<>(50);

    public void clear() {
        synchronized (cachePyiModulesLock) {
            cache.clear();
        }
    }

}
