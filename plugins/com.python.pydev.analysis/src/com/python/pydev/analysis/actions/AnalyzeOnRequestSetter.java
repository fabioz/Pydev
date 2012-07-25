/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.PyParser;

import com.python.pydev.analysis.builder.AnalysisParserObserver;

public class AnalyzeOnRequestSetter implements IPyEditListener {

    public static class AnalyzeOnRequestAction extends Action {

        private PyEdit edit;

        public AnalyzeOnRequestAction(PyEdit edit) {
            this.edit = edit;
        }

        public void run() {
            PyParser parser = edit.getParser();
            parser.forceReparse(new Tuple<String, Boolean>(AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true));
        }
    }

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
        AnalyzeOnRequestAction action = new AnalyzeOnRequestAction(edit);
        edit.addOfflineActionListener("c", action, "Code-analysis on request", false);
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
    }

}
