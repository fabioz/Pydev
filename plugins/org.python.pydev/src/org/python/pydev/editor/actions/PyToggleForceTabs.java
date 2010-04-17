package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.editor.PyEdit;

public class PyToggleForceTabs extends PyAction{
	

	public void run(IAction action) {
		if(targetEditor instanceof PyEdit){
			PyEdit pyEdit = (PyEdit) targetEditor;
			IIndentPrefs indentPrefs = pyEdit.getIndentPrefs();
			indentPrefs.setForceTabs(!indentPrefs.getForceTabs());
			updateActionState(indentPrefs);
		}
	}

	private void updateActionState(IIndentPrefs indentPrefs) {
		//This doesn't work! (setChecked and setImageDescriptor don't seem to update the action in the pop up menu).
//		setChecked(forceTabs);
//		setImageDescriptor(desc);
		
		PyEdit pyEdit = getPyEdit();
		pyEdit.updateForceTabsMessage();
	}

}
