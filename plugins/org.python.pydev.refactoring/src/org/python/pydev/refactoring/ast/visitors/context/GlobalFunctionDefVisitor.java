/******************************************************************************
* Copyright (C) 2006-2009  IFS Institute for Software and others
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
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;

public class GlobalFunctionDefVisitor extends AbstractContextVisitor<FunctionDefAdapter> {

    public GlobalFunctionDefVisitor(ModuleAdapter module, AbstractScopeNode<?> parent) {
        super(module, parent);
    }

    @Override
    protected FunctionDefAdapter createAdapter(AbstractScopeNode<?> parent, SimpleNode node) {
        return new FunctionDefAdapter(moduleAdapter, parent, (FunctionDef) node, moduleAdapter.getAdapterPrefs());
    }

    @Override
    public void visit(SimpleNode node) throws Exception {
        super.visit(node);
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        registerInContext(node);
        return super.visitFunctionDef(node);
    }

}
