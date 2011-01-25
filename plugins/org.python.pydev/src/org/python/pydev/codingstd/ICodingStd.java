/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.codingstd;

public interface ICodingStd {

    /**
     * @return whether the locals and attributes should be camel-case (otherwise they are separated with '_')
     */
    boolean localsAndAttrsCamelcase();
}
