/**
 * Copyright (c) 2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyFormatStd;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class IgnoreCompletionProposalInSameLine extends IgnoreCompletionProposal {

    private String line;
    private PySelection ps;
    private FormatStd format;

    public IgnoreCompletionProposalInSameLine(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, PyEdit edit, String line, PySelection ps, FormatStd format) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, edit);
        this.line = line; //the current line
        this.ps = ps;
        this.format = format; //may be null (in which case we get it from the editor or the default one).
    }

    @Override
    public void apply(IDocument document) {
        String messageToIgnore = fReplacementString;
        FastStringBuffer strToAdd = new FastStringBuffer(messageToIgnore, 5);
        int lineLen = line.length();

        int endLineIndex = ps.getEndLineOffset();
        boolean isComment = ParsingUtils.isCommentPartition(document, endLineIndex);

        int whitespacesAtEnd = 0;
        char c = '\0';
        for (int i = lineLen - 1; i >= 0; i--) {
            c = line.charAt(i);
            if (c == ' ') {
                whitespacesAtEnd += 1;
            } else {
                break;
            }
        }

        if (isComment) {
            if (whitespacesAtEnd == 0) {
                strToAdd.insert(0, ' '); //it's a comment already, but as it has no spaces in the end, let's add one.
            }

        } else {
            FormatStd formatStd = this.format;
            if (formatStd == null) {
                if (edit != null) {
                    formatStd = edit.getFormatStd();
                } else {
                    // Shouldn't happen when not in test mode
                    Log.log("Error: using default format (not considering project preferences).");
                    formatStd = PyFormatStd.getFormat(null);
                }
            }

            strToAdd.insert(0, '#');
            PyFormatStd.formatComment(formatStd, strToAdd);

            //Just add spaces before the '#' if there's actually some content in the line.
            if (c != '\r' && c != '\n' && c != '\0' && c != ' ') {
                int spacesBeforeComment = formatStd.spacesBeforeComment;
                if (spacesBeforeComment < 0) {
                    spacesBeforeComment = 1; //If 'manual', add a single space.
                }
                spacesBeforeComment = spacesBeforeComment - whitespacesAtEnd;
                if (spacesBeforeComment > 0) {
                    strToAdd.insertN(0, ' ', spacesBeforeComment);
                }
            }
        }

        fReplacementString = strToAdd.toString();
        super.apply(document);
    }

}
