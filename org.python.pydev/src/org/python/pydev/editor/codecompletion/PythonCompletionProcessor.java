/*
 * Created on Mar 29, 2004
 *
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.editor.PyEdit;

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

    private PyEdit edit;

    private ProposalsComparator proposalsComparator = new ProposalsComparator();

    /**
     * @param edit
     */
    public PythonCompletionProcessor(PyEdit edit) {
        this.edit = edit;
    }

    private boolean endsWithSomeChar(char cs[], String activationToken) {
        for (int i = 0; i < cs.length; i++) {
            if (activationToken.endsWith(cs[i] + "")) {
                return true;
            }
        }
        return false;

    }

    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {

        IDocument doc = viewer.getDocument();

        Point selectedRange = viewer.getSelectedRange();
        // there may not be a selected range
        java.lang.String completeDoc = doc.get();
        codeCompletion.calcDocBoundary(completeDoc, documentOffset);

        String activationToken = codeCompletion.getActivationToken(completeDoc, documentOffset);

        java.lang.String qualifier = "";

        //we complete on '.' and '('.
        //' ' gets globals
        //and any other char gets globals on token and templates.

        //we have to get the qualifier. e.g. bla.foo = foo is the qualifier.
        if (activationToken.indexOf('.') != -1) {
            while (endsWithSomeChar(new char[] { '.' }, activationToken) == false
                    && activationToken.length() > 0) {

                qualifier = activationToken.charAt(activationToken.length() - 1) + qualifier;
                activationToken = activationToken.substring(0, activationToken.length() - 1);
            }
        } else { //everything is a part of the qualifier.
            qualifier = activationToken.trim();
            activationToken = "";
        }

        int qlen = qualifier.length();

        try {
            PythonShell.getServerShell().sendGoToDirMsg(edit.getEditorFile());
        } catch (Exception e) {
            //if we don't suceed, we don't have to fail... just go on and try
            // to complete...
            e.printStackTrace();
        }

        List pythonProposals = getPythonProposals(documentOffset, doc, activationToken, qlen);
        List templateProposals = getTemplateProposals(viewer, documentOffset, activationToken, qualifier,
                pythonProposals);

        ArrayList pythonAndTemplateProposals = new ArrayList();
        pythonAndTemplateProposals.addAll(pythonProposals);
        pythonAndTemplateProposals.addAll(templateProposals);

        ArrayList returnProposals = new ArrayList();

        for (Iterator iter = pythonAndTemplateProposals.iterator(); iter.hasNext();) {
            ICompletionProposal proposal = (ICompletionProposal) iter.next();
            if (proposal.getDisplayString().startsWith(qualifier)) {
                returnProposals.add(proposal);
            }
        }

        ICompletionProposal[] proposals = new ICompletionProposal[returnProposals.size()];

        // and fill with list elements
        returnProposals.toArray(proposals);

        Arrays.sort(proposals, proposalsComparator);
        // Return the proposals
        return proposals;

    }

    /**
     * @param documentOffset
     * @param doc
     * @param theDoc
     * @param activationToken
     * @param qlen
     * @return
     */
    private List getPythonProposals(int documentOffset, IDocument doc, String activationToken, int qlen) {
        List allProposals = this.completionCache.getAllProposals(edit, doc, activationToken, documentOffset,
                qlen, codeCompletion);
        return allProposals;
    }

    /**
     * @param viewer
     * @param documentOffset
     * @param activationToken
     * @param qualifier
     * @param allProposals
     */
    private List getTemplateProposals(ITextViewer viewer, int documentOffset, String activationToken,
            java.lang.String qualifier, List allProposals) {
        List propList = new ArrayList();
        this.templatesCompletion.addTemplateProposals(viewer, documentOffset, propList);
        return propList;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
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