package com.python.pydev.analysis.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.actions.PyAction;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class PyGlobalsBrowser extends PyAction{

	public void run(IAction action) {
		//check org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog2 (this is the class that does it for java)
//		List<AbstractAdditionalInterpreterInfo> additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfo(getPyEdit().getPythonNature());

//		TwoPaneElementSelector dialog = new TwoPaneElementSelector(getShell(), new LabelProvider(), new LabelProvider());
//        dialog.setTitle("Pydev: Globals Browser");
//        dialog.setMessage("Filter");
//        dialog.setInput(ast);
//        dialog.open();

	}

}