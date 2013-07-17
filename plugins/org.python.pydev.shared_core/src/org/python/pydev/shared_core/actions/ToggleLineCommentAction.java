package org.python.pydev.shared_core.actions;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class ToggleLineCommentAction {
    private TextSelectionUtils ps;
    private String commentPattern;
    private int spacesInStart;

    public ToggleLineCommentAction(TextSelectionUtils ps, String commentPattern, int spacesInStart) {
        this.ps = ps;
        this.commentPattern = commentPattern;
        this.spacesInStart = spacesInStart;
    }

    public Tuple<Integer, Integer> execute() throws BadLocationException {
        // What we'll be replacing the selected text with

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        String selectedText = ps.getSelectedText();
        List<String> ret = StringUtils.splitInLines(selectedText);
        boolean allStartWithComments = true;
        for (String string : ret) {
            if (!StringUtils.leftTrim(string).startsWith(commentPattern)) {
                allStartWithComments = false;
                break;
            }
        }

        if (allStartWithComments) {
            return new LineUncommentAction(ps, commentPattern, spacesInStart).execute();
        } else {
            return new LineCommentAction(ps, commentPattern, spacesInStart).execute();
        }
    }
}
