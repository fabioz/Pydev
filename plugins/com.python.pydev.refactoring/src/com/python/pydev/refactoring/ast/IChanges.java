/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.python.pydev.core.Tuple;

public interface IChanges {

    /**
     * @return the change to be applied to the document
     */
    Change getChange(IDocument doc) throws Throwable;
    
    /**
     * @param doc the document where the change will be applied
     * @param tup a tuple with the document change and the text edit change
     * @return the change to be applied
     */
    Change getDocChange(IDocument doc, Tuple<TextChange, MultiTextEdit> tup) throws Throwable;

}
