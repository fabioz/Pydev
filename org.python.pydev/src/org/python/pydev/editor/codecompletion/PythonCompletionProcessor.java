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
public class PythonCompletionProcessor 
        implements IContentAssistProcessor {
    
    private PyTemplateCompletion templatesCompletion = new PyTemplateCompletion();
    private PyCodeCompletion codeCompletion = new PyCodeCompletion();
    private CompletionCache completionCache = new CompletionCache();


    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int documentOffset) {
        List propList = new ArrayList();
        IDocument doc = viewer.getDocument();

        Point selectedRange = viewer.getSelectedRange();
        // there may not be a selected range
        java.lang.String theDoc = doc.get();
        codeCompletion.calcDocBoundary(theDoc, documentOffset);
        
        String activationToken = codeCompletion
                .getActivationToken(theDoc, documentOffset);
        
        java.lang.String qualifier = "";

        while(activationToken.endsWith(".") == false && activationToken.length() > 0){
            qualifier = activationToken.charAt(activationToken.length()-1) + qualifier;
            activationToken = activationToken.substring(0, activationToken.length()-1);
        }

        theDoc = codeCompletion.partialDocument(theDoc, documentOffset);
        
        
        int qlen = qualifier.length();
        theDoc += "\n" + activationToken;

        List allProposals = this.completionCache.getAllProposals(theDoc, activationToken, documentOffset, qlen, codeCompletion);

        //templates proposals are added here.
        this.templatesCompletion.addTemplateProposals(viewer, documentOffset, propList);

        for (Iterator iter = allProposals.iterator(); iter.hasNext();) {
            ICompletionProposal proposal = (ICompletionProposal) iter.next();
            if(proposal.getDisplayString().startsWith(qualifier)){
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
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] { '.'/*, '(', '['*/ };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    public char[] getContextInformationAutoActivationCharacters() {
        // is this _really_ what we want to use??
        return new char[] { '.' };
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