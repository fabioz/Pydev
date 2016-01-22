/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.auto_edit;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.TextSelectionUtils;

public class AutoEditStrategyNewLineHelper {

    public static boolean isNewLineText(IDocument document, int length, String text) {
        return length == 0 && text != null && TextSelectionUtils.endsWithNewline(document, text) && text.length() < 3; //could be \r\n
    }

    protected int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
        while (offset < end) {
            char c = document.getChar(offset);
            if (c != ' ' && c != '\t') {
                return offset;
            }
            offset++;
        }
        return end;
    }

    public void handleNewLine(TextSelectionUtils ts, String contentType, DocumentCommand command,
            String regularIndent) {
        IDocument document = ts.getDoc();
        if (command.offset == -1 || document.getLength() == 0) {
            return;
        }

        String prevLineIndent = getPreviousLineIndent(command, document);

        boolean insideBrackets = false;
        try {
            String found = document.get(command.offset - 1, 2);
            if (found.equals("{}") || found.equals("><")) {
                insideBrackets = true;
            }
        } catch (BadLocationException e) {
            //ignore
        }

        if (insideBrackets) {
            command.shiftsCaret = false;
            command.caretOffset = command.offset + command.text.length() + prevLineIndent.length()
                    + regularIndent.length();
            command.text = new FastStringBuffer(command.text, prevLineIndent.length() + 10).append(prevLineIndent)
                    .append(regularIndent).append(command.text).append(prevLineIndent).toString();
        } else {
            command.text = new FastStringBuffer(command.text, prevLineIndent.length()).append(prevLineIndent)
                    .toString();
        }
    }

    public String getPreviousLineIndent(DocumentCommand command, IDocument document) {
        String prevLineIndent = "";
        try {
            // find start of line
            int p = (command.offset == document.getLength() ? command.offset - 1 : command.offset);
            IRegion info = document.getLineInformationOfOffset(p);
            int start = info.getOffset();

            // find white spaces
            int end = findEndOfWhiteSpace(document, start, command.offset);

            if (end > start) {
                prevLineIndent = document.get(start, end - start);
            }
        } catch (BadLocationException e) {
            //ignore
        }
        return prevLineIndent;
    }
}
