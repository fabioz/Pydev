/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_interactive_console.console.ui.internal.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.log.Log;

/**
 * Deletes the previous word (ctrl+backspace)
 * 
 * @author fabioz
 */
public class HandleDeletePreviousWord {

    public void execute(IDocument doc, int caretPosition, int commandLineOffset) {
        int initialCaretPosition = caretPosition;
        //remove all whitespaces
        while (caretPosition > commandLineOffset) {
            try {
                char c = doc.getChar(caretPosition - 1);
                if (!Character.isWhitespace(c)) {
                    break;
                }
                caretPosition -= 1;
            } catch (BadLocationException e) {
                break;
            }
        }

        //remove a word
        while (caretPosition > commandLineOffset) {
            try {
                char c = doc.getChar(caretPosition - 1);
                if (!Character.isJavaIdentifierPart(c)) {
                    break;
                }
                caretPosition -= 1;
            } catch (BadLocationException e) {
                break;
            }
        }

        if (initialCaretPosition == caretPosition && initialCaretPosition > commandLineOffset) {
            caretPosition = initialCaretPosition - 1;
        }

        try {
            doc.replace(caretPosition, initialCaretPosition - caretPosition, "");
        } catch (BadLocationException e) {
            Log.log(e);
        }

    }

}
