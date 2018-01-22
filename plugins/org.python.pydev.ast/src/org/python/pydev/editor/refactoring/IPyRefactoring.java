/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 5, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.refactoring;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.model.ItemPointer;

/**
 * @author Fabio Zadrozny
 */
public interface IPyRefactoring {

    /**
     * @return The name for the user that represents this refactoring engine.
     * 
     * This is useful for 'solving conflicts' if more than one refactoring engine provides the same action.
     */
    public String getName();

    /**
     * Rename something (class, method, local...)
     */
    public String rename(IPyRefactoringRequest request);

    /**
     * Find where something is defined (many results because it may seem something is defined in several places)
     * @return an ItemPointer to some definition
     * @throws BadLocationException 
     */
    public ItemPointer[] findDefinition(RefactoringRequest request)
            throws TooManyMatchesException, BadLocationException;

}