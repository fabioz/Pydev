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
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ClassNode;
import org.python.pydev.editor.model.FunctionNode;
import org.python.pydev.editor.model.ModelUtils;

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

        List results = new ArrayList(); 
        try {
            results.addAll(getAssignToResults(ps));
        } catch (BadLocationException e) {
        }
        
        try {
            results.addAll(getCreations(ps));
        } catch (BadLocationException e1) {
        }

        return (ICompletionProposal[]) results.toArray(new ICompletionProposal[0]);
    }

    /**
     * @param ps
     * @return
     * @throws BadLocationException
     */
    private List getCreations(PySelection ps) throws BadLocationException {
        List l = new ArrayList();

        if (ps.selection.trim().length() == 0) {
            return l;
        }

        //here, any callable can be used to create a new method or class.
        //we have to parse it, because we have to discover the parameters for the new class or method.
        
        String callName = getBeforeParentesisTok(ps);

        if(callName.length() == 0){
            return l;
        }

        String params = getInsideParentesisTok(ps);
        int firstCharPosition = PyAction.getFirstCharRelativePosition(ps.doc, ps.absoluteCursorOffset);
        
        String indentation = PyBackspace.getStaticIndentationString();

        
        String delim = PyAction.getDelimiter(ps.doc, 0);
        String cls = "class "+callName+":"+delim+delim;
        
        String self = "self";
        if(params.trim().length() != 0){
            self += ", ";
        }
        
        cls += indentation+"def __init__("+self+params+"):"+delim;
        cls += indentation+indentation+"pass"+delim;
        
        String method = "def "+callName+"(%s"+params+"):"+delim+indentation+"pass"+delim;

        if (firstCharPosition == 0){ //we are in the global context

            int newPos = 0;
            int lineOfOffset = ps.doc.getLineOfOffset(ps.absoluteCursorOffset);
            
            if(lineOfOffset > 0){
                newPos = ps.doc.getLineInformation(lineOfOffset - 1).getOffset();
            }
            
            l.add(new CompletionProposal(cls, newPos, 0, cls.length()+1, null,
                    "Create new class (global context)", null, null));

            method = method.replaceFirst("%s", "");
            l.add(new CompletionProposal(method, newPos, 0, method.length()+1, null,
                    "Create new method (global context)", null, null));

        }else{ //we are in a method or class context

	        AbstractNode root = edit.getPythonModel();
	        if (root == null){
	            return l;
	        }
	        
	        //now, discover in which node we are right now...
	        AbstractNode current = ModelUtils.getLessOrEqualNode(root, ps.absoluteCursorOffset, ps.doc);
	        while (current != null) {
	            if (current instanceof FunctionNode
	                    || current instanceof ClassNode) {
	                break;
	            }
	            current = ModelUtils.getPreviousNode(current);
	        }
	        
	        
	        if(ps.selection.indexOf("self.") != -1){ //we are going for a class method.
	            
	            if (current instanceof FunctionNode) { //if we are in a class, here we are within a method.
	                FunctionNode node = (FunctionNode) current;
	                
	                int newPos = 0;
	                int lineOfOffset = node.getStart().line;
	                
	                if(lineOfOffset > 0){
	                    newPos = ps.doc.getLineInformation(lineOfOffset).getOffset();
	                }
	                
	                int col = node.getStart().column;
	                String newIndent = indentation;
	                
	                while(newIndent.length() < col){
	                    newIndent += indentation;
	                }
	                String atStart = newIndent.replaceFirst(indentation, "");
	                method = method.replaceAll(indentation, newIndent);
	                method = atStart+method+delim;
	                
	                method = method.replaceFirst("%s", self);
	                
		            l.add(new CompletionProposal(method, newPos, 0, method.length()-4, null,
		                    "Create new method (in class)", null, null));
	            }
	            
	        }else{ //we are going for a class or a global method.
	            //TODO: End this.
//	            l.add(new CompletionProposal(callName, 0, 0, 0, null,
//	                    "Create new class", null, null));
//	            
//	            l.add(new CompletionProposal(callName, 0, 0, 0, null,
//	                    "Create new method", null, null));
	        }
        }        
        return l;
    }


    /**
     * @param ps
     * @throws BadLocationException
     */
    private List getAssignToResults(PySelection ps) throws BadLocationException {
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

            String callName = getBeforeParentesisTok(ps);

            if(callName.length() > 0){
                //all that just to change first char to lower case.
                char[] ds = callName.toCharArray(); 
                ds[0] = (""+ds[0]).toLowerCase().charAt(0);
                callName = new String(ds);
            }else{
                callName = "result";
            }

            int firstCharPosition = PyAction.getFirstCharPosition(ps.doc, ps.absoluteCursorOffset);
            callName += " = ";
            l.add(new CompletionProposal(callName, firstCharPosition, 0, 0, null,
                    "Assign to new local variable", null, null));
            
            l.add(new CompletionProposal("self." + callName, firstCharPosition, 0, 5, null,
                    "Assign to new field", null, null));
        }
        return l;
    }

    /**
     * @param ps
     * @return
     */
    private String getInsideParentesisTok(PySelection ps) {
        int beg = ps.selection.indexOf('(')+1;
        int end = ps.selection.indexOf(')');
        return ps.selection.substring(beg, end);
    }

    /**
     * @param ps
     * @return string with the token or empty token if not found.
     */
    private String getBeforeParentesisTok(PySelection ps) {
        String string = ps.selection.replaceAll("\\(.*\\)", "()");

        int i;

        String callName = "";
        if ((i = string.indexOf("()")) != -1) {
            callName = "";

            for (int j = i-1; j >= 0 && stillInTok(string, j); j--) {
                callName = string.charAt(j) + callName;
            }
            
        }
        return callName;
    }

    /**
     * @param string
     * @param j
     * @return
     */
    private boolean stillInTok(String string, int j) {
        char c = string.charAt(j);

        return c != '\n' && c != '\r' && c != ' ' && c != '.' && c != '(' && c != ')' && c != ',' && c != ']' && c != '[';
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