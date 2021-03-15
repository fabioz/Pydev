/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: fabioz
 * Created: January 2004
 */

package org.python.pydev.editor.actions;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
import org.python.pydev.shared_core.actions.LineCommentAction;
import org.python.pydev.shared_core.actions.LineCommentOption;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.EditorUtils;

/**
 * Creates a bulk comment. Comments all selected lines
 *
 * @author Fabio Zadrozny
 * @author Parhaum Toofanian
 */
public class PyComment extends PyAction {

    protected FormatStd std;

    public PyComment(FormatStd std) {
        super();
        this.std = std;
    }

    public PyComment() {
        this(null);
    }

    /**
     * Grabs the selection information and performs the action.
     */
    @Override
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }

            PyEdit pyEdit = getPyEdit();
            this.std = pyEdit.getFormatStd();

            TextSelectionUtils ps = EditorUtils.createTextSelectionUtils(pyEdit);
            // Perform the action
            IAdaptable projectAdaptable = getTextEditor();
            String commentOption = PyScopedPreferences.getString(CommentBlocksPreferences.ADD_COMMENTS_OPTION,
                    projectAdaptable);

            Tuple<Integer, Integer> repRegion = perform(ps, commentOption);

            // Put cursor at the first area of the selection
            pyEdit.selectAndReveal(repRegion.o1, repRegion.o2);
        } catch (Exception e) {
            beep(e);
        }
    }

    public Tuple<Integer, Integer> perform(TextSelectionUtils ps, String addCommentsOption)
            throws BadLocationException {
        return performComment(ps, addCommentsOption);
    }

    /**
     * Performs the action with a given PySelection
     *
     * @param ps Given PySelection
     * @return the new selection
     * @throws BadLocationException
     */
    protected Tuple<Integer, Integer> performComment(TextSelectionUtils ps, String addCommentsOption)
            throws BadLocationException {
        if (addCommentsOption == null
                || !CommentBlocksPreferences.getValuesForAddCommentsOption().contains(addCommentsOption)) {
            org.python.pydev.core.log.Log.log("Not expected add comments option.");
            addCommentsOption = LineCommentOption.DEFAULT_ADD_COMMENTS_OPTION;
        }
        int spacesInStart = this.std.spacesInStartComment;
        LineCommentAction lineCommentAction = new LineCommentAction(ps, "#", spacesInStart, addCommentsOption);
        return lineCommentAction.execute();
    }
}
