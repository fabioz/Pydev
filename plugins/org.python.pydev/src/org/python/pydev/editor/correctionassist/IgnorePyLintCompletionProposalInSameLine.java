package org.python.pydev.editor.correctionassist;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.python.pydev.ast.formatter.PyFormatter;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.PyFormatterPreferences;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class IgnorePyLintCompletionProposalInSameLine extends IgnoreCompletionProposal {

    private String line;
    private PySelection ps;
    private FormatStd format;
    private IMarker marker;

    public IgnorePyLintCompletionProposalInSameLine(String replacementString, int replacementOffset,
            int replacementLength,
            int cursorPosition, IImageHandle image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, IPyEdit edit, String line, PySelection ps, FormatStd format,
            IMarker marker) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, edit);
        this.line = line; //the current line
        this.ps = ps;
        this.format = format; //may be null (in which case we get it from the editor or the default one).
        this.marker = marker;
    }

    @Override
    protected boolean getForceReparse() {
        return false;
    }

    @Override
    public void apply(IDocument document) {
        String messageId = fReplacementString;
        FastStringBuffer strToAdd;
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
            strToAdd = new FastStringBuffer("", 40);

            if (line.contains("pylint:") && line.contains("disable=")) {
                // We can add it to an existing declaration
                strToAdd.append(", ");
                strToAdd.append(messageId);
            } else {
                // There is already a comment, but no indication of a PyLint ignore, so, just add it.
                if (whitespacesAtEnd == 0) {
                    strToAdd.append(' '); //it's a comment already, but as it has no spaces in the end, let's add one.
                }
                strToAdd.append("pylint: disable=");
                strToAdd.append(messageId);
            }

        } else {
            FormatStd formatStd = this.format;
            if (formatStd == null) {
                if (edit != null) {
                    formatStd = ((PyEdit) edit).getFormatStd();
                } else {
                    // Shouldn't happen when not in test mode
                    Log.log("Error: using default format (not considering project preferences).");
                    formatStd = PyFormatterPreferences.getFormatStd(null);
                }
            }

            strToAdd = new FastStringBuffer("", 40);
            strToAdd.append('#');
            PyFormatter.formatComment(formatStd, strToAdd, true);

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
            strToAdd.append("pylint: disable=");
            strToAdd.append(messageId);
        }

        fReplacementString = strToAdd.toString();
        super.apply(document);

        if (this.marker != null) {
            try {
                this.marker.delete();
            } catch (CoreException e) {
                Log.log(e);
            }
        }
    }

}
