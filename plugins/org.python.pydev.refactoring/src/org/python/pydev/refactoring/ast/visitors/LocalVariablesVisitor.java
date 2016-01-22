/******************************************************************************
* Copyright (C) 2007-2009  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 *
 */

package org.python.pydev.refactoring.ast.visitors;

import java.util.List;

import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;

public class LocalVariablesVisitor extends ParentVisitor {
    List<Name> names;

    public LocalVariablesVisitor() {
        names = new LinkedListWarningOnSlowOperations<Name>();
    }

    @Override
    public Object visitName(Name node) throws Exception {
        names.add(node);
        return super.visitName(node);
    }

    public List<Name> getVariables() {
        return names;
    }
}
