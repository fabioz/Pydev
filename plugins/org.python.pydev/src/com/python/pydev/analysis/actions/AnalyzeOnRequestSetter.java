/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.parser.PyParser;
import org.python.pydev.shared_core.IMiscConstants;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;

public class AnalyzeOnRequestSetter implements IPyEditListener {

    public static class AnalyzeOnRequestAction extends Action {

        private IPyEdit edit;

        public AnalyzeOnRequestAction(IPyEdit edit) {
            this.edit = edit;
        }

        @Override
        public void run() {
            PyParser parser = (PyParser) edit.getParser();
            parser.forceReparse(
                    new Tuple<String, Boolean>(IMiscConstants.ANALYSIS_PARSER_OBSERVER_FORCE, true));
        }
    }

    @Override
    public void onSave(BaseEditor edit, IProgressMonitor monitor) {
    }

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {
        IPyEdit edit = (IPyEdit) baseEditor;
        AnalyzeOnRequestAction action = new AnalyzeOnRequestAction(edit);
        edit.addOfflineActionListener("c", action, "Code-analysis on request", true);
    }

    @Override
    public void onDispose(BaseEditor edit, IProgressMonitor monitor) {
    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor edit, IProgressMonitor monitor) {
    }

}
