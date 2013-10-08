/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.structure;

import java.util.HashSet;

/**
 * @author fabioz
 *
 */
public class CollectionFactory {

    public static HashSet<Object> createHashSet(Object... objects) {
        HashSet<Object> hashSet = new HashSet<Object>();
        for (Object object : objects) {
            hashSet.add(object);
        }
        return hashSet;
    }

}
