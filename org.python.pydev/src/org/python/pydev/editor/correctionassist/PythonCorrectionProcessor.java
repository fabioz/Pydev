/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;

/**
 * /** This class should be used to give context help
 *  - Help depending on context (Ctrl+1):
 * 
 * class A: pass
 * 
 * class C:
 * 
 * def __init__(self, param): self.newMethod() <- create new method on class C <-
 * assign result to new local variable <- assign result to new field a = A()
 * a.newMethod() <- create new method on class A <- assign result to new local
 * variable <- assign result to new field
 * 
 * param. <- don't show anything.
 * 
 * self.a1 = A() self.a1.newMethod() <- create new method on class A <- assign
 * result to new local variable <- assign result to new field
 * 
 * def m(self): self.a1.newMethod() <- create new method on class A <- assign
 * result to new local variable <- assign result to new field
 * 
 * 
 * @author Fabio Zadrozny
 */
public class PythonCorrectionProcessor implements IContentAssistProcessor {

    private PyEdit edit;

    /**
     * @param edit
     */
    public PythonCorrectionProcessor(PyEdit edit) {
        this.edit = edit;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {

        PySelection ps = new PySelection(edit, false);

        List results = getAssignToResults(ps);
        results.addAll(getCreations(ps));

        return (ICompletionProposal[]) results.toArray(new ICompletionProposal[0]);
    }

    /**
     * @param ps
     * @return
     */
    private List getCreations(PySelection ps) {
        List l = new ArrayList();

        if (ps.selection.trim().length() == 0) {
            return l;
        }

        //here, any callable can be used to create a new method or class.
        //we have to parse it, because we have to discover the parameters for the new class.
        
        
        return l;
    }

    /**
     * @param ps
     */
    private List getAssignToResults(PySelection ps) {
        List l = new ArrayList();

        if (ps.selection.trim().length() == 0) {
            return l;
        }

        //first thing: check were we are and check that no single '=' exists.
        // '==' may happen.
        if (ps.selection.replaceAll("==", "").indexOf("=") == -1) {

            //second: go on and make the suggestion.
            //
            //if we have a method call, eg.:
            //  e.methodCall()| would result in the following suggestions:
            //
            //                   methodCall = e.methodCall()
            //					 self.methodCall = e.methodCall()
            //
            // NewClass()| would result in
            //
            //                   newClass = NewClass()
            //					 self.newClass = NewClass()
            //
            //now, if we don't have a method call, eg.:
            // 1+1| would result in
            //
            //					 |result| = 1+1
            //					 self.|result| = 1+1

            String string = ps.selection.replaceAll("\\(*\\)", "()");

            try {
                int firstCharPosition = PyAction.getFirstCharPosition(ps.doc, ps.absoluteCursorOffset);
                int i;

                String callName = "result";
                if ((i = string.indexOf("()")) != -1) {
                    callName = "";

                    for (int j = i-1; j >= 0 && stillInTok(string, j); j--) {
                        callName = string.charAt(j) + callName;
                    }
                    
                    if(callName.length()>0){
	                    //all that just to change first char to lower case.
	                    char[] ds = callName.toCharArray(); 
	                    ds[0] = (""+ds[0]).toLowerCase().charAt(0);
	                    callName = new String(ds);
                    }
                }

                callName += " = ";
                l.add(new CompletionProposal(callName, firstCharPosition, 0, 0, null,
                        "Assign to new local variable", null, null));
                
                l.add(new CompletionProposal("self." + callName, firstCharPosition, 0, 0, null,
                        "Assign to new field", null, null));
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        return l;
    }

    /**
     * @param string
     * @param j
     * @return
     */
    private boolean stillInTok(String string, int j) {
        char c = string.charAt(j);

        return c != '\n' && c != '\r' && c != ' ' && c != '.' && c != '(' && c != ')';
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    public char[] getContextInformationAutoActivationCharacters() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    public String getErrorMessage() {
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