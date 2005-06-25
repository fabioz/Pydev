/*
 * Created on 12/06/2005
 */
package org.python.pydev.parser.visitors.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.exprType;

/**
 * This class is used so that after transversing the AST, we have a simple structure for navigating
 * upon its nodes;
 *
 * This structure should provide:
 * - Classes
 * - Methods
 * - Indentation
 * 
 * 
 * @author Fabio
 */
public class EasyASTIteratorVisitor extends VisitorBase{

    private List nodes = new ArrayList();

    private Stack stack = new Stack();
    
    private int currentIndent = 0;

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
        entry.endLine = getEnd(lastVisited);
    }


    /**
     * @param node
     */
    private void atomic(SimpleNode node) {
        ASTEntry entry = new ASTEntry(getParent());
        entry.node = node;
        entry.endLine = getEnd(node);
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

    private int getEnd(SimpleNode v) {
        if(v instanceof Str){
            String s = ((Str)v).s;
            char[] cs = s.toCharArray();
            int found = 0;
            for (int i = 0; i < cs.length; i++) {
                if(cs[i] == '\n'){
                    found += 1;
                }
            }
//            StringTokenizer tokenizer = new StringTokenizer(s, "\n");
//            int countTokens = tokenizer.countTokens();
//            System.out.println("For-->"+s+"<-- ="+countTokens);
            return v.beginLine + found;
        }
        return v.beginLine;
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
                if(isInClassDecl()){
                    atomic(t);
                }
            }else if(t instanceof Attribute){
                if(isInClassMethodDecl()){
                    Attribute a = (Attribute) t;
                    if(a.value instanceof Name){
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
     * @return
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
     * @return
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
    
    public Iterator getIterator() {
        return nodes.iterator();
    }

    /**
     * @return
     */
    public Iterator getClassesIterator() {
        return getIterator(ClassDef.class);
    }

    public List getClassesAndMethodsList() {
        Iterator classesAndMethodsIterator = getClassesAndMethodsIterator();
        return getIteratorAsList(classesAndMethodsIterator);
    }
    
    /**
     * @param classesAndMethodsIterator
     * @return
     */
    private List getIteratorAsList(Iterator iter) {
        ArrayList list = new ArrayList();
        while (iter.hasNext()) {
            list.add(iter.next());
        }
        return list;
    }

    /**
     * @return
     */
    public Iterator getClassesAndMethodsIterator() {
        return getIterator(new Class[]{ClassDef.class, FunctionDef.class});
    }

    /**
     * @return
     */
    public Iterator getIterator(Class class_) {
        List newList = new ArrayList();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            ASTEntry entry = (ASTEntry) iter.next();
            if(entry.node.getClass().isAssignableFrom(class_)){
                newList.add(entry);
            }
        }
        return newList.iterator();
    }

    /**
     * @param classes
     * @return
     */
    public Iterator getIterator(Class[] classes) {
        List newList = new ArrayList();
        for (Iterator iter = nodes.iterator(); iter.hasNext();) {
            ASTEntry entry = (ASTEntry) iter.next();
            if(isFromClass(entry.node, classes)){
                newList.add(entry);
            }
        }
        return newList.iterator();
    }

    /**
     * @param node
     * @param classes
     * @return
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
}
