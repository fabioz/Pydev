/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.exprType;

public class DefaultArg extends SimpleNode {

    final public exprType parameter;
    final public exprType value;
    final public exprType typeDef;
    final public int id;

    /**
     * @param id The id of the node that created this argument. E.g.:
     * JJTDEFAULTARG
     * JJTONLYKEYWORDARG
     * etc.
     */
    public DefaultArg(exprType parameter, exprType value, exprType typeDef, int id) {
        this.parameter = parameter;
        this.value = value;
        this.typeDef = typeDef;
        this.id = id;
    }

    public DefaultArg(exprType parameter, exprType value, int id) {
        this.parameter = parameter;
        this.value = value;
        this.typeDef = null;
        this.id = id;
    }

}
