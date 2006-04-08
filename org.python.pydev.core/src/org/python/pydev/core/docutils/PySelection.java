/*
 * @author: ptoofani
 * @author Fabio Zadrozny
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.core.docutils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.log.Log;

/**
 * Redone the whole class, so that the interface is better defined and no
 * duplication of information is given.
 * 
 * Now, it is just used as 'shortcuts' to document and selection settings.
 * 
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public class PySelection {
    
    private IDocument doc;
    private ITextSelection textSelection;
	public static final String[] DEDENT_TOKENS = new String[]{
	    "return",
	    "break",
	    "continue",
	    "pass",
	    "raise",
	    "yield"
	};

	public static final String[] INDENT_TOKENS = new String[]{
		"if"      , 
		"for"     , 
		"except"  , 
		"def"     ,
		"class"   ,
		"else"    ,
		"elif"    ,
		"while"   ,
		"try"     ,
		"finally" 
	};
	

    /**
	 * Alternate constructor for PySelection. Takes in a text editor from Eclipse.
	 * 
	 * @param textEditor The text editor operating in Eclipse
	 */
    public PySelection(ITextEditor textEditor) {
        this(textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()), 
                (ITextSelection) textEditor.getSelectionProvider().getSelection());
    }


    /**
     * @param document the document we are using to make the selection
     * @param selection that's the actual selection. It might have an offset and a number of selected chars
     */
    public PySelection(IDocument doc, ITextSelection selection) {
        this.doc = doc;
        this.textSelection = selection;
    }

    public PySelection(IDocument doc, int line, int col) {
    	this(doc, line, col, 0);
    }
    
    public PySelection(IDocument doc, int line, int col, int len) {
    	this.doc = doc;
		this.textSelection = new TextSelection(doc, getAbsoluteCursorOffset(line, col), len);
    }
    
    public int getAbsoluteCursorOffset(int line, int col) {
        try {
            IRegion offsetR = this.doc.getLineInformation(line);
            return offsetR.getOffset() + col;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
    /**
     * @param document the document we are using to make the selection
     * @param offset the offset where the selection will happen (0 characters will be selected)
     */
    public PySelection(IDocument doc, int offset) {
        this.doc = doc;
        this.textSelection = new TextSelection(doc, offset, 0);
    }

    /**
     * Changes the selection
     * @param absoluteStart this is the offset of the start of the selection
     * @param absoluteEnd this is the offset of the end of the selection
     */
    public void setSelection(int absoluteStart, int absoluteEnd) {
        this.textSelection = new TextSelection(doc, absoluteStart, absoluteEnd-absoluteStart);
    }

    /**
     * Creates a selection for the document, so that no characters are selected and the offset is position 0
     * @param doc the document where we are doing the selection
     */
    public PySelection(IDocument doc) {
        this(doc, 0);
    }
    /**
     * In event of partial selection, used to select the full lines involved.
     */
    public void selectCompleteLine() {
        IRegion endLine = getEndLine();
        IRegion startLine = getStartLine();
        
        this.textSelection = new TextSelection(doc, startLine.getOffset(), endLine.getOffset() + endLine.getLength() - startLine.getOffset());
    }

    /**
     * @return the line where a global import would be able to happen.
     * 
     * The 'usual' structure that we take into consideration for a py file here is:
     * 
     * #coding ...
     * 
     * '''
     * multiline comment...
     * '''
     * 
     * imports #that's what we want to find out
     * 
     * code
     * 
     */
    public int getLineAvailableForImport() {
        StringBuffer multiLineBuf = new StringBuffer();
        int[] firstGlobalLiteral = getFirstGlobalLiteral(multiLineBuf, 0);

        if (multiLineBuf.length() > 0 && firstGlobalLiteral[0] >= 0 && firstGlobalLiteral[1] >= 0) {
            //ok, multiline found
            int startingMultilineComment = getLineOfOffset(firstGlobalLiteral[0]);
            
            if(startingMultilineComment < 4){
                
                //let's see if the multiline comment found is in the beggining of the document
                int lineOfOffset = getLineOfOffset(firstGlobalLiteral[1]);
                return lineOfOffset + 1;
            }else{

                return getFirstNonCommentLine();
            }
        } else {
            
            //ok, no multiline comment, let's get the first line that is not a comment
            return getFirstNonCommentLine();
        }
    }


    /**
     * @return the first line found that is not a comment.
     */
    private int getFirstNonCommentLine() {
        int lineToMoveImport = 0;
        int lines = getDoc().getNumberOfLines();
        for (int line = 0; line < lines; line++) {
            String str = getLine(line);
            if (! str.startsWith("#")) {
                lineToMoveImport = line;
                break;
            }
        }
        return lineToMoveImport;
    }
    
    
    /**
     * @param initialOffset this is the offset we should use to analyze it
     * @param buf (out) this is the comment itself
     * @return a tuple with the offset of the start and end of the first multiline comment found
     */
    public int[] getFirstGlobalLiteral(StringBuffer buf, int initialOffset){
        try {
            IDocument d = getDoc();
            String strDoc = d.get(initialOffset, d.getLength() - initialOffset);
            
            if(initialOffset > strDoc.length()-1){
                return new int[]{-1, -1};
            }
            
            char current = strDoc.charAt(initialOffset);
            
            //for checking if it is global, it must be in the beggining of a line (must be right after a \r or \n).
            
            while (current != '\'' && current != '"' && initialOffset < strDoc.length()-1) {
            	
            	//if it is inside a parenthesis, we will not take it into consideration.
                if(current == '('){
                    initialOffset = ParsingUtils.eatPar(strDoc, initialOffset, buf);
                }
                
                
                initialOffset += 1;
                if(initialOffset < strDoc.length()-1){
                    current = strDoc.charAt(initialOffset);
                }
            }

            //either, we are at the end of the document or we found a literal
            if(initialOffset < strDoc.length()-1){

            	if(initialOffset == 0){ //first char of the document... this is ok
	                int i = ParsingUtils.eatLiterals(strDoc, buf, initialOffset);
	                return new int[]{initialOffset, i};
            	}
            	
            	char lastChar = strDoc.charAt(initialOffset-1);
            	//it is only global if after \r or \n
            	if(lastChar == '\r' || lastChar == '\n'){
	                int i = ParsingUtils.eatLiterals(strDoc, buf, initialOffset);
	                return new int[]{initialOffset, i};
            	}
            	
                //ok, still not found, let's keep going
        		return getFirstGlobalLiteral(buf, initialOffset+1);
            }else{
            	return new int[]{-1, -1};
            	
            }
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void beep(Exception e) {
        Log.log(e);
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
    }

    public static String getLineWithoutCommentsOrLiterals(String l) {
        StringBuffer buf = new StringBuffer(l);
        ParsingUtils.removeCommentsWhitespacesAndLiterals(buf, false);
        return buf.toString();
        
    }
    public String getLineWithoutCommentsOrLiterals() {
        return getLineWithoutCommentsOrLiterals(getLine());
    }
    
    /**
     * Gets current line from document.
     * 
     * @return String line in String form
     */
    public String getLine() {
        return getLine(getDoc(), getCursorLine());
    }
    

    /**
     * Gets line from document.
     * 
     * @param i Line number
     * @return String line in String form
     */
    public String getLine(int i) {
        return getLine(getDoc(), i);
    }
    
    /**
     * Gets line from document.
     * 
     * @param i Line number
     * @return String line in String form
     */
    public static String getLine(IDocument doc, int i) {
        try {
            return doc.get(doc.getLineInformation(i).getOffset(), doc.getLineInformation(i).getLength());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * @param offset the offset we want to get the line
     * @return the line of the passed offset
     */
    public int getLineOfOffset(int offset) {
        try {
            return getDoc().getLineOfOffset(offset);
        } catch (BadLocationException e) {
            return 0;
        }
    }
    
    /**
     * @return the offset of the cursor
     */
    public int getLineOffset() {
    	return getLineOffset(getCursorLine());
    }
    
    /**
     * @return the offset of the specified line
     */
    public int getLineOffset(int line) {
        try {
            return getDoc().getLineInformation(line).getOffset();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Deletes a line from the document
     * @param i
     */
    public void deleteLine(int i) {
        deleteLine(getDoc(), i);
    }

    /**
     * Removes comments at the end of the document
     * @param doc this is the document from where the comments must be removed
     * @return a StringBuffer with the comments that have been removed
     */
    public static StringBuffer removeEndingComments(IDocument doc){
        StringBuffer comments = new StringBuffer();
        int lines = doc.getNumberOfLines();
        String delimiter = PySelection.getDelimiter(doc);
        for (int i = lines-1; i >= 0; i--) {
            String line = PySelection.getLine(doc, i);
            String trimmed = line.trim();
            if(trimmed.length() > 0 && trimmed.charAt(0) != '#'){
                return comments;
            }
            comments.insert(0,line);
            comments.insert(0,delimiter);
            try {
                if(line.length() > 0){
                    PySelection.deleteLine(doc, i);
                }
            } catch (Exception e) {
                //ignore
            }
        }
        return comments;
    }
    
    /**
     * Deletes a line from the document
     * @param i
     */
    public static void deleteLine(IDocument doc, int i) {
        try {
            IRegion lineInformation = doc.getLineInformation(i);
            int offset = lineInformation.getOffset();
            
            int length = -1;
            
            if(doc.getNumberOfLines() > i){
	            int nextLineOffset = doc.getLineInformation(i+1).getOffset();
	            length = nextLineOffset - offset;
            }else{
                length = lineInformation.getLength();
            }
            
            if(length > -1){
                doc.replace(offset, length, "");
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        } 
    }
    
	public void deleteSpacesAfter(int offset) {
		try {
			int initial = offset;
			String next = doc.get(offset, 1);
			
			//don't delete 'all' that is considered whitespace (as \n and \r)
			try {
				while (next.charAt(0) == ' ' || next.charAt(0) == '\t') {
					offset++;
					next = doc.get(offset, 1);
				}
			} catch (Exception e) {
				// ignore
			}
			
			final int len = offset-initial;
			if(len > 0){
				doc.replace(initial, len, "");
			}
		} catch (Exception e) {
			//ignore
		}
	}

    
    /**
     * Deletes the current selected text
     * 
     * @throws BadLocationException
     */
    public void deleteSelection() throws BadLocationException {
        int offset = textSelection.getOffset();
        doc.replace(offset, textSelection.getLength(), "");
    }

    
    public void addLine(String contents, int afterLine){
        addLine(getDoc(), getEndLineDelim(), contents, afterLine);
    }
    
    public static void addLine(IDocument doc, String endLineDelim, String contents, int afterLine){
        try {
            
            int offset = -1;
            if(doc.getNumberOfLines() > afterLine){
	            offset = doc.getLineInformation(afterLine+1).getOffset();
                
            }else{
	            offset = doc.getLineInformation(afterLine).getOffset();
            }
            
            
            if (!contents.endsWith(endLineDelim)){
                contents += endLineDelim;
            }
            
            if(offset >= 0){
                doc.replace(offset, 0, contents);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        } 
    }

    /**
     * @param ps
     * @return the line where the cursor is (from the cursor position to the end of the line).
     * @throws BadLocationException
     */
    public String getLineContentsFromCursor() throws BadLocationException {
        int lineOfOffset = getDoc().getLineOfOffset(getAbsoluteCursorOffset());
        IRegion lineInformation = getDoc().getLineInformation(lineOfOffset);
        
        
        String lineToCursor = getDoc().get(getAbsoluteCursorOffset(),   lineInformation.getOffset() + lineInformation.getLength() - getAbsoluteCursorOffset());
        return lineToCursor;
    }

    /**
     * @param ps
     * @return the line where the cursor is (from the beggining of the line to the cursor position).
     * @throws BadLocationException
     */
    public String getLineContentsToCursor() throws BadLocationException {
        int lineOfOffset = getDoc().getLineOfOffset(getAbsoluteCursorOffset());
        IRegion lineInformation = getDoc().getLineInformation(lineOfOffset);
        String lineToCursor = getDoc().get(lineInformation.getOffset(), getAbsoluteCursorOffset() - lineInformation.getOffset());
        return lineToCursor;
    }

    /**
     * Readjust the selection so that the whole document is selected.
     * 
     * @param onlyIfNothingSelected: If false, check if we already have a selection. If we
     * have a selection, it is not changed, however, if it is true, it always selects everything.
     */
    public void selectAll(boolean forceNewSelection) {
        if (!forceNewSelection){
            if(getSelLength() > 0)
                return;
        }
        
        textSelection = new TextSelection(doc, 0, doc.getLength());
    }

    /**
     * @return Returns the startLineIndex.
     */
    public int getStartLineIndex() {
        return this.getTextSelection().getStartLine();
    }


    /**
     * @return Returns the endLineIndex.
     */
    public int getEndLineIndex() {
        return this.getTextSelection().getEndLine();
    }

    /**
     * @return Returns the doc.
     */
    public IDocument getDoc() {
        return doc;
    }


    /**
     * @return Returns the selLength.
     */
    public int getSelLength() {
        return this.getTextSelection().getLength();
    }


    /**
     * @return Returns the selection.
     */
    public String getCursorLineContents() {
        try {
            int start = getStartLine().getOffset();
            int end = getEndLine().getOffset() + getEndLine().getLength();
            return this.doc.get(start, end-start);
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return "";
    }

    public static String getDelimiter(IDocument doc){
        return getDelimiter(doc, 0);
    }
    
    /**
     * This method returns the delimiter for the document
     * @param doc
     * @param startLineIndex
     * @return  delimiter for the document (\n|\r\|r\n)
     * @throws BadLocationException
     */
    public static String getDelimiter(IDocument doc, int line){
        String endLineDelim;
        try {
            if (doc.getNumberOfLines() > 1){
                endLineDelim = doc.getLineDelimiter(line);
                if (endLineDelim == null) {
                    endLineDelim = doc.getLegalLineDelimiters()[0];
                }
                return endLineDelim;
            }
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return System.getProperty("line.separator"); 
        
    }

    /**
     * @return Returns the endLineDelim.
     */
    public String getEndLineDelim() {
        return getDelimiter(getDoc());
    }

    /**
     * @return Returns the startLine.
     */
    public IRegion getStartLine() {
        try {
            return getDoc().getLineInformation(getStartLineIndex());
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return null;
    }

    /**
     * @return Returns the endLine.
     */
    public IRegion getEndLine() {
        try {
            return getDoc().getLineInformation(getEndLineIndex());
        } catch (BadLocationException e) {
            Log.log(e);
        }
        return null;
    }

    /**
     * @return Returns the cursorLine.
     */
    public int getCursorLine() {
        return this.getTextSelection().getEndLine();
    }

    /**
     * @return Returns the absoluteCursorOffset.
     */
    public int getAbsoluteCursorOffset() {
        return this.getTextSelection().getOffset();
    }

    /**
     * @return Returns the textSelection.
     */
    public ITextSelection getTextSelection() {
        return textSelection;
    }


    /**
     * @return the Selected text
     */
    public String getSelectedText() {
        int start = getTextSelection().getOffset();
        int len = getTextSelection().getLength();
        try {
            return this.doc.get(start, len);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @return
     * @throws BadLocationException
     */
    public char getCharAfterCurrentOffset() throws BadLocationException {
        return getDoc().getChar(getAbsoluteCursorOffset()+1);
    }
    
    /**
     * @return
     * @throws BadLocationException
     */
    public char getCharAtCurrentOffset() throws BadLocationException {
        return getDoc().getChar(getAbsoluteCursorOffset());
    }

    
    /**
     * @return the offset mapping to the end of the line passed as parameter.
     * @throws BadLocationException 
     */
    public int getEndLineOffset(int line) throws BadLocationException {
        IRegion lineInformation = doc.getLineInformation(line);
        return lineInformation.getOffset() + lineInformation.getLength();
    }

    /**
     * @return the offset mapping to the end of the current line.
     */
    public int getEndLineOffset() {
        IRegion endLine = getEndLine();
        return endLine.getOffset() + endLine.getLength();
    }

    /**
     * @return the offset mapping to the start of the current line.
     */
    public int getStartLineOffset() {
        IRegion startLine = getStartLine();
        return startLine.getOffset();
    }
    

    /**
     * @return the complete dotted string given the current selection and the strings after
     * 
     * e.g.: if we have a text of
     * 'value = aa.bb.cc()' and 'aa' is selected, this method would return the whole dotted string ('aa.bb.cc') 
     * @throws BadLocationException 
     */
    public String getFullRepAfterSelection() throws BadLocationException {
        int absoluteCursorOffset = getAbsoluteCursorOffset();
        int length = doc.getLength();
        int end = absoluteCursorOffset;
        char ch = doc.getChar(end);
        while(Character.isLetterOrDigit(ch) || ch == '.'){
            end++;
            //check if we can still get some char
            if(length-1 < end){
                break;
            }
            ch = doc.getChar(end);
        }
        return doc.get(absoluteCursorOffset, end-absoluteCursorOffset);
    }

    public Tuple<String, Integer> getCurrToken() throws BadLocationException {
        Tuple<String, Integer> tup = extractPrefix(doc, getAbsoluteCursorOffset(), false);
        String prefix = tup.o1;

        // ok, now, get the rest of the token, as we already have its prefix

        int start = tup.o2-prefix.length();
        int end = start;
        while (doc.getLength() - 1 >= end) {
            char ch = doc.getChar(end);
            if(Character.isJavaIdentifierPart(ch)){
                end++;
            }else{
                break;
            }
        }
        String post = doc.get(tup.o2, end-tup.o2);
        return new Tuple<String, Integer>(prefix+post, start);
    }
 
   /**
    * This function gets the tokens inside the parenthesis that start at the current selection line
    * 
    * @param addSelf: this defines whether tokens named self should be added if it is found.
    * 
    * @return a Tuple so that the first param is the list and the second the offset of the end of the parentesis it may return null if no starting parentesis was found at the current line
    */
    public Tuple<List<String>, Integer> getInsideParentesisToks(boolean addSelf) {
        List<String> l = new ArrayList<String>();

        String line = getLine();
        int openParIndex = line.indexOf('(');
        if (openParIndex == -1) { // we are in a line that does not have a parentesis
            return null;
        }
        int lineOffset = getStartLineOffset();
        StringBuffer buf = new StringBuffer();
        String docContents = doc.get();
        int i = lineOffset + openParIndex;
        int j = ParsingUtils.eatPar(docContents, i, buf);
        String insideParentesisTok = docContents.substring(i + 1, j);

        StringTokenizer tokenizer = new StringTokenizer(insideParentesisTok, ",");
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            String trimmed = tok.split("=")[0].trim();
            trimmed = trimmed.replaceAll("\\(", "");
            trimmed = trimmed.replaceAll("\\)", "");
            if (!addSelf && trimmed.equals("self")) {
                // don't add self...
            } else {
                l.add(trimmed);
            }
        }
        return new Tuple<List<String>, Integer>(l, j);
    }


    /**
     * This function replaces all the contents in the current line before the cursor for the contents passed
     * as parameter 
     */
    public void replaceLineContentsToSelection(String newContents) throws BadLocationException {
        int lineOfOffset = getDoc().getLineOfOffset(getAbsoluteCursorOffset());
        IRegion lineInformation = getDoc().getLineInformation(lineOfOffset);
        getDoc().replace(lineInformation.getOffset(), getAbsoluteCursorOffset() - lineInformation.getOffset(), newContents);

    }


    /**
     * This function goes backward in the document searching for an 'if' and returns the line that has it.
     * 
     * May return null if it was not found.
     */
    public String getPreviousLineThatAcceptsElse() {
        DocIterator iterator = new DocIterator(false);
        while(iterator.hasNext()){
            String line = (String) iterator.next();
            String trimmed = line.trim();
            if(trimmed.startsWith("if ") || trimmed.startsWith("if(") || trimmed.startsWith("for ")  || trimmed.startsWith("for(") 
                    || trimmed.startsWith("except")|| trimmed.startsWith("except(") ){
                return line;
            }
        }
        return null;
    }
    
    /**
     * @return a tuple with:
     * - the line that starts the new scope 
     * - a String with the line where some dedent token was found while looking for that scope.
     * - a string with the lowest indent (null if none was found)
     */
    public Tuple3<String, String, String> getPreviousLineThatStartsScope() {
        DocIterator iterator = new DocIterator(false);
        String foundDedent = null;
        int lowest = Integer.MAX_VALUE;
        String lowestStr = null;
        
        while(iterator.hasNext()){
            String line = (String) iterator.next();
            String trimmed = line.trim();
            
            for (String dedent : PySelection.INDENT_TOKENS) {
            	if(trimmed.startsWith(dedent)){
            		if(isCompleteToken(trimmed, dedent)){
            			return new Tuple3<String, String, String>(line, foundDedent, lowestStr);
            		}
            	}
            }
            //we have to check for the first condition (if a dedent is found, but we already found 
            //one with a first char, the dedent should not be taken into consideration... and vice-versa).
            if(lowestStr == null && foundDedent == null && startsWithDedentToken(trimmed)){
                foundDedent = line;
                
            }else if(foundDedent == null && trimmed.length() > 0){
                int firstCharPosition = getFirstCharPosition(line);
                if(firstCharPosition < lowest){
                    lowest = firstCharPosition;
                    lowestStr = line;
                }
            }

        }
        return null;
    }

    public static String [] getActivationTokenAndQual(String theDoc, int documentOffset) {
        return PySelection.getActivationTokenAndQual(new Document(theDoc), documentOffset);
        
    }


    public static String [] getActivationTokenAndQual(IDocument theDoc, int documentOffset) {
    	return getActivationTokenAndQual(theDoc, documentOffset, false);
    }


    public static String extractPrefix(IDocument document, int offset) {
    	return extractPrefix(document, offset, false).o1;
    }


    /**
     * @param theDoc
     * @param documentOffset
     * @return
     * @throws BadLocationException
     */
    public static int eatFuncCall(IDocument theDoc, int documentOffset) throws BadLocationException {
        String c = theDoc.get(documentOffset, 1);
        if(c.equals(")") == false){
            throw new AssertionError("Expecting ) to eat callable. Received: "+c);
        }
        
        while(documentOffset > 0 && theDoc.get(documentOffset, 1).equals("(") == false){
            documentOffset -= 1;
        }
        
        return documentOffset;
    }


    /**
     * Checks if the activationToken ends with some char from cs.
     */
    public static boolean endsWithSomeChar(char cs[], String activationToken) {
        for (int i = 0; i < cs.length; i++) {
            if (activationToken.endsWith(cs[i] + "")) {
                return true;
            }
        }
        return false;
    
    }


    /**
     * Returns the activation token.
     * 
     * @param theDoc
     * @param documentOffset the current cursor offset (we may have to change it if getFullQualifier is true)
     * @param getFullQualifier 
     * @return the activation token and the qualifier.
     */
    public static String [] getActivationTokenAndQual(IDocument theDoc, int documentOffset, boolean getFullQualifier) {
        Tuple<String, Integer> tupPrefix = extractPrefix(theDoc, documentOffset, getFullQualifier);
        
        if(getFullQualifier == true){
        	//may have changed
        	documentOffset = tupPrefix.o2;
        }
        
    	String activationToken = tupPrefix.o1;
        documentOffset = documentOffset-activationToken.length()-1;
    
        try {
            while(documentOffset >= 0 && documentOffset < theDoc.getLength() && theDoc.get(documentOffset, 1).equals(".")){
                String tok = extractPrefix(theDoc, documentOffset);
    
                    
                String c = theDoc.get(documentOffset-1, 1);
                
                if(c.equals("]")){
                    activationToken = "list."+activationToken;  
                    break;
                    
                }else if(c.equals("}")){
                    activationToken = "dict."+activationToken;  
                    break;
                    
                }else if(c.equals("'") || c.equals("\"")){
                    activationToken = "str."+activationToken;  
                    break;
                
                }else if(c.equals(")")){
                    documentOffset = eatFuncCall(theDoc, documentOffset-1);
                    tok = extractPrefix(theDoc, documentOffset);
                    activationToken = tok+"()."+activationToken;  
                    documentOffset = documentOffset-tok.length()-1;
                
                }else if(tok.length() > 0){
                    activationToken = tok+"."+activationToken;  
                    documentOffset = documentOffset-tok.length()-1;
                    
                }else{
                    break;
                }
    
            }
        } catch (BadLocationException e) {
            System.out.println("documentOffset "+documentOffset);
            System.out.println("theDoc.getLength() "+theDoc.getLength());
            e.printStackTrace();
        }
        
        String qualifier = "";
        //we complete on '.' and '('.
        //' ' gets globals
        //and any other char gets globals on token and templates.
    
        //we have to get the qualifier. e.g. bla.foo = foo is the qualifier.
        if (activationToken.indexOf('.') != -1) {
            while (endsWithSomeChar(new char[] { '.','[' }, activationToken) == false
                    && activationToken.length() > 0) {
    
                qualifier = activationToken.charAt(activationToken.length() - 1) + qualifier;
                activationToken = activationToken.substring(0, activationToken.length() - 1);
            }
        } else { //everything is a part of the qualifier.
            qualifier = activationToken.trim();
            activationToken = "";
        }
        return new String[]{activationToken, qualifier};
    }


    /**
     * 
     * @param document
     * @param offset
     * @param getFullQualifier if true we get the full qualifier (even if it passes the current cursor location)
     * @return
     */
    public static Tuple<String, Integer> extractPrefix(IDocument document, int offset, boolean getFullQualifier) {
    	try {
    		if(getFullQualifier){
    			//if we have to get the full qualifier, we'll have to walk the offset (cursor) forward
    			while(offset < document.getLength()){
    				char ch= document.getChar(offset);
    				if (Character.isJavaIdentifierPart(ch)){
    					offset++;
    				}else{
    					break;
    				}
    				
    			}
    		}
    		int i= offset;
    		
    		if (i > document.getLength())
    			return new Tuple<String, Integer>("", document.getLength()); //$NON-NLS-1$
    	
    		while (i > 0) {
    			char ch= document.getChar(i - 1);
    			if (!Character.isJavaIdentifierPart(ch))
    				break;
    			i--;
    		}
    
    		return new Tuple<String, Integer>(document.get(i, offset - i), offset);
    	} catch (BadLocationException e) {
    		return new Tuple<String, Integer>("", offset); //$NON-NLS-1$
    	}
    }


    /**
     * @param c
     * @param string
     */
    public static boolean containsOnly(char c, String string) {
        for (int i = 0; i < string.length(); i++) {
            if(string.charAt(i) != c){
                return false;
            }
        }
        return true;
    }


    /**
     * @param c
     * @param string
     */
    public static boolean containsOnlyWhitespaces(String string) {
        for (int i = 0; i < string.length(); i++) {
            if(Character.isWhitespace(string.charAt(i)) == false){
                return false;
            }
        }
        return true;
    }


    /**
     * @param selection
     * @return
     */
    public static String getIndentationFromLine(String selection) {
        int firstCharPosition = getFirstCharPosition(selection);
        return selection.substring(0, firstCharPosition);
    }


    /**
     * @param src
     * @return
     */
    public static int getFirstCharPosition(String src) {
        int i = 0;
    	boolean breaked = false;
    	while (i < src.length()) {
    	    if (   Character.isWhitespace(src.charAt(i)) == false && src.charAt(i) != '\t'  ) {
    	        i++;
    		    breaked = true;
    			break;
    		}
    	    i++;
    	}
    	if (!breaked){
    	    i++;
    	}
    	return (i - 1);
    }


    /**
     * @param doc
     * @param region
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativePosition(IDocument doc, IRegion region) throws BadLocationException {
        int offset = region.getOffset();
    	String src = doc.get(offset, region.getLength());
    
    	return getFirstCharPosition(src);
    }


    /**
     * @param doc
     * @param cursorOffset
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativeLinePosition(IDocument doc, int line) throws BadLocationException {
        IRegion region;
    	region = doc.getLineInformation(line);
    	return getFirstCharRelativePosition(doc, region);
    }


    /**
     * @param doc
     * @param cursorOffset
     * @return
     * @throws BadLocationException
     */
    public static int getFirstCharRelativePosition(IDocument doc, int cursorOffset) throws BadLocationException {
        IRegion region;
    	region = doc.getLineInformationOfOffset(cursorOffset);
    	return getFirstCharRelativePosition(doc, region);
    }


    /**
     * Returns the position of the first non whitespace char in the current line.
     * @param doc
     * @param cursorOffset
     * @return position of the first character of the line (returned as an absolute
     * 		   offset)
     * @throws BadLocationException
     */
    public static int getFirstCharPosition(IDocument doc, int cursorOffset)
    	throws BadLocationException {
        IRegion region;
    	region = doc.getLineInformationOfOffset(cursorOffset);
    	int offset = region.getOffset();
    	return offset + getFirstCharRelativePosition(doc, cursorOffset);
    }


    public static boolean startsWithDedentToken(String trimmedLine) {
        for (String dedent : PySelection.DEDENT_TOKENS) {
            if(trimmedLine.startsWith(dedent)){
                return isCompleteToken(trimmedLine, dedent);
            }
        }
        return false;
    }


	private static boolean isCompleteToken(String trimmedLine, String dedent) {
		if(dedent.length() < trimmedLine.length()){
		    char afterToken = trimmedLine.charAt(dedent.length());
		    if(afterToken == ' ' || afterToken == ':' || afterToken == ';' || afterToken == '('){
		        return true;
		    }
		    return false;
		}else{
		    return true;
		}
	}


    private class DocIterator implements Iterator{
        private int startingLine;
        private boolean forward;
        private boolean isFirst = true;
        public DocIterator(boolean forward){
            this.startingLine = getCursorLine();
            this.forward = forward;
        }

        public boolean hasNext() {
            if(forward){
                throw new RuntimeException("Forward iterator not implemented.");
            }else{
                return startingLine >= 0;
            }
        }

        /**
         * Note that the first thing it returns is the lineContents to cursor (and only after that
         * does it return from the full line).
         */
        public Object next() {
        	try {
        		String line;
				if (isFirst) {
					line = getLineContentsToCursor();
					isFirst = false;
				}else{
					line = getLine(startingLine);
				}
				if (forward) {
					throw new RuntimeException("Forward iterator not implemented.");
				} else {
					startingLine--;
				}
				return line;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
        }

        public void remove() {
            throw new RuntimeException("Remove not implemented.");
        }
    }










  
  
}