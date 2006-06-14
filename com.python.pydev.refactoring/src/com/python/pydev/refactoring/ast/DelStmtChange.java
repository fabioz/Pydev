package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;

public class DelStmtChange extends AbstractStmtChange{

    private SimpleNode applyAt;
    private String attr;
    private int pos;
    private boolean makeChangesToParent;

    /**
     * In this constructor, we'll not only make the changes in the document, but also in the ast.
     * @param node the parent where th e changes will be done
     * @param attr the nade of the attribute that will be changed (must be a stmtType[])
     * @param pos the position that will be changed
     */
    public DelStmtChange(SimpleNode node, String attr, int pos) {
        makeChangesToParent = true;
        this.applyAt = node;
        this.attr = attr;
        this.pos = pos;
	}

	public DelStmtChange(SimpleNode node) {
	    makeChangesToParent = false;
        this.applyAt = node;
    }

    public Change getChange(IDocument doc) throws Throwable {
    	Tuple<DocumentChange, MultiTextEdit> tup = getDocChange(doc);
        return getDocChange(doc, tup);
	}

    public Change getDocChange(IDocument doc, Tuple<DocumentChange, MultiTextEdit> tup) throws BadLocationException {
        if(makeChangesToParent){
            stmtType[] attrObj = (stmtType[]) REF.getAttrObj(applyAt, attr);
    
            if(attrObj == null || attrObj.length == 0){
            	return tup.o1;
            }
            
            //this is the statement we should remove
            stmtType next = null;
            if(attrObj.length > pos+1){
            	next = attrObj[pos+1];
            }
            
            SimpleNode stmt = attrObj[pos];
            int offsetStart = getOffsetFromNodeBegin(stmt, doc);
    		int offsetEnd = getStmtOffsetEnd(stmt, next, doc, getPrefs(doc));
            DeleteEdit delEdit = new DeleteEdit(offsetStart, offsetEnd - offsetStart);
            addTextEdit("Del Stmt Change", tup, delEdit);
    		return tup.o1;
        }else{
            throw new RuntimeException("Not Impl");
        }
    }
        

}
