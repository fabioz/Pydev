/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.REF;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;

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
        
        int prevStmtPos = 0;
        
        PrettyPrinterPrefs prefs = new PrettyPrinterPrefs("\n");
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
		PrettyPrinter printer = new PrettyPrinter(prefs, writerEraser);
        stmt.accept(printer);
        StringBuffer buffer = writerEraser.getBuffer();
        
        InsertEdit insertEdit = new InsertEdit(prevStmtPos, buffer.toString());
        
        rootEdit.addChild(insertEdit);
        docChange.addTextEditGroup(new TextEditGroup("Add Stmt Change", insertEdit));
        return docChange;
    }

    /**
     * @param prefs the preferences when printing the nodes
     * @param after: the offset returned should be after the end of the offset of this node (so this
     * is the node before the statement we're adding)
     * 
     * @param before: the offset returned should be before this stmt (so this is the node after it actually)
     *  
     * @return the end of the statement passed as an absolute offset
     */
	private int getStmtOffsetEnd(stmtType after, stmtType before, IDocument doc, PrettyPrinterPrefs prefs) {
		try {
			
			if(before != null){
				//it's easier to get it this way
				int lineOffset = doc.getLineOffset(before.beginLine-1);
				lineOffset += before.beginColumn-1;
				return lineOffset;
			}
			
			//ok, if we didn't have the node 
			GetLastStmtVisitor getLastStmtVisitor = new GetLastStmtVisitor();
			after.accept(getLastStmtVisitor);
			
			SimpleNode lastNode = getLastStmtVisitor.getLastNode();
			int lineEnd = NodeUtils.getLineEnd(lastNode);
			
			//now, let's get the col end
			WriterEraser writerEraser = new WriterEraser();
			PrettyPrinter printer = new PrettyPrinter(prefs, writerEraser);
			lastNode.accept(printer);
			StringBuffer buffer = writerEraser.getBuffer();
			int col = lastNode.beginColumn + buffer.toString().trim().length();
			
			try {
				int lineOffset = doc.getLineOffset(lineEnd-1);

				
				int offset = lineOffset + col - 1;
				if(offset > doc.getLength()){
					return doc.getLength();
				}
				
				int lineLength = doc.getLineLength(lineEnd-1);
				String lineDelimiter = doc.getLineDelimiter(lineEnd-1);
				if(offset == lineOffset + lineLength - lineDelimiter.length()){
					//we want to add it in a new line... (the start of the next line)
					return doc.getLineOffset(lineEnd);
				}
				
				return offset;
			} catch (BadLocationException e) {
				return doc.getLength();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
