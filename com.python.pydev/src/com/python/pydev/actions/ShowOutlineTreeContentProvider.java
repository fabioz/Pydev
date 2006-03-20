/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;

public final class ShowOutlineTreeContentProvider implements ITreeContentProvider {
    
    private DefinitionsASTIteratorVisitor visitor;
    private Map<Object, ASTEntry[]> cache = new HashMap<Object, ASTEntry[]>();

    public Object[] getChildren(Object element) {
        Object[] ret = (Object[]) cache.get(element);
        if(ret != null){
            return ret;
        }
        
        ASTEntry entry = (ASTEntry) element;
        
        if (entry.node instanceof ClassDef || entry.node instanceof FunctionDef) {
            Iterator<ASTEntry> it = visitor.getOutline();
            ArrayList<ASTEntry> list = new ArrayList<ASTEntry>();
            while (it.hasNext()) {
                ASTEntry next = it.next();
                if(next.parent != null && next.parent.node == entry.node){
                    list.add(next);
                }
            }
            ASTEntry[] array = list.toArray(new ASTEntry[0]);
            cache.put(element, array);
            return array;
        }

        return null;
    }

    public Object getParent(Object element) {
        ASTEntry entry = (ASTEntry) element;
        return entry.parent;
    }

    public boolean hasChildren(Object element) {
        ASTEntry entry = (ASTEntry) element;
        
        if(entry.node instanceof ClassDef || entry.node instanceof FunctionDef){
            Object[] children = getChildren(entry);
            return children != null && children.length > 0;
        }
        return false;
    }

    public Object[] getElements(Object inputElement) {
        visitor = DefinitionsASTIteratorVisitor.create((SimpleNode) inputElement);
        if(visitor == null){
            return new Object[0];
        }
        Iterator<ASTEntry> it = visitor.getOutline();
        ArrayList<ASTEntry> list = new ArrayList<ASTEntry>();
        while(it.hasNext()){
            ASTEntry next = it.next();
            if(next.parent == null){
                list.add(next);
            }
        }
        return list.toArray(new ASTEntry[0]);
    }

    public void dispose() {
        //do nothing
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        //do nothing
    }
}