/*
 * Created on Mar 29, 2004
 *
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

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
     * Edit.
     */
    private PyEdit edit;

    /**
     * Some error...
     */
    private Throwable error;



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
        ICompletionProposal[] proposals;
        
        try {
            //FIRST: discover activation token and qualifier.
            IDocument doc = viewer.getDocument();


            //list for storing the proposals
            ArrayList pythonAndTemplateProposals = new ArrayList();
            
            
            
            //SECOND: getting code completions and deciding if templates should be shown too.
            boolean showTemplates = true;
            //Get code completion proposals
            if(PyCodeCompletionPreferencesPage.useCodeCompletion()){
	            try {
                    //change the dir in the shell (so that we can ask for relative imports)... this might or not be used,
                    //but we have to do it before the completion process begins
	                AbstractShell.getServerShell(edit.getPythonNature(), AbstractShell.COMPLETION_SHELL).sendGoToDirMsg(edit.getEditorFile());
	            } catch (Exception e) {
	                //if we don't suceed, we don't have to fail... just go on and try
	                // to complete...
	            	PydevPlugin.log(e);
	            }
	
                Object[] objects = new Object[]{new ArrayList(), new Boolean(true)};
                try {
                    objects = getPythonProposals(viewer, documentOffset, doc);
                } catch (CompletionRecursionException e) {
                    //thats ok
                } catch (Throwable e) {
                    setError(e);
                }

                List pythonProposals = (List) objects[0];
                showTemplates = ((Boolean)objects[1]).booleanValue();
                pythonAndTemplateProposals.addAll(pythonProposals);
            }
            
            
            
            
            String[] strs = PySelection.getActivationTokenAndQual(doc, documentOffset); 

            String activationToken = strs[0];
            String qualifier = strs[1];

            
            //THIRD: Get template proposals (if asked for)
            if(showTemplates && (activationToken == null || activationToken.trim().length() == 0)){
                List templateProposals = getTemplateProposals(viewer, documentOffset, activationToken, qualifier);
                pythonAndTemplateProposals.addAll(templateProposals);
            }

            
            
            proposals = codeCompletion.onlyValidSorted(pythonAndTemplateProposals, qualifier);
            // Return the proposals
        } catch (RuntimeException e) {
            proposals = new ICompletionProposal[0];
            setError(e);
        }
        
        return proposals;
    }

    
    
    


    /**
     * @param e
     */
    private void setError(Throwable e) {
        this.error = e;
        PydevPlugin.log(e);
    }


    /**
     * Returns the python proposals as a list.
     * First parameter of tuple is a list and second is a Boolean object indicating whether the templates
     * should be also shown or not. 
     * @param viewer 
     * @throws CoreException
     * @throws BadLocationException
     */
    private Object[] getPythonProposals(ITextViewer viewer, int documentOffset, IDocument doc) throws CoreException, BadLocationException {
        CompletionRequest request = new CompletionRequest(edit.getEditorFile(), 
                edit.getPythonNature(), doc, documentOffset,
                codeCompletion);
        boolean showTemplates = true;
        
        //if non empty string, we're in imports section.
        String importsTipperStr = request.codeCompletion.getImportsTipperStr(request);
        
        if (importsTipperStr.length() != 0){
            showTemplates = false; //don't show templates if we are in the imports section.
        }
        
        List allProposals = request.codeCompletion.getCodeCompletionProposals(viewer, request);
        return new Object[]{allProposals, new Boolean(showTemplates)};
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
        String msg = null;
        if(this.error != null){
            msg = this.error.getMessage();
            this.error = null;
        }
        return msg;
    }

    /**
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

}