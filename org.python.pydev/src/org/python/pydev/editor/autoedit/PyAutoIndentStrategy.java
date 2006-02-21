/*
 * Created on Dec 10, 2003
 * Author: atotic
 * License: Common Public License 1.0
 */

package org.python.pydev.editor.autoedit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.copiedfromeclipsesrc.PythonPairMatcher;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.parser.visitors.ParsingUtils;

/**
 * Class which implements the following behaviors:
 * - indenting: after 'class' or 'def' 
 * - replacement: when typing colons or parentheses
 * 
 * This class uses the org.python.pydev.core.docutils.DocUtils class extensively
 * for some document-related operations.
 */
public class PyAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {

    private IIndentPrefs prefs;

    public void setIndentPrefs(IIndentPrefs prefs) {
        this.prefs = prefs;
    }

    public IIndentPrefs getIndentPrefs() {
        if (this.prefs == null) {
            this.prefs = new DefaultIndentPrefs(); //create the default if this is still not done.
        }
        return this.prefs;
    }

    /**
     * Set indentation automatically after newline.
     */
    private String autoIndentNewline(IDocument document, int length, String text, int offset)
            throws BadLocationException {
        if (length == 0 && text != null && AbstractIndentPrefs.endsWithNewline(document, text)) {

            if (offset > 0) {
                PySelection selection = new PySelection(document, offset);
                String lineWithoutComments = PySelection.getLineWithoutComments(selection.getLineContentsToCursor());
                
                if(lineWithoutComments.length() > 0){
                    
                    
                    //ok, now let's see the auto-indent
                    int curr = lineWithoutComments.length() -1;
                    char lastChar = lineWithoutComments.charAt(curr);
    
                    //we dont want whitespaces
                    while (curr > 0 && Character.isWhitespace(lastChar)) {
                        curr--;
                        lastChar = lineWithoutComments.charAt(curr);
                    }
    
                    if (curr > 0) {
    
                        if (lastChar == ':') {
                            String initial = text;
    
                            text = initial + prefs.getIndentationString();
    
                        }else{
                            //ok, normal indent until now...
                            //let's check for dedents...
                            String trimmedLine = lineWithoutComments.trim();
                            
                            if(startsWithDedentToken(trimmedLine)){
                                text = dedent(text);
                            }else{
                            	text = smartIndentAfterPar(document, text, offset);
                            }
                        }
                    }
                    
                    
                    
                    
                }
            }
        }
        return text;
    }

    public static final String[] DEDENT_TOKENS = new String[]{
        "return",
        "break",
        "continue",
        "pass",
        "raise",
        "yield"
    };

    private boolean startsWithDedentToken(String trimmedLine) {
        for (String dedent : DEDENT_TOKENS) {
            if(trimmedLine.startsWith(dedent)){
                if(dedent.length() < trimmedLine.length()){
                    char afterToken = trimmedLine.charAt(dedent.length());
                    if(afterToken == ' ' || afterToken == ';' || afterToken == '('){
                        return true;
                    }
                    return false;
                }else{
                    return true;
                }
            }
        }
        return false;
    }

    private String dedent(String text) {
        String indentationString = prefs.getIndentationString();
        int indentationLength = indentationString.length();
        int len = text.length();

        if(len >= indentationLength){
            text = text.substring(0, len - indentationLength);
        }
        return text;
    }
    private Tuple<String, Integer> removeFirstIndent(String text) {
        String indentationString = prefs.getIndentationString();
        if(text.startsWith(indentationString)){
            return new Tuple<String, Integer>(text.substring(indentationString.length()), indentationString.length());
        }
        return new Tuple<String, Integer>(text, 0);
    }

    /**
     * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(IDocument, DocumentCommand)
     */
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        // super idents newlines the same amount as the previous line
        super.customizeDocumentCommand(document, command);

        try {
            command.text = autoIndentNewline(document, command.length, command.text, command.offset);
            getIndentPrefs().convertToStd(document, command);
            

        	if (prefs.getAutoParentesis() && (command.text.equals("[") || command.text.equals("{")) ) {
        		PySelection ps = new PySelection(document, command.offset);
        		char c = command.text.charAt(0);
        		if (shouldClose(ps, c)) {
        			command.shiftsCaret = false;
                    command.text = c+""+DocUtils.getPeer(c);
                    command.caretOffset = command.offset+1;
        		}
        		
        	}else if (command.text.equals("(")) {
            	/*
            	 * Now, let's also check if we are in an 'elif ' that must be dedented in the doc
            	 */
            	autoDedentElif(document, command);

            	if(prefs.getAutoParentesis()){
	                PySelection ps = new PySelection(document, command.offset);
	                String line = ps.getLine();
	
	                if (shouldClose(ps, '(')) {
	
	                    boolean hasClass = line.indexOf("class ") != -1;
	                    boolean hasClassMethodDef = line.indexOf(" def ") != -1 || line.indexOf("\tdef ") != -1;
	                    boolean hasMethodDef = line.indexOf("def ") != -1;
	                    boolean hasNoDoublePoint = line.indexOf(":") == -1;
	
	                    command.shiftsCaret = false;
	                    if (hasNoDoublePoint && (hasClass || hasClassMethodDef || hasMethodDef)) {
	                        if (hasClass) {
	                            //command.text = "(object):"; //TODO: put some option in the interface for that
	                            //command.caretOffset = command.offset + 7;
	                            command.text = "():";
	                            command.caretOffset = command.offset + 1;
	                        } else if (hasClassMethodDef && prefs.getAutoAddSelf()) {
	                            command.text = "(self):";
	                            command.caretOffset = command.offset + 5;
	                        } else if (hasMethodDef) {
	                            command.text = "():";
	                            command.caretOffset = command.offset + 1;
	                        } else {
	                            throw new RuntimeException(getClass().toString() + ": customizeDocumentCommand()");
	                        }
	                    } else {
	                        command.text = "()";
	                        command.caretOffset = command.offset + 1;
	                    }
	                }
        		}

            }
	            
            else if (command.text.equals(":")) {
                /*
                 * The following code will auto-replace colons in function
                 * declaractions
                 * e.g.,
                 * def something(self):
                 *                    ^ cursor before the end colon
                 * 
                 * Typing another colon (i.e, ':') at that position will not insert
                 * another colon
                 */
                if(prefs.getAutoColon()){
                    performColonReplacement(document, command);
                }

                /*
                 * Now, let's also check if we are in an 'else:' that must be dedented in the doc
                 */
                autoDedentElse(document, command);
            }

            /*
             * this is a space... so, if we are in 'from xxx ', we may auto-write
             * the import 
             */
            else if (command.text.equals(" ")) {
            	if( prefs.getAutoWriteImport()){
	                PySelection ps = new PySelection(document, command.offset);
	                String completeLine = ps.getLineWithoutComments();
	                String lineToCursor = ps.getLineContentsToCursor().trim();
	                if(completeLine.indexOf("import") == -1){
	                    String importsTipperStr = PyCodeCompletion.getImportsTipperStr(lineToCursor, false);
	                    if(importsTipperStr.length() > 0){
	                        command.text = " import ";
	                    }
	                }
            	}
            	
            	
            	/*
            	 * Now, let's also check if we are in an 'elif ' that must be dedented in the doc
            	 */
            	autoDedentElif(document, command);
            }
            
            /*
             * If the command is some kind of parentheses or brace, and there's
             * already a matching one, don't insert it. Just move the cursor to
             * the next space.
             */
            else if (command.text.length() > 0 && prefs.getAutoBraces()) {
                // you can only do the replacement if the next character already there is what the user is trying to input
                
                if (command.offset < document.getLength() && document.get(command.offset, 1).equals(command.text)) {
                    // the following searches through each of the end braces and
                    // sees if the command has one of them

                    boolean found = false;
                    for (int i = 1; i <= DocUtils.BRACKETS.length && !found; i += 2) {
                        char c = DocUtils.BRACKETS[i];
                        if (c == command.text.charAt(0)) {
                            found = true;
                            performPairReplacement(document, command);
                        }
                    }
                }
            }
            

        }
        /*
         * If something goes wrong, you want to know about it, especially in a
         * unit test. If you don't rethrow the exception, unit tests will pass
         * even though you threw an exception.
         */
        catch (BadLocationException e) {
            // screw up command.text so unit tests can pick it up
            command.text = "BadLocationException";
            throw new RuntimeException(e);
        }

    }
    /**
     * This function makes the else auto-dedent (if available)
     * @return the new indent and the number of chars it has been dedented (so, that has to be considered as a shift to the left
     * on subsequent things).
     */
    public Tuple<String, Integer> autoDedentElse(IDocument document, DocumentCommand command, String tok) throws BadLocationException {
        if(getIndentPrefs().getAutoDedentElse()){
            PySelection ps = new PySelection(document, command.offset);
            String lineContents = ps.getCursorLineContents();
            if(lineContents.trim().equals(tok)){
                
                String previousIfLine = ps.getPreviousLineThatAcceptsElse();
                if(previousIfLine != null){
                    String ifIndent = PyAction.getIndentationFromLine(previousIfLine);
                    String lineIndent = PyAction.getIndentationFromLine(lineContents);
                    
                    String indent = prefs.getIndentationString();
                    if(lineIndent.length() == ifIndent.length()+indent.length()){
                        Tuple<String,Integer> dedented = removeFirstIndent(lineContents);
                        ps.replaceLineContentsToSelection(dedented.o1);
                        command.offset = command.offset - dedented.o2;
                        return dedented;
                    }
                }
            }        
        }
        return null;
    }
    
    public Tuple<String, Integer> autoDedentElse(IDocument document, DocumentCommand command) throws BadLocationException {
    	return autoDedentElse(document, command, "else");
    }

    /**
     * This function makes the else auto-dedent (if available)
     * @return the new indent and the number of chars it has been dedented (so, that has to be considered as a shift to the left
     * on subsequent things).
     */
    public Tuple<String, Integer> autoDedentElif(IDocument document, DocumentCommand command) throws BadLocationException {
    	return autoDedentElse(document, command, "elif");
    }
    

    /**
     * Create the indentation string after comma and a newline.
     * 
     * @param document
     * @param text
     * @param offset
     * @return Indentation String
     * @throws BadLocationException
     */
    private String smartIndentAfterPar(IDocument document, String text, int offset)
            throws BadLocationException {
    	//if we should not use smart indent, this function has no use.
    	if(!this.prefs.getSmartIndentPar()){
    		return text;
    	}

    	int smartIndent = totalIndentAmountAfterCommaNewline(document, offset);
        if (smartIndent > 0) {
            String initial = text;

            // Discard everything but the newline from initial, since we'll
            // build the smart indent from scratch anyway.
            int initialLength = initial.length();
            for (int i = 0; i < initialLength; i++) {
                char theChar = initial.charAt(i);
                // This covers all cases I know of, but if there is any platform
                // with weird newline then this would need to be smarter.
                if (theChar != '\r' && theChar != '\n') {
                    if (i > 0) {
                        initial = initial.substring(0, i);
                        --i;
                        smartIndent -= i;
                    }
                    break;
                }
            }

            // Create the actual indentation string
            String indentationString = prefs.getIndentationString();
            int indentationSteps = smartIndent / prefs.getTabWidth();
            int spaceSteps = smartIndent % prefs.getTabWidth();
            
            StringBuffer b = new StringBuffer(smartIndent);
            while (indentationSteps > 0){
                indentationSteps -= 1;
                b.append(indentationString);
            }
            
            while (spaceSteps >= 0){
                spaceSteps -= 1;
                b.append(" ");
            }

            return initial + b.toString();
        }
        return text;
    }

    /**
     * Private function which is called when a colon is the command.
     * 
     * The following code will auto-replace colons in function declaractions
     * e.g., def something(self): ^ cursor before the end colon
     * 
     * Typing another colon (i.e, ':') at that position will not insert another
     * colon
     * 
     * @param document
     * @param command
     * @throws BadLocationException
     */
    private void performColonReplacement(IDocument document, DocumentCommand command) {
        PySelection ps = new PySelection(document, command.offset);
        int absoluteOffset = ps.getAbsoluteCursorOffset();
        int documentLength = ps.getDoc().getLength();

        // need to check whether whether we're at the very end of the document
        if (absoluteOffset < documentLength) {
            try {
                char currentCharacter = document.getChar(absoluteOffset);

                if (currentCharacter == ':') {
                    command.text = DocUtils.EMPTY_STRING;
                    command.caretOffset = command.offset + 1;
                }

            } catch (BadLocationException e) {
                // should never happen because I just checked the length 
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * Private function to call to perform any replacement of braces.
     * 
     * The Eclipse Java editor does this by default, and it is very useful. If
     * you try to insert some kind of pair, be it a parenthesis or bracket in
     * Java, the character will not insert and instead the editor just puts your
     * cursor at the next position.
     * 
     * This function performs the equivalent for the Python editor.
     *  
     * @param document
     * @param command if the command does not contain a brace, this function does nothing.
     * @throws BadLocationException 
     */
    private void performPairReplacement(IDocument document, DocumentCommand command) throws BadLocationException {
        PySelection ps = new PySelection(document, command.offset);

        char c = ps.getCharAtCurrentOffset();
        char peer = DocUtils.getPeer(c);
        StringBuffer doc = new StringBuffer(document.get());
        //it is not enough just counting the chars, we have to ignore those that are within comments or literals.
        ParsingUtils.removeCommentsWhitespacesAndLiterals(doc);
        int chars = PyAction.countChars(c, doc);
        int peers = PyAction.countChars(peer, doc);
        
        if( chars == peers){
            //if we have the same number of peers, we want to eat the char
            command.text = DocUtils.EMPTY_STRING;
            command.caretOffset = command.offset + 1;
        }
    }

    /**
     * @param ps
     * @param c the char to close
     * @return
     * @throws BadLocationException
     */
    private boolean shouldClose(PySelection ps, char c) throws BadLocationException {
        String line = ps.getLine();

        String lineContentsFromCursor = ps.getLineContentsFromCursor();

        for (int i = 0; i < lineContentsFromCursor.length(); i++) {
            if (!Character.isWhitespace(lineContentsFromCursor.charAt(i))) {
                return false;
            }
        }

        int i = PyAction.countChars(c, line);
        int j = PyAction.countChars(c, line);

        if (j > i) {
            return false;
        }

        return true;
    }

    /**
	 * Return smart indent amount for new line. This should be done for
	 * multiline structures like function parameters, tuples, lists and
	 * dictionaries.
	 * 
	 * Example:
	 * 
	 * a=foo(1, #
	 * 
	 * We would return the indentation needed to place the caret at the #
	 * position.
	 * 
	 * @param document
	 *            The document
	 * @param offset
	 *            The document offset of the last character on the previous line
	 * @return indent, or -1 if smart indent could not be determined (fall back
	 *         to default)
	 */
    private int totalIndentAmountAfterCommaNewline(IDocument document, int offset)
            throws BadLocationException {
    	
        int lineStart = document.getLineInformationOfOffset(offset).getOffset();
        String line = document.get(lineStart, offset - lineStart);
        int lineLength = line.length();

        for (int i = 0; i < lineLength ; i++) {
            char theChar = line.charAt(i);

            // This covers all cases I know of, but if there is any platform
            // with weird newline then this would need to be smarter.
            if (theChar == '\r' || theChar == '\n')
                break;

            if (theChar == ')' || theChar == ']' || theChar == '}') {
            	char peer = DocUtils.getPeer(theChar);
            	if(PyAction.countChars(theChar, line) > PyAction.countChars(peer, line)){
            		//ok, we have to do a dedent here
            		PythonPairMatcher matcher = new PythonPairMatcher(DocUtils.BRACKETS);
            		int openingPeerPosition = matcher.searchForOpeningPeer(offset, peer, theChar, document);
            		if(openingPeerPosition > 0){
	            		IRegion lineInformationOfOffset = document.getLineInformationOfOffset(openingPeerPosition);
	            		int j = openingPeerPosition - lineInformationOfOffset.getOffset();
	            		return j+1;
            		}
            	}
            }
            
            if (theChar == '(' || theChar == '[' || theChar == '{') {
                char peer = DocUtils.getPeer(theChar);
                if(PyAction.countChars(theChar, line) > PyAction.countChars(peer, line)){

	                //ok, it's not just returning the line now, we have to check for tabs and make each
	                //tab count for the tabWidth.
	                int smartIndent = i+1;
	                String string = line.substring(0, smartIndent - 1);
	                
	                for (int j = 0; j < string.length(); j++) {
	                    char c = string.charAt(j);
	                    if (c == '\t') {
	                        smartIndent += prefs.getTabWidth() - 1;
	                    }
	                }
	                return smartIndent;
                }
            }
        }
        return -1;
    }
}