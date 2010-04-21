/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;

public final class ShowOutlineTreeContentProvider implements ITreeContentProvider {
    
    private List<ASTEntry> outline;
    
    private Map<SimpleNode, List<ASTEntry>> objectsWithParentStillNotDeterminedInOutline;
    private final Map<Object, ASTEntry[]> cache = new HashMap<Object, ASTEntry[]>();
	private Object[] elementsForCurrentInput;
	private Object newInput;
	private static final ASTEntry[] EMPTY = new ASTEntry[0];

    public Object[] getChildren(Object element) {
        Object[] ret = (Object[]) cache.get(element);
        if(ret != null){
            return ret;
        }
        
        ASTEntry entry = (ASTEntry) element;
        
        if (objectsWithParentStillNotDeterminedInOutline != null && (entry.node instanceof ClassDef || entry.node instanceof FunctionDef)) {
        	List<ASTEntry> list = objectsWithParentStillNotDeterminedInOutline.remove(entry.node);
			ASTEntry[] array = EMPTY;
        	if(list != null){
        		array = list.toArray(EMPTY);
        	}
        	cache.put(element, array);
            return array;
        }

        return EMPTY;
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
    	if(this.newInput == null){
    		this.inputChanged(null, null, inputElement);
    	}
    	Assert.isTrue(inputElement == this.newInput);
    	return elementsForCurrentInput==null?EMPTY:elementsForCurrentInput;
    }

    public void dispose() {
        outline = null;
        objectsWithParentStillNotDeterminedInOutline = null;
        cache.clear();
    	elementsForCurrentInput = null;
    	newInput = null;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    	if(newInput == this.newInput){
    		return;
    	}
    	
    	//let go of the old info before getting the new info
    	dispose();
    	this.newInput = newInput;
    	
    	if(newInput == null){
    		return;
    	}
    	
        //do nothing
    	DefinitionsASTIteratorVisitor visitor = DefinitionsASTIteratorVisitor.create((SimpleNode) newInput);
    	if(visitor == null){
    		elementsForCurrentInput = EMPTY;
    		return;
    	}
    	
    	outline = new ArrayList<ASTEntry>();
    	objectsWithParentStillNotDeterminedInOutline = new HashMap<SimpleNode, List<ASTEntry>>();
    	for(Iterator<ASTEntry> it=visitor.getOutline();it.hasNext();){
    		ASTEntry next = it.next();
    		outline.add(next);
    		if(next.parent != null){
    			List<ASTEntry> list = objectsWithParentStillNotDeterminedInOutline.get(next.parent.node);
    			if(list == null){
    				list = new ArrayList<ASTEntry>();
    				objectsWithParentStillNotDeterminedInOutline.put(next.parent.node, list);
    			}
    			list.add(next);
    		}
    	}
    	
    	ArrayList<ASTEntry> list = new ArrayList<ASTEntry>();
    	Iterator<ASTEntry> it = outline.iterator();
    	while(it.hasNext()){
    		ASTEntry next = it.next();
    		if(next.parent == null){
    			list.add(next);
    		}
    	}
    	elementsForCurrentInput = list.toArray(new ASTEntry[0]);
    }

}