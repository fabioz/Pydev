package com.python.pydev.analysis.actions;

import java.util.ListResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;

import com.python.pydev.analysis.builder.AnalysisParserObserver;

public class AnalyzeOnRequestSetter implements IPyEditListener{

	public static class AnalyzeOnRequestAction extends Action {

		private PyEdit edit;

		public AnalyzeOnRequestAction(PyEdit edit) {
			this.edit = edit;
		}
		public  void run(){
			//just send a reparse
			edit.getParser().parseNow(true, new Tuple<String, Boolean>(AnalysisParserObserver.ANALYSIS_PARSER_OBSERVER_FORCE, true));
		}
	}
	
	public void onSave(PyEdit edit) {
	}

	public void onCreateActions(ListResourceBundle resources, PyEdit edit) {
		AnalyzeOnRequestAction action = new AnalyzeOnRequestAction(edit);
		edit.addOfflineActionListener("c", action, "Code-analysis on request", true);
	}

	public void onDispose(PyEdit edit) {
	}

	public void onSetDocument(IDocument document, PyEdit edit) {
	}

}
