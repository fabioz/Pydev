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
        String sel = getLine(ps);
        
        
        //at least some text must be selected 
        if(ps.textSelection.getLength() > 0){
            
	        try {
	            results.addAll(getTryProps(ps));
	        } catch (BadLocationException e) {
	        }
        } else if(sel.indexOf("import") == -1){

            try {
                results.addAll(getAssignToResults(ps));
	        } catch (BadLocationException e) {
	        }
	        
	        try {
	            results.addAll(getCreations(ps));
	        } catch (BadLocationException e1) {
	        }
        }else{

	        try {
	            results.addAll(getMoveImports(ps));
	        } catch (BadLocationException e1) {
	        }
        }

    
        return (ICompletionProposal[]) results.toArray(new ICompletionProposal[0]);
    }

    
    
    /**
     * @param ps
     * @return
     */
    private List getTryProps(PySelection ps)  throws BadLocationException {
        ArrayList l = new ArrayList();
        String indentation = PyBackspace.getStaticIndentationString();
        
        int start = ps.startLine.getOffset();
        int end = ps.endLine.getOffset()+ps.endLine.getLength();
        
        String string = ps.doc.get(start, end-start);
        String delimiter = PyAction.getDelimiter(ps.doc, 0);
        
        int firstCharPosition = PyAction.getFirstCharRelativePosition(ps.doc, start);
        String startIndent = "";
        int i = 0;
        while(i < firstCharPosition){
            startIndent += " ";
            i++;
        }
        
        int finRelNewPos;
        int excRelNewPos;
        string = indentation+ string.replaceAll(delimiter, delimiter+indentation);
        String except = startIndent+"try:"+delimiter+string+delimiter;
        except += startIndent+"except:"+delimiter;
        excRelNewPos = except.length() - delimiter.length() -1;
        except += startIndent+indentation+"raise";

        String finall = startIndent+"try:"+delimiter+string+delimiter;
        finall += startIndent+"finally:"+delimiter;
        finall += startIndent+indentation;
        finRelNewPos = finall.length();
        finall += "pass";

        l.add(new CompletionProposal(except, start, end-start, excRelNewPos, null,
                "Surround with try..except", null, null));
        
        l.add(new CompletionProposal(finall, start, end-start, finRelNewPos, null,
                "Surround with try..finally", null, null));

        return l;
    }

    /**
     * @param ps
     * @return
     */
    private List getMoveImports(PySelection ps) throws BadLocationException {
        ArrayList l = new ArrayList();
        String sel = getLine(ps).trim();

        int i = sel.indexOf("import");
        if(ps.startLineIndex != ps.endLineIndex)
            return l;
        
        
        String delimiter = PyAction.getDelimiter(ps.doc, 0);
        
        if(i != -1){
            l.add(new FixCompletionProposal(sel+delimiter, 0, 0, ps.startLine.getOffset(), null,
                    "Move import to global scope", null, null, ps.startLineIndex+1));
        }
        return l;
    }

    /**
     * @param ps
     * @return
     * @throws BadLocationException
     */
    private List getCreations(PySelection ps) throws BadLocationException {
        List l = new ArrayList();
        String sel = getLine(ps);


        if (sel.trim().length() == 0) {
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
        String cls = "class "+callName+"(object):"+delim+delim;
        
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
                newPos = ps.doc.getLineInformation(lineOfOffset).getOffset();
            }
            
            l.add(new FixCompletionProposal(cls, newPos, 0, cls.length()+1, null,
                    "Make this a new class", null, null, ps.startLineIndex+4));

            method = method.replaceFirst("%s", "");
            l.add(new FixCompletionProposal(method, newPos, 0, method.length()+1, null,
                    "Make this a new method", null, null, ps.startLineIndex+2));

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
	        
	        
	        if(sel.indexOf("self.") != -1){ //we are going for a class method.
	            
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
		        
	            while (current != null) {
	                if(current instanceof ClassNode) {
		                break;
		            }
		            current = ModelUtils.getPreviousNode(current);
		        }
	            if(current instanceof ClassNode){
	                ClassNode node = (ClassNode) current;
	                
	                int newPos = 0;
	                int lineOfOffset = node.getStart().line;
	                
	                if(lineOfOffset > 0){
	                    newPos = ps.doc.getLineInformation(lineOfOffset).getOffset();
	                }
	                method = method+delim;
	                
	                method = method.replaceFirst("%s", "");
	                
		            l.add(new CompletionProposal(method, newPos, 0, method.length(), null,
		                    "Create new method (in global context)", null, null));

	                cls = cls+delim;

	                l.add(new CompletionProposal(cls, newPos, 0, cls.length(), null,
		                    "Create new class (in global context)", null, null));
	            }
	        }
        }        
        return l;
    }


    /**
     * 
     */
    private String getLine(PySelection ps) {
        return ps.selection.replaceAll("#.*", "");
    }
    
    /**
     * @param ps
     * @throws BadLocationException
     */
    private List getAssignToResults(PySelection ps) throws BadLocationException {
        List l = new ArrayList();
        String sel = getLine(ps);
        if (sel.trim().length() == 0) {
            return l;
        }

        //first thing: check were we are and check that no single '=' exists.
        // '==' may happen.
        if (sel.replaceAll("==", "").indexOf("=") == -1) {

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

            String callName = getTokToAssign(ps);

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
    private String getTokToAssign(PySelection ps) {
        String beforeParentesisTok = getBeforeParentesisTok(ps);
        if(beforeParentesisTok.length() > 0){
            return beforeParentesisTok;
        }
        //otherwise, try to find . (ignore code after #)
        String string = getLine(ps);
        String callName = "";
        //get parentesis position and go backwards

        int i;
        if ((i = string.lastIndexOf(".")) != -1) {
            callName = "";

            for (int j = i+1; j < string.length() && stillInTok(string, j); j++) {
                callName += string.charAt(j);
            }
        }
        return callName;
    }

    /**
     * @param ps
     * @return
     */
    private String getInsideParentesisTok(PySelection ps) {
        String sel = getLine(ps);

        int beg = sel.indexOf('(')+1;
        int end = sel.indexOf(')');
        return sel.substring(beg, end);
    }

    /**
     * @param ps
     * @return string with the token or empty token if not found.
     */
    private String getBeforeParentesisTok(PySelection ps) {
        String string = getLine(ps);

        int i;

        String callName = "";
        //get parentesis position and go backwards
        if ((i = string.indexOf("(")) != -1) {
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

        return c != '\n' && c != '\r' && c != ' ' && c != '.' && c != '(' && c != ')' && c != ',' && c != ']' && c != '[' && c != '#';
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