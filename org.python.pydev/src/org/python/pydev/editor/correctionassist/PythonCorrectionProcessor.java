/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PyBackspace;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionProposal;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ClassNode;
import org.python.pydev.editor.model.FunctionNode;
import org.python.pydev.editor.model.ModelUtils;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

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

        PySelection ps = new PySelection(edit, false);

        List results = new ArrayList();
        String sel = getLine(ps);
        
        
        //at least some text must be selected 
        if(ps.textSelection.getLength() > 0){
            
	        try {
	            results.addAll(getTryProps(ps));
	        } catch (BadLocationException e) {
	        }
        
	        
        }else if(sel.indexOf("import ") != -1){
            //import 
            
	        try {
	            results.addAll(getMoveImports(ps));
	        } catch (BadLocationException e1) {
	        }

        }else if (sel.indexOf("class ") != -1){
            
            try {
                results.addAll(getClassProps(ps));
	        } catch (BadLocationException e) {
	        }
            
            
        }else if (sel.indexOf("def ") != -1){
            
            try {
                results.addAll(getOverrideProps(ps));
	        } catch (BadLocationException e) {
	        }
            
            
        }else {
            
            try {
                results.addAll(getAssignToResults(ps));
	        } catch (BadLocationException e) {
	        }
	        
	        try {
	            results.addAll(getCreations(ps));
	        } catch (BadLocationException e1) {
	        }

        }

    
        return (ICompletionProposal[]) results.toArray(new ICompletionProposal[0]);
    }

    
    
    /**
     * @param ps
     * @return
     */
    private List getOverrideProps(PySelection ps) throws BadLocationException {
        ArrayList l = new ArrayList();
        String sel = getLine(ps);
        int j = sel.indexOf("def ");
        
        String indentation = PyBackspace.getStaticIndentationString();
        String delimiter = PyAction.getDelimiter(ps.doc, 0);
        String indStart = "";

        for (int i = 0; i < j; i++) {
            indStart += " ";
        }
        
        String start = sel.substring(0, j+4);
        
        String[] strs = PyCodeCompletion.getActivationTokenAndQual(ps.doc, ps.absoluteCursorOffset);
        String tok = strs[1];
        
        
        CompletionState state = new CompletionState(ps.startLineIndex, ps.absoluteCursorOffset - ps.startLine.getOffset(), null, edit.getPythonNature());
        CompletionRequest request = new CompletionRequest(edit, ps.doc, "self", ps.absoluteCursorOffset, 0, new PyCodeCompletion(true), "");
        IToken[] selfCompletions = PyCodeCompletion.getSelfCompletions(request, new ArrayList(), state, true);
        for (int i = 0; i < selfCompletions.length; i++) {
            IToken token = selfCompletions[i];
            String rep = token.getRepresentation();
            if(rep.startsWith(tok)){
		        StringBuffer buffer = new StringBuffer( start );
                buffer.append(rep);

                String args = token.getArgs();
	            if(args.equals("()")){
	                args = "( self )";
	            }
		        buffer.append(args);
                
                buffer.append(":");
                buffer.append(delimiter);
                
                buffer.append(indStart);
                buffer.append(indentation);
                buffer.append("'''");
                buffer.append(delimiter);

                buffer.append(indStart);
                buffer.append(indentation);
		        buffer.append("@see super method: "+rep);
                buffer.append(delimiter);

                buffer.append(indStart);
                buffer.append(indentation);
                buffer.append("'''");
                buffer.append(delimiter);
                
                buffer.append(indStart);
                buffer.append(indentation);
                
                String comp = buffer.toString();

                System.out.println("comp ="+comp);
                l.add(new CompletionProposal(comp, ps.startLine.getOffset(), ps.startLine.getLength(), comp.length() , imageCache.get(UIConstants.ASSIST_NEW_CLASS),
                        rep+" (Override)", null, null));
            }
        }
        
        return l;
    }

    /**
     * @param ps
     * @return
     */
    private List getClassProps(PySelection ps) throws BadLocationException {
        ArrayList l = new ArrayList();

        String indentation = PyBackspace.getStaticIndentationString();
        String delimiter = PyAction.getDelimiter(ps.doc, 0);
        
        
        String sel = getLine(ps);
        int beg = sel.indexOf('(');
        int end = sel.indexOf(')');
        
        if(beg != -1 && end != -1){
            sel = sel.substring(beg+1, end);
        }
        
        StringBuffer buffer = new StringBuffer();
        buffer.append(delimiter);
        
        StringTokenizer tokenizer = new StringTokenizer(sel);
        while (tokenizer.hasMoreTokens()) {
            String cl = tokenizer.nextToken();
            CompletionState completionState = new CompletionState(ps.cursorLine, ps.absoluteCursorOffset - ps.startLine.getOffset(), cl, edit.getPythonNature());;
            IToken[] tokens = edit.getPythonNature().getAstManager().getCompletionsForToken(edit.getEditorFile(), ps.doc, completionState);
            
            //ok, now that we have the tokens, we have to discover which ones are methods...
            for (int i = 0; i < tokens.length; i++) {
                IToken token = tokens[i];
                if(token.getType() == PyCodeCompletion.TYPE_FUNCTION){
                    String rep = token.getRepresentation();
                    if(rep.startsWith("_") == false){
				        buffer.append(delimiter);
				        buffer.append(indentation);
				        buffer.append("def ");
				        buffer.append(rep);

				        String args = token.getArgs();
			            if(args.equals("()")){
			                args = "( self )";
			            }
				        buffer.append(args);
				        buffer.append(":");
				        buffer.append(delimiter);
				        buffer.append(indentation);
				        buffer.append(indentation);
				        buffer.append("'''");

				        buffer.append(delimiter);
				        buffer.append(indentation);
				        buffer.append(indentation);
				        buffer.append("@see "+sel+"."+rep);
				        
				        buffer.append(delimiter);
				        buffer.append(indentation);
				        buffer.append(indentation);
				        buffer.append("'''");
				        buffer.append(delimiter);
                    }
                }
            }
            int offset = ps.startLine.getOffset()+ps.startLine.getLength();
            l.add(new CompletionProposal(buffer.toString(), offset, 0, 0 , imageCache.get(UIConstants.ASSIST_NEW_CLASS),
                    "Implement super public interface", null, null));
        }
        return l;
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

        l.add(new CompletionProposal(except, start, end-start, excRelNewPos, imageCache.get(UIConstants.ASSIST_TRY_EXCEPT),
                "Surround with try..except", null, null));
        
        l.add(new CompletionProposal(finall, start, end-start, finRelNewPos, imageCache.get(UIConstants.ASSIST_TRY_FINNALLY),
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
        
        int lineToMoveImport = 0;
        int lines = ps.doc.getNumberOfLines();
        for (int line = 0; line < lines; line++) {
            String str = ps.getLine(line);
            if(str.startsWith("import ") || str.startsWith("from ")){
                lineToMoveImport = line;
                break;
            }
        }
        
        int offset = ps.doc.getLineOffset(lineToMoveImport);
        
        
        if(i >= 0){
            l.add(new FixCompletionProposal(sel+delimiter, offset, 0, ps.startLine.getOffset(), imageCache.get(UIConstants.ASSIST_MOVE_IMPORT),
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
            
            l.add(new FixCompletionProposal(cls, newPos, 0, cls.length()+1, imageCache.get(UIConstants.ASSIST_NEW_CLASS),
                    "Make this a new class", null, null, ps.startLineIndex+4));

            method = method.replaceFirst("%s", "");
            l.add(new FixCompletionProposal(method, newPos, 0, method.length()+1, imageCache.get(UIConstants.ASSIST_NEW_METHOD),
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
	                
		            l.add(new CompletionProposal(method, newPos, 0, method.length()-4, imageCache.get(UIConstants.ASSIST_NEW_METHOD),
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
	                String finalMethod = method+delim;
	                
	                finalMethod = finalMethod.replaceFirst("%s", "" );
	                
		            l.add(new CompletionProposal(finalMethod, newPos, 0, finalMethod.length(), imageCache.get(UIConstants.ASSIST_NEW_METHOD),
		                    "Create new method (in global context)", null, null));

	                cls = cls+delim;

	                l.add(new CompletionProposal(cls, newPos, 0, cls.length(), imageCache.get(UIConstants.ASSIST_NEW_CLASS),
		                    "Create new class (in global context)", null, null));
	                

	                
	                //now get the attempt to make this a new method ------------------------------------------
	                newPos = 0;
	                lineOfOffset = ps.doc.getLineOfOffset(ps.absoluteCursorOffset);
	                
	                if(lineOfOffset > 0){
	                    newPos = ps.doc.getLineInformation(lineOfOffset).getOffset();
	                }

	                int col = firstCharPosition;
	                String newIndent = indentation;
	                
	                while(newIndent.length() <= col){ //one more...
	                    newIndent += indentation;
	                }
	                String atStart = newIndent.replaceFirst(indentation, "");
	                method = method.replaceAll(indentation, newIndent);
	                method = atStart+method+delim;

	                if(params.trim().length() > 0){
	                    method = method.replaceFirst("%s", "self, ");
	                }else{
	                    method = method.replaceFirst("%s", "self");
	                }
	                l.add(new FixCompletionProposal(method, newPos, 0, method.length()+1, imageCache.get(UIConstants.ASSIST_NEW_METHOD),
	                        "Make this a new method (in class)", null, null, ps.startLineIndex+3));

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
                callName = lowerChar(callName, 0);
                if (callName.startsWith("get") && callName.length() > 3){
                    callName = callName.substring(3);
	                callName = lowerChar(callName, 0);
                }
            }else{
                callName = "result";
            }

            int firstCharPosition = PyAction.getFirstCharPosition(ps.doc, ps.absoluteCursorOffset);
            callName += " = ";
            l.add(new CompletionProposal(callName, firstCharPosition, 0, 0, imageCache.get(UIConstants.ASSIST_ASSIGN_TO_LOCAL),
                    "Assign to new local variable", null, null));
            
            l.add(new CompletionProposal("self." + callName, firstCharPosition, 0, 5, imageCache.get(UIConstants.ASSIST_ASSIGN_TO_CLASS),
                    "Assign to new field", null, null));
        }
        return l;
    }
    
    private String lowerChar(String s, int pos){
        char[] ds = s.toCharArray(); 
        ds[pos] = (""+ds[pos]).toLowerCase().charAt(0);
        return new String(ds);
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