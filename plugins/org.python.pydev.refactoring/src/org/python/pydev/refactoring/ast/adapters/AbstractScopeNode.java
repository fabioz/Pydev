/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
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

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.visitors.FindDuplicatesVisitor;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.context.ClassDefVisitor;
import org.python.pydev.refactoring.ast.visitors.context.LocalFunctionDefVisitor;
import org.python.pydev.refactoring.ast.visitors.context.ScopeAssignedVisitor;
import org.python.pydev.refactoring.ast.visitors.context.ScopeVariablesVisitor;
import org.python.pydev.shared_core.structure.Tuple;

public abstract class AbstractScopeNode<T extends SimpleNode> extends AbstractNodeAdapter<T> {
    private List<SimpleAdapter> usedVariables;
    private List<SimpleAdapter> assignedVariables;
    private List<FunctionDefAdapter> functions;
    private List<IClassDefAdapter> classes;

    protected AbstractScopeNode() {

    }

    public AbstractScopeNode(ModuleAdapter module, AbstractScopeNode<? extends SimpleNode> parent, T node,
            AdapterPrefs adapterPrefs) {
        super(module, parent, node, adapterPrefs);
    }

    public List<FunctionDefAdapter> getFunctions() {
        if (functions == null) {
            T node = getASTNode();
            ModuleAdapter module = getModule();
            assert (node != null);
            assert (module != null);
            LocalFunctionDefVisitor visitor = VisitorFactory.createContextVisitor(LocalFunctionDefVisitor.class, node,
                    module, this);
            functions = visitor.getAll();
        }

        return functions;
    }

    public List<IClassDefAdapter> getClasses() {
        if (this.classes == null) {
            ClassDefVisitor visitor = VisitorFactory.createContextVisitor(ClassDefVisitor.class, this.getASTNode(),
                    getModule(), this);

            this.classes = visitor.getAll();
        }
        return this.classes;
    }

    public List<SimpleAdapter> getAssignedVariables() {
        if (assignedVariables == null) {
            ScopeAssignedVisitor visitor = VisitorFactory.createContextVisitor(ScopeAssignedVisitor.class,
                    getASTNode(), this.getModule(), this);
            assignedVariables = visitor.getAll();
        }
        return assignedVariables;
    }

    public List<SimpleAdapter> getUsedVariables() {
        if (usedVariables == null) {
            ScopeVariablesVisitor visitor = VisitorFactory.createContextVisitor(ScopeVariablesVisitor.class,
                    getASTNode(), this.getModule(), this);
            usedVariables = visitor.getAll();
        }
        return usedVariables;
    }

    public boolean alreadyUsedName(String newName) {
        for (SimpleAdapter adapter : this.getUsedVariables()) {
            if (adapter.getName().compareTo(newName) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Provides all the duplicates in this scope (excluding the one from the passed selection).
     */
    public List<Tuple<ITextSelection, SimpleNode>> getDuplicates(ITextSelection selection, exprType expression) {
        FindDuplicatesVisitor v = VisitorFactory.createDuplicatesVisitor(selection, getASTNode(), expression, this,
                this.getModule().getDoc());
        return v.getDuplicates();
    }
}
