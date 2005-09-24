/*
 * @author: ptoofani
 * @author Fabio Zadrozny
 * Created: June 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.parser.visitors.ParsingUtils;
import org.python.pydev.plugin.PydevPlugin;

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

    /**
     * @param document the document we are using to make the selection
     * @param offset the offset where the selection will happen (0 characters will be selected)
     */
    public PySelection(IDocument doc, int offset) {
        this.doc = doc;
        this.textSelection = new TextSelection(doc, offset, 0);
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
            while (current != '\'' && current != '"' && initialOffset < strDoc.length()-1) {
                if(current == '('){
                    initialOffset = ParsingUtils.eatPar(strDoc, initialOffset, buf);
                }
                initialOffset += 1;
                if(initialOffset < strDoc.length()-1){
                    current = strDoc.charAt(initialOffset);
                }
            }
            
            if(initialOffset == strDoc.length()-1){
                return new int[]{-1, -1};
            }else{
                //either, we are at the end of the document or we found a literal
                int i = ParsingUtils.eatLiterals(strDoc, buf, initialOffset);
                return new int[]{initialOffset, i};
            }
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void beep(Exception e) {
        PydevPlugin.log(e);
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
    }

    public static String getLineWithoutComments(String l) {
        int i;
        if((i = l.indexOf('#') ) != -1){
            l = l.substring(0, i);
        }
        return l;
        
    }
    public String getLineWithoutComments() {
        return getLineWithoutComments(getLine());
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
     * Gets cursor offset within a line.
     * 
     * @return int Offset to put cursor at
     */
    public int getLineOffset() {
        try {
            return getDoc().getLineInformation(getCursorLine()).getOffset();
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
     * Deletes a line from the document
     * @param i
     */
    public static void deleteLine(IDocument doc, int i) {
        try {
            int offset = doc.getLineInformation(i).getOffset();
            
            int length = -1;
            
            if(doc.getNumberOfLines() > i){
	            int nextLineOffset = doc.getLineInformation(i+1).getOffset();
	            length = nextLineOffset - offset;
            }else{
                length = doc.getLineInformation(i).getLength();
            }
            
            if(length > -1){
                doc.replace(offset, length, "");
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        } 
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
     * @return the line where the cursor is (from the beggining of the line to the cursor position).
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
            PydevPlugin.log(e);
        }
        return "";
    }


    /**
     * @return Returns the endLineDelim.
     */
    public String getEndLineDelim() {
        return PyAction.getDelimiter(getDoc());
    }

    /**
     * @return Returns the startLine.
     */
    public IRegion getStartLine() {
        try {
            return getDoc().getLineInformation(getStartLineIndex());
        } catch (BadLocationException e) {
            PydevPlugin.log(e);
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
            PydevPlugin.log(e);
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
     * @return the offset mapping to the end of the current line.
     */
    public int getEndLineOffset() {
        IRegion endLine = getEndLine();
        return endLine.getOffset() + endLine.getLength();
    }
    
}