/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;

public interface IChanges {

    /**
     * @return the change to be applied to the document
     * @throws Throwable 
     */
    Change getChange(Document doc) throws Throwable;

}
