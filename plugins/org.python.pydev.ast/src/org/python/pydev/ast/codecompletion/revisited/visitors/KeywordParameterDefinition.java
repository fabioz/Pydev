/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion.revisited.visitors;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Call;

/**
 * A definition where we found things as a keyword parameter in a call.
 * 
 * It contains the access to the keyword parameter as its ast and an additional call attribute (and attribute
 * if the call was inside an attribute)
 * 
 * @author fabioz
 */
public class KeywordParameterDefinition extends Definition {

    public Call call;

    public KeywordParameterDefinition(int line, int col, String value, SimpleNode ast, ILocalScope scope,
            IModule module, Call call) {
        super(line, col, value, ast, scope, module, false);
        this.call = call;
    }

}
