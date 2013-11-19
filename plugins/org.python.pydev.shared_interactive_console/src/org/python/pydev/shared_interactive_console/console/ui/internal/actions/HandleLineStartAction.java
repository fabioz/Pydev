/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_interactive_console.console.ui.internal.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleViewer;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsolePartitioner;
import org.python.pydev.shared_interactive_console.console.ui.ScriptStyleRange;

/**
 * Handles the action of going to the start of the line (Home)
 *
 * @author Fabio
 */
public class HandleLineStartAction {

    /**
     * When going to the line start, we must actually go to: 1st char / prompt end / line start (depending
     * on where we are currently)
     * 
     * @return true if it was done and false otherwise.
     */
    public boolean execute(IDocument doc, int caretOffset, int commandLineOffset, IScriptConsoleViewer viewer) {
        try {
            TextSelectionUtils ps = new TextSelectionUtils(doc, caretOffset);
            int lineOffset = ps.getLineOffset();

            int promptEndOffset = lineOffset;
            ScriptConsolePartitioner partitioner = (ScriptConsolePartitioner) doc.getDocumentPartitioner();
            int docLen = doc.getLength();

            for (; promptEndOffset < docLen; promptEndOffset++) {
                ScriptStyleRange[] range = partitioner.getStyleRanges(promptEndOffset, 1);
                if (range.length >= 1) {
                    if (range[0].scriptType != ScriptStyleRange.PROMPT) {
                        break;
                    }
                }
            }

            int absoluteCursorOffset = ps.getAbsoluteCursorOffset();

            IRegion lineInformation = doc.getLineInformationOfOffset(absoluteCursorOffset);
            String contentsFromPrompt = doc.get(promptEndOffset,
                    lineInformation.getOffset() + lineInformation.getLength() - promptEndOffset);
            int firstCharPosition = TextSelectionUtils.getFirstCharPosition(contentsFromPrompt);
            int firstCharOffset = promptEndOffset + firstCharPosition;

            //1st see: if we're in the start of the line, go to the 1st char after the prompt
            if (lineOffset == absoluteCursorOffset || firstCharOffset < absoluteCursorOffset) {
                viewer.setCaretOffset(firstCharOffset, false);
                return true;
            }

            if (promptEndOffset < absoluteCursorOffset) {
                viewer.setCaretOffset(promptEndOffset, false);
                return true;
            }

            viewer.setCaretOffset(lineOffset, false);
            return true;

        } catch (BadLocationException e) {
            Log.log(e);
        }
        return false;

    }
}
