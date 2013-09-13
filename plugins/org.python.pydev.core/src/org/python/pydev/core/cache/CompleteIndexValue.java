/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import java.util.Set;

/**
 * @author fabioz
 *
 */
public class CompleteIndexValue {

    public Set<String> entries;

    @Override
    public String toString() {
        return entries != null ? entries.toString() : "null";
    }
}
