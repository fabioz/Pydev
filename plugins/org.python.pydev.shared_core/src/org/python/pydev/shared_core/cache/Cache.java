/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.cache;

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

    /**
     * If the cache has some process to remove stale entries, this is the time to do so.
     * 
     * Note that this interface does not include synchronization, so, clients should do the synchronization themselves.
     */
    public void removeStaleEntries();

    /**
     * Clears the cache.
     */
    public void clear();
}
