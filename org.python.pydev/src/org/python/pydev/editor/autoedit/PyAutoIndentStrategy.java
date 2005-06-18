/*
 * Created on Dec 10, 2003
 * Author: atotic
 * License: Common Public License 1.0
 */

package org.python.pydev.editor.autoedit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;

/**
 * Implements indenting behavior.
 * 
 * <p>
 * Tabs vs. spaces indentation
 * <p>
 * Borrows heavily from {@link org.eclipse.jface.text.DefaultAutoIndentStrategy}, and the pyeclipse (PythonAutoIndentStrategy).
 */
public class PyAutoIndentStrategy extends DefaultAutoIndentStrategy {

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

    public static String createSpaceString(int width) {
        StringBuffer b = new StringBuffer(width);
        while (width-- > 0)
            b.append(" ");
        return b.toString();
    }

    /**
     * Set indentation automatically after newline.
     * 
     * @param document
     * @param length
     * @param text
     * @param offset
     * @return String
     * @throws BadLocationException
     */
    private String autoIndentNewline(IDocument document, int length, String text, int offset) throws BadLocationException {
        if (length == 0 && text != null && AbstractIndentPrefs.endsWithNewline(document, text)) {

            if (offset > 0) {
                char lastChar = document.getChar(offset - 1);

                if (lastChar == ':') {
                    String initial = text;

                    text = initial + prefs.getIndentationString();

                } else if (lastChar == ',') {
                    text = indentAfterCommaNewline(document, text, offset);
                }
            }
        }
        return text;
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
    private String indentAfterCommaNewline(IDocument document, String text, int offset) throws BadLocationException {
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
                        smartIndent -= --i;
                    }
                    break;
                }
            }

            // Create the actual indentation string
            String indentationString = prefs.getIndentationString();
            int indentationSteps = smartIndent / prefs.getTabWidth();
            int spaceSteps = smartIndent % prefs.getTabWidth();
            StringBuffer b = new StringBuffer(smartIndent);
            while (indentationSteps-- > 0)
                b.append(indentationString);
            while (spaceSteps-- > 0)
                b.append(" ");

            return initial + b.toString();
        }
        return text;
    }

    /**
     * Return smart indent amount for new line. This should be done for multiline structures like function parameters, tuples, lists and dictionaries.
     * 
     * Example:
     * 
     * a=foo(1, #
     * 
     * We would return the indentation needed to place the caret at the # position.
     * 
     * @param document The document
     * @param offset The document offset of the last character on the previous line
     * @return indent, or -1 if smart indent could not be determined (fall back to default)
     */
    private int totalIndentAmountAfterCommaNewline(IDocument document, int offset) throws BadLocationException {
        int lineStart = document.getLineInformationOfOffset(offset).getOffset();
        String line = document.get(lineStart, offset - lineStart);
        int lineLength = line.length();

        for (int i = lineLength - 1; i > 0; i--) {
            char theChar = line.charAt(i);

            // This covers all cases I know of, but if there is any platform
            // with weird newline then this would need to be smarter.
            if (theChar == '\r' || theChar == '\n')
                break;

            if (theChar == '(' || theChar == '[' || theChar == '{') {
                //ok, it's not just returning the line now, we have to check for tabs and make each
                //tab count for the tabWidth.
                int smartIndent = lineLength - (lineLength - i) + 1;
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
        return -1;
    }

    /**
     * 
     * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(IDocument, DocumentCommand)
     */
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        // super idents newlines the same amount as the previous line
        super.customizeDocumentCommand(document, command);
        try {
            command.text = autoIndentNewline(document, command.length, command.text, command.offset);

            prefs.convertToStd(document, command);
            
            if(command.text.equals("(")){
                PySelection ps = new PySelection(document, command.offset);
		        String line = ps.getLine();
                
                if(shouldClose(ps)){
                
        	        boolean hasClass = line.indexOf("class ") != -1;
                    boolean hasClassMethodDef = line.indexOf(" def ") != -1;
                    boolean hasMethodDef = line.indexOf("def ") != -1;
                    boolean hasNoDoublePoint = line.indexOf(":") == -1;
                    
	                command.shiftsCaret = false;
                    if(hasNoDoublePoint && (hasClass || hasClassMethodDef || hasMethodDef)){
                        if(hasClass){
                            command.text = "(object):";
			                command.caretOffset = command.offset + 7;
                        }else if (hasClassMethodDef){
                            command.text = "(self):";
			                command.caretOffset = command.offset + 5;
                        }else if (hasMethodDef){
                            command.text = "():";
			                command.caretOffset = command.offset + 1;
                        }else{
                            throw new RuntimeException("Something went wrong");
                        }
        	        }else{
		                command.text = "()";
		                command.caretOffset = command.offset + 1;
        	        }
                }
                
            }
            

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    
    /**
     * @param ps
     * @return
     * @throws BadLocationException
     */
    private boolean shouldClose(PySelection ps) throws BadLocationException {
        String line = ps.getLine();

        String lineContentsFromCursor = ps.getLineContentsFromCursor();
        
        for (int i = 0; i < lineContentsFromCursor.length(); i++) {
            if(!Character.isWhitespace(lineContentsFromCursor.charAt(i))){
                return false;
            }
        }
            
        
        int i = PyAction.countChars('(', line);
        int j = PyAction.countChars(')', line);
        
        if(j > i){
            return false;
        }
        
        return true;
    }

}