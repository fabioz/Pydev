/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * This class is used so that after transversing the AST, we have a simple structure for navigating
 * upon its nodes;
 *
 * This structure should provide:
 * - Imports
 * - Classes (and attributes)
 * - Methods
 * 
 * 
 * 
 * Note: it does not only provide global information, but also inner information, such as methods from a class.
 * 
 * @author Fabio
 */
public class EasyASTIteratorVisitor extends VisitorBase{

    private List<ASTEntry> nodes = new ArrayList<ASTEntry>();

    private Stack<SimpleNode> stack = new Stack<SimpleNode>();
    
    private SimpleNode lastVisited;
    
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
    private void atomic(SimpleNode node) {
        ASTEntry entry = new ASTEntry(getParent());
        entry.node = node;
        entry.endLine = NodeUtils.getLineEnd(node);
        nodes.add(entry);
    }

    /**
     * @return
     */
    private ASTEntry getParent() {
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
    private ASTEntry getEntryWithNode(SimpleNode o) {
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            ASTEntry entry = (ASTEntry) iter.next();
            if(entry.node == o){
                return entry;
            }
        }
        return null;
    }

    /** 
     * @see org.python.parser.ast.VisitorBase#visitImport(org.python.parser.ast.Import)
     */
    public Object visitImport(Import node) throws Exception {
        atomic(node);
        return super.visitImport(node);
    }

    /** 
     * @see org.python.parser.ast.VisitorBase#visitImportFrom(org.python.parser.ast.ImportFrom)
     */
    public Object visitImportFrom(ImportFrom node) throws Exception {
        atomic(node);
        return super.visitImportFrom(node);
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

    /** 
     * @see org.python.parser.ast.VisitorBase#visitAssign(org.python.parser.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        exprType[] targets = node.targets;
        for (int i = 0; i < targets.length; i++) {
            exprType t = targets[i];
            
            if(t instanceof Name){
                //we are in the class declaration
                if(isInClassDecl()){
                    //add the attribute for the class
                    atomic(t);
                }
                
            }else if(t instanceof Attribute){
                
                //we are in a method from the class
                if(isInClassMethodDecl()){
                    Attribute a = (Attribute) t;
                    if(a.value instanceof Name){
                        
                        //it is an instance variable attribute
                        Name n = (Name) a.value;
                        if (n.id.equals("self")){
		                    atomic(t);
                        }
                    }
                }
            }
        }
        return super.visitAssign(node);
    }
    
    /**
     * @return wether we are in a class or method declaration scope
     */
    private boolean isInClassMethodDecl() {
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
    private boolean isInClassDecl() {
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
    public Iterator getIterator() {
        return nodes.iterator();
    }

    /**
     * @return an iterator for all the classes definitions
     */
    public Iterator getClassesIterator() {
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
    private List<ASTEntry> getIteratorAsList(Iterator<ASTEntry> iter) {
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
    private boolean isFromClass(SimpleNode node, Class[] classes) {
        Class class1 = node.getClass();
        for (int i = 0; i < classes.length; i++) {
            if(class1.isAssignableFrom(classes[i])){
                return true;
            }
        }
        return false;
    }
    
    public static EasyASTIteratorVisitor create(SimpleNode root){
        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    public List<ASTEntry> getAsList(Class[] classes) {
        return getIteratorAsList(getIterator(classes));
    }
}
