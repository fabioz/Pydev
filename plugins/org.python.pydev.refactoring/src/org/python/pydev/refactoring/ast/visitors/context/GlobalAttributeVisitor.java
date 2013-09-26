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

import java.util.SortedSet;
import java.util.TreeSet;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;

public class GlobalAttributeVisitor extends AbstractContextVisitor<SimpleAdapter> {

    private SortedSet<String> uniqueAttributes;

    private FunctionDef lastFunctionDef;

    public GlobalAttributeVisitor(ModuleAdapter module, AbstractScopeNode<?> parent) {
        super(module, parent);
        uniqueAttributes = new TreeSet<String>();
    }

    @Override
    protected void registerInContext(SimpleNode node) {
        addUniqueOnly(node);
    }

    private void addUniqueOnly(SimpleNode node) {
        if (!(uniqueAttributes.contains(getUniqueID(node)))) {
            uniqueAttributes.add(getUniqueID(node));
            if (!(moduleAdapter.isImport(nodeHelper.getName(node)))) {
                super.registerInContext(node);
            }
        }
    }

    private String getUniqueID(SimpleNode node) {
        String parentName = nodeHelper.getName(parents.peek().getASTNode());
        String nodeName = nodeHelper.getName(node);
        return parentName + nodeName;
    }

    protected boolean isInAttribute() {
        for (SimpleNode node : stack) {
            if (nodeHelper.isAttribute(node)) {
                return true;
            }
        }
        return false;
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
        if (nodeHelper.isSelf(node.id)) {
            return null;
        }
        if (isInClassDef()) {
            if (!isInFunctionDef()) {
                if (!(moduleAdapter.isGlobal(nodeHelper.getName(node)))) {
                    registerInContext(node);
                }
            } else if (lastFunctionDef != null) {
                for (stmtType stmt : lastFunctionDef.body) {
                    if (nodeHelper.isClassDef(stmt)) {
                        if (stmt.equals(parents.peek().getASTNode())) {
                            registerInContext(node);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        if (isParentClassDecl() && isInAttribute()) {
            if (nodeHelper.isAttribute(stack.peek())) {
                registerInContext(node);
            }
        }
        return super.visitNameTok(node);
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        before(node);
        if (isParentClassDecl()) {
            if (nodeHelper.isName(node.value)) {
                SimpleNode parent = parents.peek().getASTNode();
                if (nodeHelper.isFullyQualified(node.value, parent)) {
                    if (nodeHelper.isNameTok(node.attr)) {
                        visit(node.attr);
                    }
                }
            }
        }
        after();
        return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        // ignore name!
        visit(node.args);
        visit(node.keywords);
        visit(node.starargs);
        visit(node.kwargs);
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        lastFunctionDef = node;
        // Track by class only (avoid function tracking)
        updateASTContext(node);
        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        AbstractNodeAdapter<? extends SimpleNode> context = before(node);
        pushParent(context);
        visit(node.body);
        parents.pop();
        after();
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        if (nodeHelper.isPropertyAssign(node)) {
            return null;
        }

        before(node);
        visit(node.targets);
        after();
        return null;
    }

    @Override
    protected SimpleAdapter createAdapter(AbstractScopeNode<?> parent, SimpleNode node) {
        return new SimpleAdapter(moduleAdapter, parent, node, moduleAdapter.getAdapterPrefs());
    }

}
