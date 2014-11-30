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

package org.python.pydev.refactoring.ast.adapters;

import java.util.List;

import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.context.LocalFunctionDefVisitor;
import org.python.pydev.refactoring.ast.visitors.context.ScopeAssignedVisitor;

public class FunctionDefAdapter extends AbstractScopeNode<FunctionDef> {

    private FunctionArgAdapter arguments;

    private List<FunctionDefAdapter> functions;

    public FunctionDefAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, FunctionDef node,
            AdapterPrefs adapterPrefs) {
        super(module, parent, node, adapterPrefs);
        this.arguments = new FunctionArgAdapter(getModule(), this, getASTNode().args, adapterPrefs);
        this.functions = null;
    }

    public FunctionArgAdapter getArguments() {
        return arguments;
    }

    public boolean isInit() {
        return nodeHelper.isInit(getASTNode());
    }

    public boolean isDefaultInit() {
        return isInit() && (arguments.isEmptyArgument() || arguments.hasOnlySelf());
    }

    public String getSignature() {
        return arguments.getSignature();
    }

    @Override
    public String getNodeBodyIndent() {
        FunctionDef functionNode = getASTNode();
        if (functionNode.body == null || functionNode.body.length == 0) {
            PySelection pySelection = new PySelection(getModule().getDoc());
            String indentationFromLine = PySelection.getIndentationFromLine(pySelection
                    .getLine(functionNode.beginLine - 1));
            return indentationFromLine
                    + DefaultIndentPrefs.get(this.getAdapterPrefs().projectAdaptable).getIndentationString();

        }

        return getModule().getIndentationFromAst(functionNode.body[0]);
    }

    @Override
    public List<FunctionDefAdapter> getFunctions() {
        if (this.functions == null) {
            LocalFunctionDefVisitor visitor = null;
            visitor = VisitorFactory.createContextVisitor(LocalFunctionDefVisitor.class, this.getASTNode(),
                    getModule(), this);

            this.functions = visitor.getAll();
        }
        return this.functions;
    }

    @Override
    public List<SimpleAdapter> getAssignedVariables() {
        ScopeAssignedVisitor visitor = VisitorFactory.createContextVisitor(ScopeAssignedVisitor.class, getASTNode(),
                this.getModule(), this);
        return visitor.getAll();
    }
}
