/*
 * Created on Mar 29, 2004
 *
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.editor.PyEdit;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 * 
 * This class is responsible for code completion / template completion.
 */
public class PythonCompletionProcessor implements IContentAssistProcessor {

    /**
     * This makes the templates completion
     */
    private PyTemplateCompletion templatesCompletion = new PyTemplateCompletion();

    /**
     * This makes python code completion
     */
    private PyCodeCompletion codeCompletion = new PyCodeCompletion();

    /**
     * This is the cache, so that we can get it if we want it later.
     */
    private CompletionCache completionCache = new CompletionCache();

    /**
     * Edit.
     */
    private PyEdit edit;

    /**
     * Compares proposals so that we can order them.
     */
    private ProposalsComparator proposalsComparator = new ProposalsComparator();


    /**
     * @param edit
     */
    public PythonCompletionProcessor(PyEdit edit) {
        this.edit = edit;
    }


    /**
     * This is the interface implemented to get the completions.
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {

        try {
            //FIRST: discover activation token and qualifier.
            IDocument doc = viewer.getDocument();

            java.lang.String completeDoc = doc.get();
            
            String[] strs = codeCompletion.getActivationTokenAndQual(doc, documentOffset); 

            String activationToken = strs[0];
            String qualifier = strs[1];
            int qlen = qualifier.length();

            //list for storing the proposals
            ArrayList pythonAndTemplateProposals = new ArrayList();

            
            
            
            
            //SECOND: getting code completions and deciding if templates should be shown too.
            boolean showTemplates = true;
            //Get code completion proposals
            if(PyCodeCompletionPreferencesPage.useCodeCompletion()){
	            try {
	                PythonShell.getServerShell(PythonShell.COMPLETION_SHELL).sendGoToDirMsg(edit.getEditorFile());
	            } catch (Exception e) {
	                //if we don't suceed, we don't have to fail... just go on and try
	                // to complete...
	                e.printStackTrace();
	            }
	
	            try {
                    Object[] objects = getPythonProposals(documentOffset, doc, activationToken, qlen);
                    List pythonProposals = (List) objects[0];
                    showTemplates = ((Boolean)objects[1]).booleanValue();
                    pythonAndTemplateProposals.addAll(pythonProposals);
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
            
            
            
            
            
            //THIRD: Get template proposals (if asked for)
            if(showTemplates){
                List templateProposals = getTemplateProposals(viewer, documentOffset, activationToken, qualifier);
                pythonAndTemplateProposals.addAll(templateProposals);
            }

            
            
            
            
            
            
            //FOURTH: Now, we have all the proposals, only thing is deciding wich ones are valid (depending on
            //qualifier) and sorting them correctly.
            Collection returnProposals = new HashSet();
            String lowerCaseQualifier = qualifier.toLowerCase();
            
            for (Iterator iter = pythonAndTemplateProposals.iterator(); iter.hasNext();) {
                ICompletionProposal proposal = (ICompletionProposal) iter.next();
                if (proposal.getDisplayString().toLowerCase().startsWith(lowerCaseQualifier)) {
                    returnProposals.add(proposal);
                }
            }

            ICompletionProposal[] proposals = new ICompletionProposal[returnProposals.size()];

            // and fill with list elements
            returnProposals.toArray(proposals);

            Arrays.sort(proposals, proposalsComparator);
            // Return the proposals
            return proposals;
        } catch (CoreException e) {
            
            ErrorDialog.openError(null,"Error", "Error", e.getStatus());
        }
        return new ICompletionProposal[0]; //if error happens, return no completions.
    }

    
    
    
    /**
     * Returns the python proposals as a list.
     * First parameter of tuple is a list and second is a Boolean object indicating whether the templates
     * should be also shown or not. 
     * @throws CoreException
     * @throws BadLocationException
     */
    private Object[] getPythonProposals(int documentOffset, IDocument doc, String activationToken, int qlen) throws CoreException, BadLocationException {
        //we always ask the completions cache... even if it is not in the cache (cache takes care of asking them
        //and putting them in it for later calls).
        return  this.completionCache.getProposals(edit, doc, activationToken, documentOffset,
                qlen, codeCompletion);
    }

    
    
    /**
     * Returns the template proposals as a list.
     */
    private List getTemplateProposals(ITextViewer viewer, int documentOffset, String activationToken,
            java.lang.String qualifier) {
        List propList = new ArrayList();
        this.templatesCompletion.addTemplateProposals(viewer, documentOffset, propList);
        return propList;
    }

    /**
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
     */
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
        return null;
    }

    /**
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

    /**
     * Adds a char to an array of chars and returns the new array. 
     * 
     * @param c
     * @param toAdd
     * @return
     */
    private char[] addChar(char[] c, char toAdd) {
        char[] c1 = new char[c.length + 1];

        int i;

        for (i = 0; i < c.length; i++) {
            c1[i] = c[i];
        }
        c1[i] = toAdd;
        return c1;

    }

    /**
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    public char[] getContextInformationAutoActivationCharacters() {
        return new char[] {};
    }

    /**
     * If completion fails for some reason, we could give it here...
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    public java.lang.String getErrorMessage() {
        return null;
    }

    /**
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

}