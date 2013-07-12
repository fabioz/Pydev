package org.python.pydev.shared_core.actions;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class LineUncommentAction {

    private TextSelectionUtils ps;
    private String commentPattern;
    private int spacesInStart;

    public LineUncommentAction(TextSelectionUtils ps, String commentPattern, int spacesInStart) {
        this.ps = ps;
        this.commentPattern = commentPattern;
        this.spacesInStart = spacesInStart;
    }

    public FastStringBuffer uncommentLines(String selectedText) {
        List<String> ret = StringUtils.splitInLines(selectedText);
        FastStringBuffer strbuf = new FastStringBuffer(selectedText.length() + (ret.size() * 2));

        String spacesInStartComment = "";
        if (spacesInStart > 0) {
            spacesInStartComment = StringUtils.createSpaceString(spacesInStart);
        }
        String expectedStart = this.commentPattern + spacesInStartComment;

        boolean allStartWithSpaces = true;
        for (String string : ret) {
            if (!StringUtils.leftTrim(string).startsWith(expectedStart)) {
                expectedStart = this.commentPattern;
                allStartWithSpaces = false;
                break;
            }
        }

        int expectedStartLength = expectedStart.length();
        if (allStartWithSpaces) {
            for (String line : ret) {
                int i = line.indexOf(expectedStart);
                strbuf.append(line.substring(0, i));
                strbuf.append(line.substring(i + expectedStartLength));
            }
        } else {
            for (String line : ret) {
                if (StringUtils.leftTrim(line).startsWith(expectedStart)) {
                    int i = line.indexOf(expectedStart);
                    strbuf.append(line.substring(0, i));
                    strbuf.append(line.substring(i + expectedStartLength));
                } else {
                    strbuf.append(line);
                }
            }
        }
        return strbuf;
    }

    public Tuple<Integer, Integer> execute() throws BadLocationException {
        // What we'll be replacing the selected text with

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        String selectedText = ps.getSelectedText();

        FastStringBuffer strbuf = uncommentLines(selectedText);
        ITextSelection txtSel = ps.getTextSelection();
        int start = txtSel.getOffset();
        int len = txtSel.getLength();

        String replacement = strbuf.toString();
        // Replace the text with the modified information
        ps.getDoc().replace(start, len, replacement);
        return new Tuple<Integer, Integer>(start, replacement.length());
    }
}
