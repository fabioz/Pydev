/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.python.pydev.core.ModulesKey;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.shared_core.cache.LRUCache;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This is a 'global' cache implementation, that can have at most n objects in
 * the memory at any time.
 */
final class ModulesManagerCache {
    /**
     * Defines the maximum amount of modules that can be in the memory at any time (for all the managers)
     */
    private static final int MAX_NUMBER_OF_MODULES = 400;

    /**
     * The access to the cache is synchronized
     */
    private LRUCache<Tuple<ModulesKey, ModulesManager>, AbstractModule> internalCache;
    private final Object lock = new Object();

    ModulesManagerCache() {
        internalCache = new LRUCache<Tuple<ModulesKey, ModulesManager>, AbstractModule>(MAX_NUMBER_OF_MODULES);
    }

    /**
     * Overridden so that if we do not find the key, we have the chance to create it.
     */
    public AbstractModule getObj(ModulesKey key, ModulesManager modulesManager) {
        synchronized (modulesManager.modulesKeysLock) {
            Tuple<ModulesKey, ModulesManager> keyTuple = new Tuple<ModulesKey, ModulesManager>(key, modulesManager);

            synchronized (lock) {
                AbstractModule obj = internalCache.getObj(keyTuple);
                if (obj == null && modulesManager.modulesKeys.containsKey(key)) {
                    key = modulesManager.modulesKeys.get(key); //get the 'real' key
                    obj = AbstractModule.createEmptyModule(key);
                    internalCache.add(keyTuple, obj);
                }
                return obj;
            }
        }
    }

    public void remove(ModulesKey key, ModulesManager modulesManager) {
        synchronized (modulesManager.modulesKeysLock) {
            Tuple<ModulesKey, ModulesManager> keyTuple = new Tuple<ModulesKey, ModulesManager>(key, modulesManager);
            synchronized (lock) {
                internalCache.remove(keyTuple);
            }
        }
    }

    public void add(ModulesKey key, AbstractModule n, ModulesManager modulesManager) {
        synchronized (modulesManager.modulesKeysLock) {
            Tuple<ModulesKey, ModulesManager> keyTuple = new Tuple<ModulesKey, ModulesManager>(key, modulesManager);
            synchronized (lock) {
                internalCache.add(keyTuple, n);
            }
        }
    }

    public void clear() {
        synchronized (lock) {
            internalCache.clear();
        }
    }
}