/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * If the cache is to be used by multiple threads,
 * the cache must be wrapped with code to synchronize the methods
 * cache = (Map)Collections.synchronizedMap(cache);
 * 
 * (it is actually serializable or not depending on its keys and values)
 */
public class LRUCache<Key, Val> implements Cache<Key, Val>, Serializable{

    protected int maxSize;

    private static final long serialVersionUID = 1L;
    
    public LRUCache(int maxSize){
        this.maxSize = maxSize;
        cache = createMap(maxSize);
    }

    protected LinkedHashMap<Key, Val> createMap(int maxSize) {
        return new LinkedHashMap<Key,Val>(maxSize+1, .75F, true) {
            // This method is called just after a new entry has been added
            public boolean removeEldestEntry(Map.Entry eldest) {
                return size() > LRUCache.this.maxSize;
            }
        };
    }
    
    
    //Create cache
    protected LinkedHashMap<Key,Val> cache;
    
    public Val getObj(Key key) {
        return cache.get(key);
    }
    
    public void remove(Key key) {
        cache.remove(key);
    }
    
    public void add(Key key, Val val) {
        cache.put(key, val);
    }
    
    public void clear(){
        cache.clear();
    }
    
}
