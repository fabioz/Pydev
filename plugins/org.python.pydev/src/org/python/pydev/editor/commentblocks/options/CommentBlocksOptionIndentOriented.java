package org.python.pydev.editor.commentblocks.options;

import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.TextSelectionUtils;

public class CommentBlocksOptionIndentOriented extends CommentBlocksOption {
    private int lastFirstCharPosition;
    private FastStringBuffer lineBuf;

    public CommentBlocksOptionIndentOriented(int spacesInStart, String commentPattern) {
        super(spacesInStart, commentPattern);
        lastFirstCharPosition = 0;
        lineBuf = new FastStringBuffer();
    }

    @Override
    protected String getCommentedLine(String line) {
        if (line.length() == 0) {
            return line;
        }
        lineBuf.clear();
        lineBuf.append(line);
        lineBuf.leftTrim();

        boolean addSpacesInStartComment = true;
        if (lineBuf.length() == 0) {
            // Not even the '\n' at the end of the line remained.
            addSpacesInStartComment = false;
            lineBuf.append(line);
            if (lastFirstCharPosition > 0) {
                lineBuf.leftTrimSpacesAndTabs(); // i.e.: don't trim new lines this time.
            } else {
                // i.e.: empty line and there were no contents before, let's keep the current
                // indent.
                lineBuf.rightTrimNewLines();
                lastFirstCharPosition = lineBuf.length();
                lineBuf.clear();
                lineBuf.append(line);
                lineBuf.leftTrimSpacesAndTabs();
            }
        } else {
            lastFirstCharPosition = TextSelectionUtils.getFirstCharPosition(line);
        }
        if (addSpacesInStartComment) {
            return getCommentedLine(lastFirstCharPosition, lineBuf.toString(), spacesInStartComment);
        } else {
            return getCommentedLine(lastFirstCharPosition, lineBuf.toString(), "");
        }
    }
}
