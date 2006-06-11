/*
 * Created on Jun 10, 2006
 * @author Fabio
 */
package org.python.pydev.parser.visitors.scope;

import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.commentType;

public class OutlineCreatorVisitor extends EasyASTIteratorWithChildrenVisitor{

    public static OutlineCreatorVisitor create(SimpleNode ast) {
        OutlineCreatorVisitor visitor = new OutlineCreatorVisitor();
        if(ast == null){
            return visitor;
        }
            
        try {
            ast.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

    private boolean isInAssign;
    

    
    @Override
    public void traverse(SimpleNode node) throws Exception {
        checkSpecials(node.specialsBefore);
        super.traverse(node);
        checkSpecials(node.specialsAfter);
    }
    
    @Override
    public void traverse(FunctionDef node) throws Exception {
        checkSpecials(node.specialsBefore);
        super.traverse(node);
        checkSpecials(node.specialsAfter);
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImport(org.python.pydev.parser.jython.ast.Import)
     */
    public Object visitImport(Import node) throws Exception {
        atomic(node);
        return super.visitImport(node);
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitImportFrom(org.python.pydev.parser.jython.ast.ImportFrom)
     */
    public Object visitImportFrom(ImportFrom node) throws Exception {
        atomic(node);
        return super.visitImportFrom(node);
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        isInAssign = true;
        try{
            DefinitionsASTIteratorVisitor.visitAssign(this, node, false);
        }finally{
            isInAssign = false;
        }
        traverse(node);
        
        return null;

    }
    
    @Override
    protected void doAddNode(ASTEntry entry) {
        SimpleNode node = entry.node;
        
        if(node instanceof commentType){
            commentType type = (commentType) node;
            if(type.beginColumn == 1){
                entry.parent = null; //top-level
            }
        }
        
        super.doAddNode(entry);
    }

    private void checkSpecials(List<Object> specials) {
        if(specials == null || isInAssign){
            return;
        }
        for (Object object : specials) {
            if(object instanceof commentType){
                commentType type = (commentType) object;
                if(type.id.trim().startsWith("#---")){
                    atomic(type);
                }
            }
        }
    }

}
