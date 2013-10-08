/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.decoratorsType;

public class Decorators extends SimpleNode {

    public final decoratorsType[] exp;

    public Decorators(decoratorsType[] exp, int id) {
        this.exp = exp;
        this.setId(id);
    }
}
