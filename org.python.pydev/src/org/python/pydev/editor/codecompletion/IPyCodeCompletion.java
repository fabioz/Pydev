package org.python.pydev.editor.codecompletion;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;

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
     */
    @SuppressWarnings("unchecked")
    public abstract List getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request) throws CoreException,
            BadLocationException;

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

    /**
     * Compares proposals so that we can order them.
     */
    public static final ProposalsComparator PROPOSAL_COMPARATOR = new ProposalsComparator();

}