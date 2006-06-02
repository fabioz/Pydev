package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.Tuple;

public abstract class AbstractStmtChange implements IChanges {
	
	/**
	 * This method returns a document change where changes should be added 
	 * @param doc the document this change is linked to
	 * @return a tuple with the document change and the root of the text edit change
	 */
    protected Tuple<DocumentChange, MultiTextEdit> getDocChange(IDocument doc) {
    	DocumentChange docChange = new DocumentChange("Add Stmt Change", doc);
    	
    	MultiTextEdit rootEdit = new MultiTextEdit();
    	docChange.setEdit(rootEdit);
    	docChange.setKeepPreviewEdits(true);
    	Tuple<DocumentChange, MultiTextEdit> tup = new Tuple<DocumentChange, MultiTextEdit>(docChange, rootEdit);
		return tup;
	}
    
    
    protected void addTextEdit(String desc, Tuple<DocumentChange, MultiTextEdit> tup, TextEdit edit) {
    	tup.o2.addChild(edit);
    	tup.o1.addTextEditGroup(new TextEditGroup(desc, edit));
	}


}
