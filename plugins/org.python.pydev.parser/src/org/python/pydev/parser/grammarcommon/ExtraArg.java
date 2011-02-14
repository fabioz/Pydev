/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.exprType;

public class ExtraArg extends SimpleNode {
    public final NameTok tok;
    public final exprType typeDef;

    public ExtraArg(NameTok tok, int id) {
        this(tok, id, null);
    }

    public ExtraArg(NameTok tok, int id, exprType typeDef) {
        this.setId(id);
        this.tok = tok;
        this.typeDef = typeDef;
    }

}
