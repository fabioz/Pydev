package org.python.pydev.core.docstrings;

import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.DocstringInfo;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class AssistDocstringProposalCore {

    private final String initial;
    private final String delimiter;
    private final String docStringMarker;
    private final String delimiterAndIndent;
    private final String preferredDocstringStyle2;
    private final boolean inFunctionLine;
    private final DocstringInfo finalDocstringFromFunction;
    private final String indentation;
    private final FastStringBuffer buf;
    private final List<String> params;

    public String fReplacementString;
    public int fReplacementOffset;
    public int fReplacementLength;
    public int fCursorPosition;

    public AssistDocstringProposalCore(String initial, String delimiter, String docStringMarker,
            String delimiterAndIndent, String preferredDocstringStyle2, boolean inFunctionLine,
            DocstringInfo finalDocstringFromFunction, String indentation, FastStringBuffer buf, List<String> params,
            String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition) {
        this.initial = initial;
        this.delimiter = delimiter;
        this.docStringMarker = docStringMarker;
        this.delimiterAndIndent = delimiterAndIndent;
        this.preferredDocstringStyle2 = preferredDocstringStyle2;
        this.inFunctionLine = inFunctionLine;
        this.finalDocstringFromFunction = finalDocstringFromFunction;
        this.indentation = indentation;
        this.buf = buf;
        this.params = params;

        this.fReplacementString = replacementString;
        this.fReplacementOffset = replacementOffset;
        this.fReplacementLength = replacementLength;
        this.fCursorPosition = cursorPosition;
    }

    public ReplaceEdit createTextEdit(IDocument document) {
        if (inFunctionLine) {
            // Let's check if this function already has a docstring (if it does, update the current docstring
            // instead of creating a new one).
            String updatedDocstring = null;
            if (finalDocstringFromFunction != null) {
                updatedDocstring = AssistDocString.updatedDocstring(finalDocstringFromFunction.string, params,
                        delimiter,
                        initial + indentation,
                        preferredDocstringStyle2);
            }
            if (updatedDocstring != null) {
                fReplacementOffset = finalDocstringFromFunction.startLiteralOffset;
                fReplacementLength = finalDocstringFromFunction.endLiteralOffset
                        - finalDocstringFromFunction.startLiteralOffset;
                int initialLen = buf.length();
                buf.clear();
                fCursorPosition -= initialLen - buf.length();
                buf.append(updatedDocstring);
            } else {
                // Create the docstrings
                for (String paramName : params) {
                    if (!PySelection.isIdentifier(paramName)) {
                        continue;
                    }
                    if (this.preferredDocstringStyle2.equals("G")) {
                        buf.append(delimiterAndIndent).append(indentation).append(paramName).append(":");
                    } else {
                        buf.append(delimiterAndIndent).append(preferredDocstringStyle2).append("param ")
                                .append(paramName)
                                .append(":");
                    }
                    if (DocstringPreferences.getTypeTagShouldBeGenerated(paramName)) {
                        buf.append(delimiterAndIndent).append(preferredDocstringStyle2).append("type ")
                                .append(paramName)
                                .append(":");
                    }
                }
            }

        } else {
            // It's a class declaration - do nothing.
        }
        if (finalDocstringFromFunction == null) {
            buf.append(delimiterAndIndent).append(docStringMarker);
        }

        String comp = buf.toString();
        this.fReplacementString = comp;

        //remove the next line if it is a pass...
        if (finalDocstringFromFunction == null) {
            PySelection ps = new PySelection(document, fReplacementOffset);
            int iNextLine = ps.getCursorLine() + 1;
            String nextLine = ps.getLine(iNextLine);
            if (nextLine.trim().equals("pass")) {
                try {
                    IRegion lineInformation = document.getLineInformation(iNextLine);
                    int offset = lineInformation.getOffset();

                    int length = -1;

                    if (document.getNumberOfLines() > iNextLine) {
                        int nextLineOffset = document.getLineInformation(iNextLine + 1).getOffset();
                        length = nextLineOffset - offset;
                    } else {
                        length = lineInformation.getLength();
                    }

                    if (length > -1) {
                        int endOffset = offset + nextLine.length();
                        fReplacementLength = endOffset - fReplacementOffset;
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }
        ReplaceEdit replaceEdit = new ReplaceEdit(fReplacementOffset, fReplacementLength, fReplacementString);
        return replaceEdit;
    }
}
