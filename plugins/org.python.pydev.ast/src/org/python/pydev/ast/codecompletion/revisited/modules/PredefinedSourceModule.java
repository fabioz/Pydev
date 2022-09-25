/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion.revisited.modules;

import java.io.File;

import org.python.pydev.ast.codecompletion.revisited.visitors.GlobalModelVisitor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.SimpleNode;

public class PredefinedSourceModule extends SourceModule {

    public PredefinedSourceModule(String name, File f, SimpleNode n, Throwable parseError, IPythonNature nature) {
        super(name, f, n, parseError, nature);
        if ("builtins".equals(this.name)) {
            filter = (choice, token) -> {
                if (choice == GlobalModelVisitor.GLOBAL_TOKENS) {
                    String rep = token.getOriginalRep();
                    if (rep.startsWith("_") && !(rep.startsWith("__") && rep.endsWith("__"))) {
                        return false;
                    }
                }
                return true;
            };
        }
    }
}
