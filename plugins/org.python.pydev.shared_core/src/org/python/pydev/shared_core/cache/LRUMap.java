/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author fabioz
 *
 */
public final class LRUMap<Key, Val> extends LinkedHashMap<Key, Val> {

    private static final long serialVersionUID = 1L;

    private int maxSize;

    public LRUMap(int maxSize) {
        super(maxSize + 1);
        if (maxSize <= 0) {
            throw new AssertionError("Max size must be > 0.");
        }
        this.maxSize = maxSize;
    }

    // This method is called just after a new entry has been added
    @Override
    public boolean removeEldestEntry(Map.Entry<Key, Val> eldest) {
        return size() > this.maxSize;
    }

}
