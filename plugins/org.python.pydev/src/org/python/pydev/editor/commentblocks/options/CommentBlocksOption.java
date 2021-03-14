package org.python.pydev.editor.commentblocks.options;

import java.util.List;

import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class CommentBlocksOption {

    protected final String spacesInStartComment;
    protected String commentPattern;

    public CommentBlocksOption(int spacesInStart, String commentPattern) {
        this.spacesInStartComment = getSpacesInStartComment(spacesInStart);
        this.commentPattern = commentPattern;
    }

    private static String getSpacesInStartComment(int spacesInStart) {
        String spacesInStartComment = "";
        if (spacesInStart > 0) {
            spacesInStartComment = StringUtils.createSpaceString(spacesInStart);
        }
        return spacesInStartComment;
    }

    private static List<String> getLines(String text) {
        List<String> ret = StringUtils.splitInLines(text);
        if (ret.isEmpty()) {
            ret.add(text);
        } else {
            if (text.endsWith("\r") || text.endsWith("\n")) {
                ret.add("");
            }
        }
        return ret;
    }

    final public FastStringBuffer commentLines(String selectedText) {
        List<String> ret = getLines(selectedText);
        initCustomAttributes(ret);
        FastStringBuffer strbuf = new FastStringBuffer(selectedText.length() + ret.size()
                + ((spacesInStartComment.length() + 2) * ret.size()));
        for (String line : ret) {
            strbuf.append(getCommentedLine(line));
        }
        return strbuf;
    }

    protected void initCustomAttributes(List<String> lines) {
    }

    protected String getCommentedLine(String line) {
        return getCommentedLine(line, spacesInStartComment);
    }

    protected String getCommentedLine(String line, String spacesInStartComment) {
        return getCommentedLine(0, line, spacesInStartComment);
    }

    protected String getCommentedLine(int spacesBeforeComment, String line, String spacesInStartComment) {
        return StringUtils.createSpaceString(spacesBeforeComment) + commentPattern + spacesInStartComment + line;
    }

}
