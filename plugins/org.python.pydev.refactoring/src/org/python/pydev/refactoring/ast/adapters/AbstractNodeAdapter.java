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

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.jython.ast.factory.NodeHelper;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.position.IndentVisitor;
import org.python.pydev.refactoring.ast.visitors.position.LastLineVisitor;
import org.python.pydev.shared_core.utils.Reflection;

public abstract class AbstractNodeAdapter<T extends SimpleNode> implements IASTNodeAdapter<T> {
    private ModuleAdapter module;
    private AbstractScopeNode<? extends SimpleNode> parent;
    private T adaptee;
    protected NodeHelper nodeHelper;
    private AdapterPrefs adapterPrefs;

    protected AbstractNodeAdapter() {
    }

    public AbstractNodeAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, T node, AdapterPrefs adapterPrefs) {
        init(module, parent, node, adapterPrefs);
    }

    protected void init(ModuleAdapter module, AbstractScopeNode<?> parent, T node, AdapterPrefs adapterPrefs) {
        this.module = module;
        this.parent = parent;
        this.adaptee = node;
        this.nodeHelper = new NodeHelper(adapterPrefs);
        this.adapterPrefs = adapterPrefs;
    }

    public AdapterPrefs getAdapterPrefs() {
        return adapterPrefs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getASTNode()
     */
    @Override
    public T getASTNode() {
        return adaptee;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getASTParent()
     */
    @Override
    public SimpleNode getASTParent() {
        return getParent().getASTNode();
    }

    @Override
    public String getName() {
        return nodeHelper.getName(getASTNode());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getNodeBodyIndent()
     */
    @Override
    public String getNodeBodyIndent() {
        return module.getIndentationFromAst(getASTNode());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getNodeFirstLine()
     */
    @Override
    public int getNodeFirstLine(boolean considerDecorators) {
        T astNode = getASTNode();
        if (!considerDecorators) {
            return astNode.beginLine;
        } else {
            return nodeHelper.getFirstLineConsideringDecorators(astNode);
        }
    }

    /**
     * Note that the line returned is 1-based.
     * 
     * @param beforeLine: 1-based too.
     */
    public int getLastNodeFirstLineBefore(int beforeLine) {
        SimpleNode astNode = getASTNode();
        int last = astNode.beginLine;

        stmtType[] body = (stmtType[]) Reflection.getAttrObj(astNode, "body");
        if (body != null) {
            for (int i = 0; i < body.length; i++) {
                SimpleNode node = body[i];
                if (!nodeHelper.isImport(node) && !nodeHelper.isStr(node)) {
                    int curr = node.beginLine;
                    if (curr > beforeLine) {
                        return last;
                    }
                    last = curr;
                }
            }
        }
        return last;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getNodeIndent()
     */
    @Override
    public int getNodeIndent() {
        IndentVisitor visitor = VisitorFactory.createVisitor(IndentVisitor.class, getASTNode());
        return visitor.getIndent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getNodeLastLine()
     */
    @Override
    public int getNodeLastLine() {
        LastLineVisitor visitor = VisitorFactory.createVisitor(LastLineVisitor.class, getASTNode());
        return visitor.getLastLine();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getParent()
     */
    @Override
    public AbstractScopeNode<? extends SimpleNode> getParent() {
        return parent;
    }

    @Override
    public String getParentName() {
        return nodeHelper.getName(getASTParent());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getParentNode()
     */
    @Override
    public SimpleNode getParentNode() {
        return getASTParent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#isModule()
     */
    @Override
    public boolean isModule() {
        return getParent() == null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getModule()
     */
    @Override
    public ModuleAdapter getModule() {
        return this.module;
    }

    @Override
    public String toString() {
        return "Adapter of " + adaptee;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((adaptee == null) ? 0 : adaptee.hashCode());
        result = prime * result + ((module == null) ? 0 : module.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractNodeAdapter other = (AbstractNodeAdapter) obj;
        if (adaptee == null) {
            if (other.adaptee != null) {
                return false;
            }
        } else if (!adaptee.equals(other.adaptee)) {
            return false;
        }
        if (module == null) {
            if (other.module != null) {
                return false;
            }
        } else if (!module.equals(other.module)) {
            return false;
        }
        return true;
    }

}
