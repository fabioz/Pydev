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
import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.editor.PyDoubleClickStrategy;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;

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
     * 
     * @param document
     * @param length
     * @param text
     * @param offset
     * @return String
     * @throws BadLocationException
     */
    private String autoIndentNewline(IDocument document, int length, String text, int offset)
            throws BadLocationException {
        if (length == 0 && text != null && AbstractIndentPrefs.endsWithNewline(document, text)) {

            if (offset > 0) {
                char lastChar = document.getChar(offset - 1);

                //we dont want whitespaces
                while (offset > 0 && lastChar == DocUtils.SPACE) {
                    offset--;
                    lastChar = document.getChar(offset - 1);
                }

                if (offset > 0) {

                    if (lastChar == DocUtils.COLON) {
                        String initial = text;

                        text = initial + prefs.getIndentationString();

                    } else if (lastChar == DocUtils.COMMA) {
                        text = indentAfterCommaNewline(document, text, offset);
                    }
                }
            }
        }
        return text;
    }


    /**
     * 
     * 
     * @see org.eclipse.jface.text.IAutoEditStrategy#customizeDocumentCommand(IDocument, DocumentCommand)
     */
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        // super idents newlines the same amount as the previous line
        super.customizeDocumentCommand(document, command);

        try {
            command.text = autoIndentNewline(document, command.length, command.text, command.offset);
            prefs.convertToStd(document, command);
            

            if (command.text.equals(Character.toString(DocUtils.BEGIN_PARENTHESIS)) && prefs.getAutoParentesis()) {
                PySelection ps = new PySelection(document, command.offset);
                String line = ps.getLine();

                if (shouldClose(ps)) {

                    boolean hasClass = line.indexOf("class ") != -1;
                    boolean hasClassMethodDef = line.indexOf(" def ") != -1;
                    boolean hasMethodDef = line.indexOf("def ") != -1;
                    boolean hasNoDoublePoint = line.indexOf(":") == -1;

                    command.shiftsCaret = false;
                    if (hasNoDoublePoint && (hasClass || hasClassMethodDef || hasMethodDef)) {
                        if (hasClass) {
                            //command.text = "(object):"; //TODO: put some option in the interface for that
                            //command.caretOffset = command.offset + 7;
                            command.text = "():";
                            command.caretOffset = command.offset + 1;
                        } else if (hasClassMethodDef) {
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
            else if (command.text.equals(Character.toString(DocUtils.COLON)) && prefs.getAutoColon()) {
                performColonReplacement(document, command);
            }

            /*
             * If the command is some kind of parentheses or brace, and there's
             * already a matching one, don't insert it. Just move the cursor to
             * the next space.
             */
            else if (command.text.length() > 0 && prefs.getAutoBraces()) {
                // you can only do the replacement if the next character already there is what the user is trying to input
                if (command.offset < document.getLength()
                        && document.get(command.offset, 1).equals(command.text)) {
                    // the following searches through each of the end braces and
                    // sees if the command has one of them
                    boolean found = false;
                    for (int i = 1; i <= PyDoubleClickStrategy.BRACKETS.length && !found; i += 2) {
                        char c = PyDoubleClickStrategy.BRACKETS[i];
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
     * Create the indentation string after comma and a newline.
     * 
     * @param document
     * @param text
     * @param offset
     * @return Indentation String
     * @throws BadLocationException
     */
    private String indentAfterCommaNewline(IDocument document, String text, int offset)
            throws BadLocationException {
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
            while (spaceSteps-- >= 0)
                b.append(Character.toString(DocUtils.SPACE));

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

                if (currentCharacter == DocUtils.COLON) {
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
     */
    private void performPairReplacement(IDocument document, DocumentCommand command) {
        PySelection ps = new PySelection(document, command.offset);

        /*
         * Start matching.
         */
        /*
         * TODO: Might have to optimize and check for less braces if delay
         * between user's typing characters is too big.
         * 
         * You can do this by passing a smaller array to PythonPairMatcher()
         */
        PythonPairMatcher matcher = new PythonPairMatcher(PyDoubleClickStrategy.BRACKETS);
        IRegion region = matcher.match(ps.getDoc(), command.offset + 1);
        if (region != null) {
            command.text = DocUtils.EMPTY_STRING;
            command.caretOffset = command.offset + 1;
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
            if (!Character.isWhitespace(lineContentsFromCursor.charAt(i))) {
                return false;
            }
        }

        int i = PyAction.countChars(DocUtils.BEGIN_PARENTHESIS, line);
        int j = PyAction.countChars(DocUtils.END_PARENTHESIS, line);

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
                    if (c == DocUtils.TAB) {
                        smartIndent += prefs.getTabWidth() - 1;
                    }
                }
                return smartIndent;
            }
        }
        return -1;
    }
}