/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;

public interface IChanges {

    /**
     * @return the change to be applied to the document
     * @throws Throwable 
     */
    Change getChange(IDocument doc) throws Throwable;

}
