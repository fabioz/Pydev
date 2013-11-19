/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import java.io.Serializable;

import org.python.pydev.shared_core.cache.CacheMapWrapper;

/**
 * @author fabioz
 *
 */
public final class SoftHashMapCache<Key, Val> extends CacheMapWrapper<Key, Val> implements Serializable {

    private static final long serialVersionUID = 1L;

    public SoftHashMapCache() {
        super(new SoftHashMap<Key, Val>());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void removeStaleEntries() {
        ((SoftHashMap) cache).removeStaleEntries();
    }
}
