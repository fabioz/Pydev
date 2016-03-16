/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.cache;

import java.util.Map;

/**
 * @author fabioz
 *
 */
public class CacheMapWrapper<Key, Val> implements Cache<Key, Val> {

    protected CacheMapWrapper(Map<Key, Val> cache) {
        this.cache = cache;
    }

    //Create cache
    protected final Map<Key, Val> cache;

    @Override
    public Val getObj(Key key) {
        return cache.get(key);
    }

    @Override
    public void remove(Key key) {
        cache.remove(key);
    }

    @Override
    public void add(Key key, Val val) {
        cache.put(key, val);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void removeStaleEntries() {
        //Subclasses need to override if they have this concept (i.e.: SoftHashMap)
    }
}
