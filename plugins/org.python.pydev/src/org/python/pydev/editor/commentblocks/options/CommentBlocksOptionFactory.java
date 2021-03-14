package org.python.pydev.editor.commentblocks.options;

import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;

public class CommentBlocksOptionFactory {

    public static CommentBlocksOption createCommentBlocksOption(String option) {
        return createCommentBlocksOption(option, 1);
    }

    public static CommentBlocksOption createCommentBlocksOption(String option, int spacesInStart) {
        return createCommentBlocksOption(option, spacesInStart, "#");
    }

    public static CommentBlocksOption createCommentBlocksOption(String option, int spacesInStart,
            String commentPattern) {
        if (CommentBlocksPreferences.ADD_COMMENTS_AT_INDENT.equals(option)) {
            return new CommentBlocksOptionAtIndent(spacesInStart, commentPattern);
        } else if (CommentBlocksPreferences.ADD_COMMENTS_INDENT_ORIENTED.equals(option)) {
            return new CommentBlocksOptionIndentOriented(spacesInStart, commentPattern);
        } else {
            return new CommentBlocksOption(spacesInStart, commentPattern);
        }
    }

}
