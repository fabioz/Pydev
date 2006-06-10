/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package org.python.pydev.parser.visitors.scope;

import java.util.ArrayList;

public class EasyASTIteratorWithChildrenVisitor extends EasyAstIteratorBase{

    /**
     * Overriden because we deal only with the nodes with children in this iterator
     * 
     * @see org.python.pydev.parser.visitors.scope.EasyAstIteratorBase#createEntry()
     */
    @Override
    protected ASTEntry createEntry() {
        ASTEntry entry;
        if(parents.size() > 0){
            entry = new ASTEntryWithChildren((ASTEntryWithChildren) parents.peek());
        }else{
            entry = new ASTEntryWithChildren(null);
        }
        return entry;
    }
    
    /**
     * This implementation only adds it to the flattened list (nodes) if there is no parent.
     * Otherwise (if there is a parent), this implementation will add it to the parents children.
     * 
     * @see org.python.pydev.parser.visitors.scope.EasyAstIteratorBase#doAddNode(org.python.pydev.parser.visitors.scope.ASTEntry)
     */
    @Override
    protected void doAddNode(ASTEntry entry) {
        if(entry.parent == null){
            super.doAddNode(entry);
        }else{
            ASTEntryWithChildren parent = (ASTEntryWithChildren)entry.parent;
            if(parent.children == null){
                parent.children = new ArrayList<ASTEntryWithChildren>();
            }
            parent.children.add((ASTEntryWithChildren) entry);
        }
    }
}
