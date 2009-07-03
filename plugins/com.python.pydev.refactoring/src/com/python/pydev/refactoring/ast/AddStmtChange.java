/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.prettyprinter.PrettyPrinter;
import org.python.pydev.parser.prettyprinter.PrettyPrinterPrefs;
import org.python.pydev.parser.prettyprinter.WriterEraser;


/**
 * This change adds a statement to the ast
 * @author Fabio
 */
public class AddStmtChange extends AbstractStmtChange{

    private SimpleNode applyAt;
    private String attr;
    private int pos;
    private stmtType stmt;
    private boolean addNewLineAfterStmt;

    public AddStmtChange(SimpleNode node, String attr, int pos, stmtType stmt, boolean addNewLineAfterStmt) {
        this.applyAt = node;
        this.attr = attr;
        this.pos = pos;
        this.stmt = stmt;
        this.addNewLineAfterStmt = addNewLineAfterStmt;
    }

    
    /**
     * @see com.python.pydev.refactoring.ast.IChanges#getChange(org.eclipse.jface.text.IDocument)
     */
    public Change getChange(IDocument doc) throws Throwable {
        Tuple<TextChange, MultiTextEdit> tup = getDocChange(doc);
        
        return getDocChange(doc, tup);
    }


    public Change getDocChange(IDocument doc, Tuple<TextChange, MultiTextEdit> tup) throws Exception {
        stmtType[] attrObj = (stmtType[]) REF.getAttrObj(applyAt, attr);
        
        int prevStmtPos = 0;
        
        PrettyPrinterPrefs prefs = getPrefs(doc);
        if(attrObj == null || attrObj.length == 0){
            //if had no stmts, ignore the passed position
            attrObj = new stmtType[]{stmt}; 
            
        }else if(pos == 0){
            stmtType[] newAttrObj = new stmtType[attrObj.length+1];
            System.arraycopy(attrObj, 0, newAttrObj, 1, attrObj.length);
            newAttrObj[pos] = stmt;
            attrObj = newAttrObj;
            
        }else if(attrObj.length == pos){
            //if it is the last position, add it there
            stmtType[] newAttrObj = new stmtType[attrObj.length+1];
            System.arraycopy(attrObj, 0, newAttrObj, 0, attrObj.length);
            newAttrObj[pos] = stmt;
            attrObj = newAttrObj;
            prevStmtPos = getStmtOffsetEnd(newAttrObj[pos-1], null, doc, prefs);
            
        }else{
            //if is some insertion in the middle
            List<stmtType> lst = new ArrayList<stmtType>(Arrays.asList(attrObj));
            lst.add(pos, stmt);
            attrObj = lst.toArray(new stmtType[lst.size()]);
            prevStmtPos = getStmtOffsetEnd(attrObj[pos-1], attrObj[pos+1], doc, prefs);
        }
        
        WriterEraser writerEraser = new WriterEraser();
        PrettyPrinter printer = new PrettyPrinter(prefs, writerEraser, !addNewLineAfterStmt); //as it is a single statement, we won't add new lines when ending it
        stmt.accept(printer);
        StringBuffer buffer = writerEraser.getBuffer();
        
        InsertEdit insertEdit = new InsertEdit(prevStmtPos, buffer.toString());
        
        addTextEdit("Add Stmt Change", tup, insertEdit);
        return tup.o1;
    }





}
