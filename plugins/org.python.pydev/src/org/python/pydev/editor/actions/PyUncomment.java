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

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PyFormatStd.FormatStd;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author fabioz
 */
public class PyUncomment extends PyComment {

    public PyUncomment(FormatStd std) {
        super(std);
    }

    public PyUncomment() {
        this(null);
    }

    /* Selection element */

    @Override
    public Tuple<Integer, Integer> perform(TextSelectionUtils ps) throws BadLocationException {
        return performUncomment(ps);
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
     * @return the new selection
     * @throws BadLocationException 
     */
    protected Tuple<Integer, Integer> performUncomment(TextSelectionUtils ps) throws BadLocationException {
        PyRemoveBlockComment pyRemoveBlockComment = new PyRemoveBlockComment();
        return pyRemoveBlockComment.performUncommentBlock(ps, ps.getStartLineIndex(), ps.getEndLineIndex());
    }

}