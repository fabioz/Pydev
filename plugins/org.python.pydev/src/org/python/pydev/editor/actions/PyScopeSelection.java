/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.actions.ScopeSelectionAction;

/**
 * @author fabioz
 *
 */
public class PyScopeSelection extends PyAction {

    @Override
    public void run(IAction action) {
        try {
            PyEdit editor = getPyEdit();
            IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();

            new ScopeSelectionAction().perform(doc, selection, editor);
        } catch (Exception e) {
            Log.log(e);
        }
    }
}
