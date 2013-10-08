/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.cache;

import java.io.Serializable;

/**
 * If the cache is to be used by multiple threads,
 * the cache must be wrapped with code to synchronize the methods
 * cache = (Map)Collections.synchronizedMap(cache);
 * 
 * (it is actually serializable or not depending on its keys and values)
 */
public final class LRUCache<Key, Val> extends CacheMapWrapper<Key, Val> implements Serializable {

    private static final long serialVersionUID = 1L;

    public LRUCache(int maxSize) {
        super(new LRUMap<Key, Val>(maxSize));
    }

}
