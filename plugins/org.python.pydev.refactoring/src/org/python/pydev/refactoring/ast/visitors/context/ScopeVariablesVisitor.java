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

import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;

/**
 * Looks for all variables in a defined scope
 *
 */
public class ScopeVariablesVisitor extends AbstractContextVisitor<SimpleAdapter> {

    public ScopeVariablesVisitor(ModuleAdapter module, AbstractScopeNode<?> parent) {
        super(module, parent);
    }

    @Override
    public void visit(SimpleNode node) throws Exception {
        if (nodeHelper.isClassDef(node)) {
            return;
        }
        if (nodeHelper.isFunctionDef(node)) {
            return;
        }

        super.visit(node);
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (nodeHelper.isClassDef(node)) {
            return;
        }
        if (nodeHelper.isFunctionDef(node)) {
            return;
        }

        super.traverse(node);
    }

    @Override
    public Object visitImport(Import node) throws Exception {
        registerInContext(node);
        return null;
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        if (AbstractVisitor.isWildImport(node)) {
            throw new RuntimeException("Cannot handle wild imports.");
        }
        registerInContext(node);
        return null;
    }

    @Override
    protected SimpleAdapter createAdapter(AbstractScopeNode<?> parent, SimpleNode node) {
        return new SimpleAdapter(this.moduleAdapter, parent, node, moduleAdapter.getAdapterPrefs());
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        visit(node.value); // could be a local variable if not self
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        visit(node.body);
        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        visit(node.body);
        return null;
    }

    @Override
    public Object visitName(Name node) throws Exception {
        if (node.id.compareTo(NodeHelper.KEYWORD_SELF) == 0) {
            return null;
        }

        registerInContext(node);
        return null;
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        if (node.ctx != NameTok.FunctionName) {
            registerInContext(node);
        }
        return null;
    }

}
