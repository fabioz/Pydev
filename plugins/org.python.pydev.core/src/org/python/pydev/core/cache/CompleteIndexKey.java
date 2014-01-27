/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import java.io.Serializable;

import org.python.pydev.core.ModulesKey;

/**
 * @author fabioz
 *
 */
public class CompleteIndexKey implements Serializable {

    private static final long serialVersionUID = 1L;

    public ModulesKey key;

    public long lastModified;

    /**
     * @param key
     */
    public CompleteIndexKey(ModulesKey key) {
        this.key = key;
    }

    public CompleteIndexKey(ModulesKey key, long lastModified) {
        this.key = key;
        this.lastModified = lastModified;
    }

    public CompleteIndexKey(String name) {
        this(new ModulesKey(name, null));
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CompleteIndexKey)) {
            return false;
        }

        CompleteIndexKey m = (CompleteIndexKey) o;
        if (!(this.key.name.equals(m.key.name))) {
            return false;
        }

        //consider only the name
        return true;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.key.name.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.key.toString();
    }
}
