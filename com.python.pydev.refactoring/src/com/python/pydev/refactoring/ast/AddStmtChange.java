/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.REF;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;

import com.python.pydev.refactoring.visitors.PrettyPrinter;
import com.python.pydev.refactoring.visitors.PrettyPrinterPrefs;
import com.python.pydev.refactoring.visitors.WriterEraser;

/**
 * This change adds a statement to the ast
 * @author Fabio
 */
public class AddStmtChange implements IChanges {

    private SimpleNode applyAt;
    private String attr;
    private int pos;
    private stmtType stmt;

    public AddStmtChange(SimpleNode node, String attr, int pos, stmtType stmt) {
        this.applyAt = node;
        this.attr = attr;
        this.pos = pos;
        this.stmt = stmt;
    }

    /**
     * @see com.python.pydev.refactoring.ast.IChanges#getChange(org.eclipse.jface.text.Document)
     */
    public Change getChange(Document doc) throws Throwable {
        DocumentChange docChange = new DocumentChange("Add Stmt Change", doc);
        
        MultiTextEdit rootEdit = new MultiTextEdit();
        docChange.setEdit(rootEdit);
        docChange.setKeepPreviewEdits(true);

        stmtType[] attrObj = (stmtType[]) REF.getAttrObj(applyAt, attr);
        if(attrObj == null || attrObj.length == 0){
            attrObj = new stmtType[]{stmt}; 
            
        }else if(attrObj.length == pos){
            stmtType[] newAttrObj = new stmtType[attrObj.length+1];
            System.arraycopy(attrObj, 0, newAttrObj, 0, attrObj.length);
            newAttrObj[pos] = stmt;
            attrObj = newAttrObj;
        }else{
            throw new RuntimeException("still not ok");
        }
        
        WriterEraser writerEraser = new WriterEraser();
        PrettyPrinter printer = new PrettyPrinter(new PrettyPrinterPrefs("\n"), writerEraser);
        stmt.accept(printer);
        StringBuffer buffer = writerEraser.getBuffer();
        
        InsertEdit insertEdit = new InsertEdit(0, buffer.toString());
        
        rootEdit.addChild(insertEdit);
        docChange.addTextEditGroup(new TextEditGroup("Add Stmt Change", insertEdit));
        return docChange;
    }

}
