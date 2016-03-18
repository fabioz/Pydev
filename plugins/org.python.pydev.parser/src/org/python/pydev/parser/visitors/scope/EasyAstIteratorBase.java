/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * Iterator base. Keeps track of the current scope (method or class).
 * 
 * This object (and subclasses) are 'disposable', meaning that they should only do 1 visit and after that
 * only the methods to get the classes should be called. 
 * 
 * @author fabioz
 */
public abstract class EasyAstIteratorBase extends VisitorBase {

    protected List<ASTEntry> nodes = new ArrayList<ASTEntry>();

    protected final FastStack<SimpleNode> stack = new FastStack<SimpleNode>(20);
    protected final FastStack<ASTEntry> parents = new FastStack<ASTEntry>(10);

    protected SimpleNode lastVisited;
    protected ASTEntry lastDefVisited; //ClassDef or FunctionDef

    private int higherLine = -1;

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#unhandled_node(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        this.lastVisited = node;
        int l = NodeUtils.getLineEnd(this.lastVisited);
        if (l > higherLine) {
            higherLine = l;
        }
        if (node.specialsAfter != null) {
            for (Object o : node.specialsAfter) {
                if (o instanceof ISpecialStr) {
                    ISpecialStr str = (ISpecialStr) o;
                    if (str.getBeginLine() > higherLine) {
                        higherLine = str.getBeginLine();
                    }
                }
            }
        }

        //the lastDefVisited is only kept if it's really the last definition visited (if there's a node
        //visited after the last def, it's not kept).
        if (this.lastDefVisited != null) {
            this.lastDefVisited = null;
        }
        return null;
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#traverse(org.python.pydev.parser.jython.SimpleNode)
     */
    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (node instanceof FunctionDef) {
            traverse((FunctionDef) node); //the order we traverse it is different
        } else {
            node.traverse(this);
        }
    }

    /**
     * @param node
     * @return
     */
    protected ASTEntry before(SimpleNode node) {
        ASTEntry entry;
        entry = createEntry();

        entry.node = node;

        doAddNode(entry);
        stack.push(node);
        return entry;
    }

    /**
     * @param entry the entry we're adding. The default implementation adds
     * the node to the returned nodes (flattened list)
     */
    protected void doAddNode(ASTEntry entry) {
        nodes.add(entry);
    }

    /**
     * @param entry
     */
    protected void after(ASTEntry entry) {
        stack.pop();

        //only set the end line if it was still not set
        if (entry.endLine == 0) {
            int lineEnd = NodeUtils.getLineEnd(lastVisited);
            if (lineEnd > higherLine) {
                entry.endLine = lineEnd;
            } else {
                entry.endLine = higherLine;
            }

            //also make comments found after the node a part of its context.
            List<Object> s = entry.node.specialsAfter;
            if (s != null) {
                for (Object o : s) {
                    if (o instanceof commentType) {
                        commentType comment = (commentType) o;
                        if (comment.beginLine > entry.endLine) {
                            entry.endLine = comment.beginLine;
                        }
                    }
                }
            }
        }
        this.lastDefVisited = entry;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        Object ret = super.visitModule(node);

        //after visiting the module, let's put the comments to the scope of the last definition found
        //if there were no other statements out of the class scope.
        int size = this.nodes.size();
        if (size > 0) {
            int i = -1;
            if (node.specialsAfter != null) {
                for (Object o : node.specialsAfter) {
                    if (o instanceof commentType) {
                        commentType type = (commentType) o;
                        if (type.beginLine > i) {
                            i = type.beginLine;
                        }
                    }
                }
            }
            if (i != -1 && this.lastDefVisited != null) {
                if (lastDefVisited.endLine < i) {
                    lastDefVisited.endLine = i;
                }
            }

        }
        return ret;
    }

    /**
     * @param node the node we're adding in an 'atomic' way
     * @return the ast entry that was created in this 'atomic' add
     */
    protected ASTEntry atomic(SimpleNode node) {
        ASTEntry entry;
        entry = createEntry();
        entry.node = node;
        entry.endLine = NodeUtils.getLineEnd(node);
        doAddNode(entry);
        return entry;
    }

    /**
     * @return the created entry (with its parent set)
     */
    protected ASTEntry createEntry() {
        ASTEntry entry;
        if (parents.size() > 0) {
            entry = new ASTEntry(parents.peek());
        } else {
            entry = new ASTEntry(null);
        }
        return entry;
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitClassDef(org.python.pydev.parser.jython.ast.ClassDef)
     */
    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        ASTEntry entry = before(node);
        parents.push(entry);
        traverse(node);
        after(entry);
        parents.pop();
        return null;
    }

    protected boolean isInGlobal() {
        Iterator<SimpleNode> iterator = stack.iterator();
        while (iterator.hasNext()) {
            SimpleNode node = iterator.next();
            if (node instanceof ClassDef || node instanceof FunctionDef) {
                return false;
            }
        }
        return true;

    }

    /**
     * @return whether we are in a class or method definition scope
     */
    protected boolean isInClassMethodDecl() {
        Iterator<SimpleNode> iterator = stack.iterator();
        while (iterator.hasNext()) {
            SimpleNode node = iterator.next();
            if (node instanceof ClassDef) {
                break;
            }
        }
        while (iterator.hasNext()) {
            SimpleNode node = iterator.next();
            if (node instanceof FunctionDef) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether we are in a class definition scope
     */
    protected boolean isInClassDecl() {
        if (stack.size() == 0) {
            return false;
        }

        SimpleNode last = stack.peek();
        if (last instanceof ClassDef) {
            return true;
        }
        return false;
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFunctionDef(org.python.pydev.parser.jython.ast.FunctionDef)
     */
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        ASTEntry entry = before(node);
        parents.push(entry);
        traverse(node);
        parents.pop();
        after(entry);

        return null;
    }

    public void traverse(FunctionDef node) throws Exception {
        if (node.decs != null) {
            for (int i = 0; i < node.decs.length; i++) {
                if (node.decs[i] != null)
                    node.decs[i].accept(this);
            }
        }

        if (node.name != null)
            node.name.accept(this);
        if (node.args != null)
            node.args.accept(this);
        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null)
                    node.body[i].accept(this);
            }
        }
    }

    /**
     * @return and iterator that passes through all the nodes
     */
    public Iterator<ASTEntry> getIterator() {
        return nodes.iterator();
    }

    /**
     * @return an iterator for all the classes definitions
     */
    public Iterator<ASTEntry> getClassesIterator() {
        return getIterator(ClassDef.class);
    }

    /**
     * @return a list with all the class and method definitions
     */
    public List<ASTEntry> getClassesAndMethodsList() {
        return getAsList(new Class[] { ClassDef.class, FunctionDef.class });
    }

    /**
     * @param iter this is the iterator we want to get as a list
     * @return a list with the elements of the iterator
     */
    protected List<ASTEntry> getIteratorAsList(Iterator<ASTEntry> iter) {
        ArrayList<ASTEntry> list = new ArrayList<ASTEntry>();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    /**
     * @return an iterator for class and method definitions
     */
    public Iterator<ASTEntry> getClassesAndMethodsIterator() {
        return getIterator(new Class[] { ClassDef.class, FunctionDef.class });
    }

    /**
     * @return an iterator for method definitions
     */
    public Iterator<ASTEntry> getMethodsIterator() {
        return getIterator(new Class[] { FunctionDef.class });
    }

    /**
     * @see EasyASTIteratorVisitor#getIterator(Class[])
     */
    public Iterator<ASTEntry> getIterator(Class class_) {
        return getIterator(new Class[] { class_ });
    }

    public List<ASTEntry> getAsList(Class... classes) {
        List<ASTEntry> newList = new ArrayList<ASTEntry>();
        for (Iterator<ASTEntry> iter = nodes.iterator(); iter.hasNext();) {
            ASTEntry entry = iter.next();
            if (isFromClass(entry.node, classes)) {
                newList.add(entry);
            }
        }
        return newList;
    }

    public List<ASTEntry> getAsList(Class class_) {
        return getAsList(new Class[] { class_ });
    }

    /**
     * @param classes the classes we are searching for
     * @return an iterator with nodes found from the passed classes
     */
    public Iterator<ASTEntry> getIterator(Class... classes) {
        return getAsList(classes).iterator();
    }

    /**
     * @return an iterator that will pass through Name and NameTok tokens
     */
    public Iterator<ASTEntry> getNamesIterator() {
        return new NameIterator(nodes);
    }

    public Iterator<ASTEntry> getOutline() {
        return new OutlineIterator(nodes);
    }

    public List<ASTEntry> getAll() {
        return nodes;
    }

    /**
     * @return an iterator that will pass all the nodes that were added in this visitor
     */
    public Iterator<ASTEntry> getAllIterator() {
        return nodes.iterator();
    }

    /**
     * @param node this is the node we are analyzing
     * @param classes this are the classes we are looking for
     * @return true if the node is from one of the passed classes (may be some subclass too)
     */
    @SuppressWarnings("unchecked")
    protected boolean isFromClass(SimpleNode node, Class[] classes) {
        Class class1 = node.getClass();
        for (int i = 0; i < classes.length; i++) {
            if (class1.isAssignableFrom(classes[i])) {
                return true;
            }
        }
        return false;
    }

}
