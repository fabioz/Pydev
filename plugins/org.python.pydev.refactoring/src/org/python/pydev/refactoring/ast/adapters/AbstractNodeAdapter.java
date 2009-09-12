/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.position.IndentVisitor;
import org.python.pydev.refactoring.ast.visitors.position.LastLineVisitor;

public abstract class AbstractNodeAdapter<T extends SimpleNode> implements IASTNodeAdapter<T> {
    private ModuleAdapter module;
    private AbstractScopeNode<? extends SimpleNode> parent;
    private T adaptee;
    protected NodeHelper nodeHelper;

    protected AbstractNodeAdapter() {
    }

    public AbstractNodeAdapter(ModuleAdapter module, AbstractScopeNode<?> parent, T node, String endLineDelim) {
        init(module, parent, node, endLineDelim);
    }

    protected void init(ModuleAdapter module, AbstractScopeNode<?> parent, T node, String endLineDelim) {
        this.module = module;
        this.parent = parent;
        this.adaptee = node;
        this.nodeHelper = new NodeHelper(endLineDelim);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getASTNode()
     */
    public T getASTNode() {
        return adaptee;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getASTParent()
     */
    public SimpleNode getASTParent() {
        return getParent().getASTNode();
    }

    public String getName() {
        return nodeHelper.getName(getASTNode());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getNodeBodyIndent()
     */
    public int getNodeBodyIndent() {
        IndentVisitor visitor = VisitorFactory.createVisitor(IndentVisitor.class, getASTNode());
        return visitor.getIndent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getNodeFirstLine()
     */
    public int getNodeFirstLine() {
        return getASTNode().beginLine;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getNodeIndent()
     */
    public int getNodeIndent() {
        IndentVisitor visitor = VisitorFactory.createVisitor(IndentVisitor.class, getASTNode());
        return visitor.getIndent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getNodeLastLine()
     */
    public int getNodeLastLine() {
        LastLineVisitor visitor = VisitorFactory.createVisitor(LastLineVisitor.class, getASTNode());
        return visitor.getLastLine();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getParent()
     */
    public AbstractScopeNode<? extends SimpleNode> getParent() {
        return parent;
    }

    public String getParentName() {
        return nodeHelper.getName(getASTParent());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getParentNode()
     */
    public SimpleNode getParentNode() {
        return getASTParent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#isModule()
     */
    public boolean isModule() {
        return getParent() == null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.refactoring.ast.adapters.IASTNodeAdapte#getModule()
     */
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
        if(this == obj){
            return true;
        }
        if(obj == null){
            return false;
        }
        if(getClass() != obj.getClass()){
            return false;
        }
        final AbstractNodeAdapter other = (AbstractNodeAdapter) obj;
        if(adaptee == null){
            if(other.adaptee != null){
                return false;
            }
        }else if(!adaptee.equals(other.adaptee)){
            return false;
        }
        if(module == null){
            if(other.module != null){
                return false;
            }
        }else if(!module.equals(other.module)){
            return false;
        }
        return true;
    }

}
