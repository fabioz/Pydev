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
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.PropertyAdapter;

public class PropertyVisitor extends AbstractContextVisitor<PropertyAdapter> {

    public PropertyVisitor(ModuleAdapter module, AbstractScopeNode<?> parent) {
        super(module, parent);
    }

    @Override
    protected PropertyAdapter createAdapter(AbstractScopeNode<?> parent, SimpleNode node) {
        return new PropertyAdapter(moduleAdapter, parent, node, moduleAdapter.getAdapterPrefs());
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        if (nodeHelper.isAssign(stack.peek())) {
            registerInContext(stack.peek());
        }
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        if (nodeHelper.isPropertyAssign(node)) {
            stack.push(node);
            visit(node.value);
            stack.pop();
        }
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        return null;
    }

    /**
     * Traverse class body only
     */
    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        AbstractNodeAdapter<? extends SimpleNode> context = before(node);
        pushParent(context);
        visit(node.body);
        parents.pop();
        after();
        return null;
    }

}
