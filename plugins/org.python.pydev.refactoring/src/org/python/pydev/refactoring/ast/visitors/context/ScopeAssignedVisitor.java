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

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;

public class ScopeAssignedVisitor extends AbstractContextVisitor<SimpleAdapter> {

    private List<String> globalVars;

    public ScopeAssignedVisitor(ModuleAdapter module, AbstractScopeNode<?> parent) {
        super(module, parent);
        globalVars = new ArrayList<String>();
    }

    @Override
    protected void registerInContext(SimpleNode node) {
        String varName = nodeHelper.getName(node);
        if (!(globalVars.contains(varName))) {
            globalVars.add(varName);
            super.registerInContext(node);
        }

    }

    @Override
    public Object visitImport(Import node) throws Exception {
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        return null;
    }

    @Override
    public Object visitName(Name node) throws Exception {
        if (nodeHelper.isAssign(stack.peek())) {
            registerInContext(node);
        }
        return null;
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        // ignore attribute (must)
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        before(node);
        visit(node.targets);
        after();
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        visit(node.body);
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        return null;
    }

    @Override
    protected SimpleAdapter createAdapter(AbstractScopeNode<?> parent, SimpleNode node) {
        return new SimpleAdapter(moduleAdapter, parent, node, moduleAdapter.getAdapterPrefs());
    }

}
