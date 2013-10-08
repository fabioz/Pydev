/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.visitors.context;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;

public class LocalAttributeVisitor extends GlobalAttributeVisitor {

    private boolean inLocalScope;

    public LocalAttributeVisitor(ModuleAdapter module, AbstractScopeNode<?> parent) {
        super(module, parent);
        inLocalScope = false;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (nodeHelper.isClassDef(node)) {
            ClassDef classDef = (ClassDef) node;
            visit(classDef.body);
        } else {
            super.traverse(node);
        }
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        if (inLocalScope) {
            return null;
        } else {
            inLocalScope = true;
            return super.visitClassDef(node);
        }
    }
}
