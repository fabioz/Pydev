/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.editor.IPySyntaxHighlightingAndCodeCompletionEditor;

public class PythonStringCompletionProcessor extends PythonCompletionProcessor {

    public PythonStringCompletionProcessor(IPySyntaxHighlightingAndCodeCompletionEditor edit,
            PyContentAssistant pyContentAssistant) {
        super(edit, pyContentAssistant);
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        //no auto-activation within strings.
        return new char[] { '@' };
    }

    @Override
    protected IPyCodeCompletion getCodeCompletionEngine() {
        return new PyStringCodeCompletion();
    }

    /**
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return new IContextInformationValidator() {

            @Override
            public void install(IContextInformation info, ITextViewer viewer, int offset) {
            }

            @Override
            public boolean isContextInformationValid(int offset) {
                return true;
            }

        };
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
        return new IContextInformation[] {};
    }

}
