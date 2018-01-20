/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.parser.jython.ast.ListComp;

public class ListCompDefinition extends Definition {

    /**
     * This is the token name.
     */
    public final String target;

    /**
     * Constructor.
     *
     * [|x| for x in [|R|(), G()]]:  |x| is the target and |R| is the value.
     */
    public ListCompDefinition(String value, String target, ListComp ast, int line, int col,
            ILocalScope scope, IModule module) {
        super(line, col, value, ast, scope, module);
        this.target = target;
    }

}
