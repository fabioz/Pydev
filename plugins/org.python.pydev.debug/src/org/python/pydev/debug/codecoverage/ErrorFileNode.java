/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;

/**
 * @author Fabio Zadrozny
 */
public class ErrorFileNode implements ICoverageLeafNode {
    public Object node;
    public String desc;

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ErrorFileNode)) {
            return false;
        }

        ErrorFileNode f = (ErrorFileNode) obj;
        return f.node.equals(node) && f.desc == desc;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return FileNode.getName(node.toString(), PyCoveragePreferences.getNameNumberOfColumns()) + "   " + desc;
    }

}
