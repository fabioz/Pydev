package org.python.pydev.editor.commentblocks.options;

import java.util.List;

import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class CommentBlocksOptionAtIndent extends CommentBlocksOption {

    private int lowestIndent;
    private FastStringBuffer strBuf;

    public CommentBlocksOptionAtIndent(int spacesInStart, String commentPattern) {
        super(spacesInStart, commentPattern);
        lowestIndent = -1;
        strBuf = new FastStringBuffer();
    }

    private static int getLowestIndent(List<String> lines) {
        int lowestIndent = -1;
        for (String line : lines) {
            int indent = PySelection.getIndentationFromLine(line).length();
            if (lowestIndent == -1 || indent < lowestIndent) {
                lowestIndent = indent;
            }
        }
        return lowestIndent;
    }

    @Override
    protected void initCustomAttributes(List<String> lines) {
        this.lowestIndent = getLowestIndent(lines);
    }

    @Override
    protected String getCommentedLine(String line) {
        strBuf.clear().append(line);
        strBuf.deleteFirstChars(lowestIndent);
        return getCommentedLine(lowestIndent, strBuf.toString(), spacesInStartComment);
    }

}
