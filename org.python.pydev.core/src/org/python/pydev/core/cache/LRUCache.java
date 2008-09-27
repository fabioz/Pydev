package org.python.pydev.core.cache;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;


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
    private transient int removeEntries;
    private transient int initialMaxSize;
    
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
    
    public void stopGrowAsNeeded(){
        synchronized(this){
            removeEntries--;
            if(removeEntries == 0){
                maxSize = initialMaxSize;
                Iterator<Entry<Key, Val>> iter = cache.entrySet().iterator();
                //go to the position of the 'eldest' entries
                for (int i = 0; i < cache.size() - maxSize; i++) {
                    iter.next();
                }
                //and now remove the eldest entries
                while(cache.size() > maxSize && iter.hasNext()){
                    iter.next();
                    iter.remove();
                }
            }
        }
    }
    
    /**
     * Can be used to stop removing oldest entries (this can be useful when doing some 'local' operations that want
     * to be faster, having the new max size passed as the new 'value'. Note that other calls to this method will
     * be ignored and will not change that limit). 
     * 
     * Also, the link between startGrowAsNeeded and stopGrowAsNeeded should be synchronized to avoid that one thread
     * raises and another one stopping that raise (so, use this with care).
     */
    public void startGrowAsNeeded(int newSize){
        if(removeEntries > 0){
            throw new RuntimeException("There is alrdeady a new size in action. This class should be synched for this access.");
        }
        synchronized(this){
            removeEntries++;
            if(removeEntries == 1){
                //start
                initialMaxSize = maxSize;
                maxSize = newSize;
            }
        }
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
