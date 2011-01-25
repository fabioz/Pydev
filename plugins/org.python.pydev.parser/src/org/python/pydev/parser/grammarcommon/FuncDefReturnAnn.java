/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;

public class FuncDefReturnAnn extends SimpleNode {
    public final SimpleNode node;

    public FuncDefReturnAnn(SimpleNode node) {
        this.node = node;
    }
}
