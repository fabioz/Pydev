/**
 * Copyright (c) 2014 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.actions;

import java.lang.ref.WeakReference;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.editor.BaseEditor;

import com.python.pydev.refactoring.RefactoringPlugin;
import com.python.pydev.refactoring.markoccurrences.MarkOccurrencesJob;
import com.python.pydev.refactoring.ui.MarkOccurrencesPreferencesPage;

public class ToggleMarkOccurrences extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        ITextEditor activeEditor = EditorUtils.getActiveEditor();
        if (!(activeEditor instanceof PyEdit)) {
            return null;
        }
        PyEdit editor = (PyEdit) activeEditor;

        try {
            IPreferenceStore store = RefactoringPlugin.getDefault().getPreferenceStore();
            boolean prev = store.getBoolean(MarkOccurrencesPreferencesPage.USE_MARK_OCCURRENCES);
            store.setValue(MarkOccurrencesPreferencesPage.USE_MARK_OCCURRENCES, !prev);

            editor.getStatusLineManager().setMessage(
                    "Toggled mark occurrences. Currently: " + (prev ? "Off" : "On"));

            MarkOccurrencesJob.scheduleRequest(new WeakReference<BaseEditor>(editor),
                    editor.createTextSelectionUtils(), 0); //On the action, ask it to happen now.
        } catch (Exception e) {
            Log.log(e);
        }

        return null;
    }

}
