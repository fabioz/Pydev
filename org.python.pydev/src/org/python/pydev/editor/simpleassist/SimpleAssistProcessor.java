/*
 * Created on 24/09/2005
 */
package org.python.pydev.editor.simpleassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;

public class SimpleAssistProcessor implements IContentAssistProcessor {
    public static final char[] ALL_ASCII_CHARS = new char[]{
            'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
        };

    private PyEdit edit;

    public SimpleAssistProcessor(PyEdit edit){
        this.edit = edit;
    }

    /**
     * Computes the simple proposals
     *  
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        IDocument doc = viewer.getDocument();
        String[] strs = PyCodeCompletion.getActivationTokenAndQual(doc, offset); 

        String activationToken = strs[0];
        String qualifier = strs[1];

        PySelection ps = new PySelection(edit);
        List<ICompletionProposal> results = new ArrayList<ICompletionProposal>();

        List<ISimpleAssistParticipant> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_SIMPLE_ASSIST);
        
        for (ISimpleAssistParticipant participant : participants) {
            results.addAll(participant.computeCompletionProposals(activationToken, qualifier, ps, edit, offset));
        }
        
        Collections.sort(results, PyCodeCompletion.PROPOSAL_COMPARATOR);
        return (ICompletionProposal[]) results.toArray(new ICompletionProposal[0]);
    }

    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    /**
     * only very simple proposals should be here, as it is auto-activated for any character
     *  
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        return ALL_ASCII_CHARS;
    }

    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    public String getErrorMessage() {
        return null;
    }

    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

}
