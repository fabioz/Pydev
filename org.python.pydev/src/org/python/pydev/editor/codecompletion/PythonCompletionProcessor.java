/*
 * Created on Mar 29, 2004
 *
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Point;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 * 
 * This class is responsible for code completion / template completion.
 */
public class PythonCompletionProcessor implements IContentAssistProcessor {

    private PyTemplateCompletion templatesCompletion = new PyTemplateCompletion();

    private PyCodeCompletion codeCompletion = new PyCodeCompletion();

    private CompletionCache completionCache = new CompletionCache();

    private boolean endsWithSomeChar(char cs[], String activationToken) {
        for (int i = 0; i < cs.length; i++) {
            if (activationToken.endsWith(cs[i] + "")) {
                return true;
            }
        }
        return false;

    }

    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int documentOffset) {
        List propList = new ArrayList();
        IDocument doc = viewer.getDocument();

        Point selectedRange = viewer.getSelectedRange();
        // there may not be a selected range
        java.lang.String theDoc = doc.get();
        codeCompletion.calcDocBoundary(theDoc, documentOffset);

        String activationToken = codeCompletion.getActivationToken(theDoc,
                documentOffset);

        java.lang.String qualifier = "";
        char[] cs = getCompletionProposalAutoActivationCharacters();

        while (endsWithSomeChar(cs, activationToken) == false
                && activationToken.length() > 0) {

            qualifier = activationToken.charAt(activationToken.length() - 1)
                    + qualifier;
            activationToken = activationToken.substring(0, activationToken
                    .length() - 1);
        }

        theDoc = codeCompletion.partialDocument(theDoc, documentOffset);

        int qlen = qualifier.length();
        theDoc += "\n" + activationToken;

        List allProposals = this.completionCache.getAllProposals(theDoc,
                activationToken, documentOffset, qlen, codeCompletion);

        //templates proposals are added here.
        this.templatesCompletion.addTemplateProposals(viewer, documentOffset,
                propList);

        for (Iterator iter = allProposals.iterator(); iter.hasNext();) {
            ICompletionProposal proposal = (ICompletionProposal) iter.next();
            if (proposal.getDisplayString().startsWith(qualifier)) {
                propList.add(proposal);
            }
        }

        ICompletionProposal[] proposals = new ICompletionProposal[propList
                .size()];
        // and fill with list elements
        propList.toArray(proposals);
        // Return the proposals
        return proposals;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    public IContextInformation[] computeContextInformation(ITextViewer viewer,
            int documentOffset) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        char[] c = new char[0];
        if (PyCodeCompletionPreferencesPage.isToAutocompleteOnDot()) {
            c = addChar(c, '.');
        }
        if (PyCodeCompletionPreferencesPage.isToAutocompleteOnPar()) {
            c = addChar(c, '(');
        }
        return c;
    }

    private char[] addChar(char[] c, char toAdd) {
        char[] c1 = new char[c.length + 1];

        int i;

        for (i = 0; i < c.length; i++) {
            c1[i] = c[i];
        }
        c1[i] = toAdd;
        return c1;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    public char[] getContextInformationAutoActivationCharacters() {
        return new char[] {};
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    public java.lang.String getErrorMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    public IContextInformationValidator getContextInformationValidator() {
        // TODO Auto-generated method stub
        return null;
    }

}