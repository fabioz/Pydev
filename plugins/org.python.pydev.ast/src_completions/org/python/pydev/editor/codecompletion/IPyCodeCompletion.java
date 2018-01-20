/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;

public interface IPyCodeCompletion {

    /**
     * Returns a list with the tokens to use for autocompletion.
     * 
     * The list is composed from tuples containing the following:
     * 
     * 0 - String  - token name
     * 1 - String  - token description
     * 2 - Integer - token type (see constants)
     * @param viewer 
     * 
     * @return list of IToken.
     * 
     * (This is where we do the "REAL" work).
     * @throws BadLocationException
     * @throws MisconfigurationException 
     * @throws IOException 
     * @throws PythonNatureWithoutProjectException 
     */
    public abstract List<Object> getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request)
            throws CoreException, BadLocationException, IOException, MisconfigurationException,
            PythonNatureWithoutProjectException;

    /**
     * Returns non empty string if we are in imports section 
     * 
     * @param theActivationToken
     * @param edit
     * @param doc
     * @param documentOffset
     * @return single space string if we are in imports but without any module
     *         string with current module (e.g. foo.bar.
     */
    public abstract ImportInfo getImportsTipperStr(CompletionRequest request);

}