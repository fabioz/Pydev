package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;

public class DelStmtChange extends AbstractStmtChange{

    private SimpleNode applyAt;
    private String attr;
    private int pos;

    public DelStmtChange(SimpleNode node, String attr, int pos) {
        this.applyAt = node;
        this.attr = attr;
        this.pos = pos;
	}

	public Change getChange(IDocument doc) throws Throwable {
    	Tuple<DocumentChange, MultiTextEdit> tup = getDocChange(doc);
    	
        stmtType[] attrObj = (stmtType[]) REF.getAttrObj(applyAt, attr);

        if(attrObj == null || attrObj.length == 0){
        	return tup.o1;
        }
        
        //this is the statement we should remove
        stmtType stmt = attrObj[pos];
        
		return tup.o1;
	}

}
