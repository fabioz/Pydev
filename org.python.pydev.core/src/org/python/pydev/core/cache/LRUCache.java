package org.python.pydev.core.cache;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * If the cache is to be used by multiple threads,
 * the cache must be wrapped with code to synchronize the methods
 * cache = (Map)Collections.synchronizedMap(cache);
 */
public class LRUCache<Key, Val> implements Cache<Key, Val>{

	private int maxSize;

	public LRUCache(int maxSize){
		this.maxSize = maxSize;
	}
	
	//Create cache
    Map<Key,Val> cache = new LinkedHashMap<Key,Val>(maxSize+1, .75F, true) {
        // This method is called just after a new entry has been added
        public boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maxSize;
        }
    };
    
	public Val getObj(Key key) {
		return cache.get(key);
	}
    
	public void remove(Key key) {
		cache.remove(key);
	}
	
	public void add(Key key, Val val) {
		cache.put(key, val);
	}
	
}
