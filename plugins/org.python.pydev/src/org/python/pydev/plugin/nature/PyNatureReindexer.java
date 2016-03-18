/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.nature;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

public class PyNatureReindexer implements IPyEditListener {

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
        PyEdit edit = (PyEdit) baseEditor;
        edit.addOfflineActionListener("--reindex", new Action() {
            @Override
            public void run() {
                for (IPythonNature nature : PythonNature.getAllPythonNatures()) {
                    nature.rebuildPath();
                }
            }
        }, "Rebuilds the internal structure for all Pydev projects.", true);
    }

    @Override
    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {
    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {
    }

}
