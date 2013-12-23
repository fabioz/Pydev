/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Selects the matching bracket for the current bracket.
 */
public class PyGoToMatchingBracket extends PyAction {

    public void run(IAction action) {
        PyEdit pyEdit = getPyEdit();
        PySelection ps = new PySelection(pyEdit);
        if (ps.getSelLength() != 0) {
            return;
        }
        try {
            IDocument doc = ps.getDoc();
            char c = doc.getChar(ps.getAbsoluteCursorOffset() - 1);
            boolean opening = StringUtils.isOpeningPeer(c);
            boolean closing = StringUtils.isClosingPeer(c);

            if (opening || closing) {
                PythonPairMatcher matcher = new PythonPairMatcher();
                IRegion match = matcher.match(doc, ps.getAbsoluteCursorOffset());
                if (match != null) {
                    if (closing) {
                        pyEdit.setSelection(match.getOffset() + 1, 0);
                    } else {//opening
                        pyEdit.setSelection(match.getOffset() + match.getLength(), 0);
                    }
                }
            }
        } catch (BadLocationException e) {
            return;
        }
    }

}
