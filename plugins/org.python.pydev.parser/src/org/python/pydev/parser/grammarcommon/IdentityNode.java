/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;

public class IdentityNode extends SimpleNode {
    public int id;
    public Object image;

    IdentityNode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setImage(Object image) {
        this.image = image;
    }

    public Object getImage() {
        return image;
    }

    public String toString() {
        return "IdNode[" + id + ", " + image + "]";
    }
}