/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.ast.visitors;

import java.util.LinkedList;
import java.util.List;

import org.python.pydev.parser.jython.ast.Name;

public class LocalVariablesVisitor extends ParentVisitor {
    List<Name> names;

    public LocalVariablesVisitor() {
        names = new LinkedList<Name>();
    }

    public Object visitName(Name node) throws Exception {
        names.add(node);
        return super.visitName(node);
    }

    public List<Name> getVariables() {
        return names;
    }
}
