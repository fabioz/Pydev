/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.correctionassist.heuristics.AssistAssign;
import org.python.pydev.editor.correctionassist.heuristics.AssistDocString;
import org.python.pydev.editor.correctionassist.heuristics.AssistImport;
import org.python.pydev.editor.correctionassist.heuristics.AssistTry;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * This class should be used to give context help
 * 
 * Help depending on context (Ctrl+1):
 * 
 * class A: pass
 * 
 * class C:
 * 
 * def __init__(self, param): 
 * 	    self.newMethod()<- create new method on class C  (with params if needed)
 * 						<- assign result to new local variable 
 * 						<- assign result to new field 
 * 
 * 		a = A()
 * 		a.newMethod()   <- create new method on class A 
 * 						<- assign result to new local variable 
 * 						<- assign result to new field
 * 
 * 		param.b() <- don't show anything.
 * 
 * 		self.a1 = A() 
 * 		self.a1.newMethod() <- create new method on class A (difficult part is discovering class)
 * 							<- assign result to new local variable 
 * 							<- assign result to new field
 * 
 * 		def m(self): 
 * 			self.a1.newMethod() <- create new method on class A 
 * 								<- assign result to new local variable 
 * 								<- assign result to new field
 * 
 * 			import compiler	<- move import to global context
 * 			NewClass() <- Create class NewClass (Depends on new class wizard)
 *
 * 	   a() <-- make this a new method in this class 
 *                       																				 
 * @author Fabio Zadrozny
 */
public class PythonCorrectionProcessor implements IContentAssistProcessor {

    private PyEdit edit;
    private ImageCache imageCache;

    /**
     * @param edit
     */
    public PythonCorrectionProcessor(PyEdit edit) {
        this.edit = edit;
        this.imageCache = new ImageCache(PydevPlugin.getDefault().getBundle().getEntry("/"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

        PySelection ps = new PySelection(edit);

        List<ICompletionProposal> results = new ArrayList<ICompletionProposal>();
        String sel = PyAction.getLineWithoutComments(ps);
        
        
        List<IAssistProps> assists = new ArrayList<IAssistProps>();
        assists.add(new AssistTry());
        assists.add(new AssistImport());
        assists.add(new AssistDocString());
        assists.add(new AssistAssign());
        
        assists.addAll(ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_CTRL_1));
        
        for (IAssistProps assist : assists) {
            try {
                if (assist.isValid(ps, sel, edit, offset)) {
                    try {
                        results.addAll(
                                assist.getProps(
                                        ps, 
                                        imageCache, 
                                        edit.getEditorFile(), 
                                        (PythonNature)edit.getPythonNature(), 
                                        edit,
                                        offset)
                        );
                    } catch (BadLocationException e) {
                        PydevPlugin.log(e);
                    }
                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }

        Collections.sort(results, PyCodeCompletion.PROPOSAL_COMPARATOR);

        return (ICompletionProposal[]) results.toArray(new ICompletionProposal[0]);
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    public String getErrorMessage() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

}