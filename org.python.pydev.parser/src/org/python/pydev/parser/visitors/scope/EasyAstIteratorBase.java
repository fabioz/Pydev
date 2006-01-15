package org.python.pydev.parser.visitors.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.VisitorBase;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * Iterator base. Keeps track of the current scope (method or class).
 * 
 * @author fabioz
 */
public abstract class EasyAstIteratorBase  extends VisitorBase{

    protected List<ASTEntry> nodes = new ArrayList<ASTEntry>();

    protected Stack<SimpleNode> stack = new Stack<SimpleNode>();
    
    protected SimpleNode lastVisited;
    
    /** 
     * @see org.python.parser.ast.VisitorBase#unhandled_node(org.python.parser.SimpleNode)
     */
    protected Object unhandled_node(SimpleNode node) throws Exception {
        this.lastVisited = node;
        return null;
    }

    /** 
     * @see org.python.parser.ast.VisitorBase#traverse(org.python.parser.SimpleNode)
     */
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    
    /**
     * @param node
     * @return
     */
    private ASTEntry before(SimpleNode node) {
        ASTEntry entry = new ASTEntry(getParent());
        entry.node = node;

        nodes.add(entry);
        stack.push(node);
        return entry;
    }

    /**
     * @param entry
     */
    private void after(ASTEntry entry) {
        stack.pop();
        entry.endLine = NodeUtils.getLineEnd(lastVisited);
    }


    /**
     * @param node
     */
    protected void atomic(SimpleNode node) {
        ASTEntry entry = new ASTEntry(getParent());
        entry.node = node;
        entry.endLine = NodeUtils.getLineEnd(node);
        nodes.add(entry);
    }

    /**
     * @return
     */
    protected ASTEntry getParent() {
        for (int i = stack.size()-1; i >= 0; i--) {
            SimpleNode o = (SimpleNode) stack.get(i);
            if(o instanceof ClassDef || o instanceof FunctionDef){
                return getEntryWithNode(o);
            }
        }
        return null;
    }

    /**
     * @param o
     * @return
     */
    protected ASTEntry getEntryWithNode(SimpleNode o) {
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            ASTEntry entry = (ASTEntry) iter.next();
            if(entry.node == o){
                return entry;
            }
        }
        return null;
    }

    /** 
     * @see org.python.parser.ast.VisitorBase#visitClassDef(org.python.parser.ast.ClassDef)
     */
    public Object visitClassDef(ClassDef node) throws Exception {
        ASTEntry entry = before(node);
        Object ret = super.visitClassDef(node);
        after(entry);
        
        return ret;
    }
    
    protected boolean isInGlobal() {
        Iterator iterator = stack.iterator();
        while(iterator.hasNext()){
            SimpleNode node = (SimpleNode) iterator.next();
            if(node instanceof ClassDef || node instanceof FunctionDef){
                return false;
            }
        }
        return true;
        
    }
    
    /**
     * @return wether we are in a class or method declaration scope
     */
    protected boolean isInClassMethodDecl() {
        Iterator iterator = stack.iterator();
        while(iterator.hasNext()){
            SimpleNode node = (SimpleNode) iterator.next();
            if(node instanceof ClassDef){
                break;
            }
        }
        while(iterator.hasNext()){
            SimpleNode node = (SimpleNode) iterator.next();
            if(node instanceof FunctionDef){
                return true;
            }
        }
        return false;
    }

    /**
     * @return whether we are in a class declaration scope
     */
    protected boolean isInClassDecl() {
        if(stack.size() == 0){
            return false;
        }
        
        SimpleNode last = (SimpleNode) stack.peek();
        if(last instanceof ClassDef){
            return true;
        }
        return false;
    }

    /** 
     * @see org.python.parser.ast.VisitorBase#visitFunctionDef(org.python.parser.ast.FunctionDef)
     */
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        ASTEntry entry = before(node);
        Object ret = super.visitFunctionDef(node);
        after(entry);
        
        return ret;
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
        Iterator<ASTEntry> classesAndMethodsIterator = getClassesAndMethodsIterator();
        return getIteratorAsList(classesAndMethodsIterator);
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
        return getIterator(new Class[]{ClassDef.class, FunctionDef.class});
    }

    /**
     * @see EasyASTIteratorVisitor#getIterator(Class[])
     */
    public Iterator<ASTEntry> getIterator(Class class_) {
        return getIterator(new Class[]{class_});
    }

    /**
     * @param classes the classes we are searching for
     * @return an iterator with nodes found from the passed classes
     */
    public Iterator<ASTEntry> getIterator(Class[] classes) {
        List<ASTEntry> newList = new ArrayList<ASTEntry>();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            ASTEntry entry = (ASTEntry) iter.next();
            if(isFromClass(entry.node, classes)){
                newList.add(entry);
            }
        }
        return newList.iterator();
    }

    /**
     * @param node this is the node we are analyzing
     * @param classes this are the classes we are looking for
     * @return true if the node is from one of the passed classes (may be some subclass too)
     */
    protected boolean isFromClass(SimpleNode node, Class[] classes) {
        Class class1 = node.getClass();
        for (int i = 0; i < classes.length; i++) {
            if(class1.isAssignableFrom(classes[i])){
                return true;
            }
        }
        return false;
    }
    
    public List<ASTEntry> getAsList(Class[] classes) {
        return getIteratorAsList(getIterator(classes));
    }

}
