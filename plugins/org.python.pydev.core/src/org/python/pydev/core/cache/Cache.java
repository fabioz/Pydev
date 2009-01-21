package org.python.pydev.core.cache;



/**
 * Defines the interface for a cache
 */
public interface Cache<Key, Val> {

    /**
     * This method returns the value for the given key. 
     */
    public Val getObj(Key o);

    /**
     * This method removes some key from the cache
     */
    public void remove(Key key);

    /**
     * Adds some value to the cache
     */
    public void add(Key key, Val n);
}
