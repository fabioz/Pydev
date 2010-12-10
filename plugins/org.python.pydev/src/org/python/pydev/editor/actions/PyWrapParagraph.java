package org.python.pydev.editor.actions;

import java.util.Collection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.editor.ActionInfo;
import org.python.pydev.editor.PyEdit;

public class PyWrapParagraph extends PyAction {
    
    /**
     * Makes the wrap paragraph (registered from the scripting engine).
     */
    public void run(IAction action) {
        try {
        	if(!canModifyEditor()){
        		return;
        	}

        	PyEdit pyEdit = getPyEdit();
        	Collection<ActionInfo> offlineActionDescriptions = pyEdit.getOfflineActionDescriptions();
        	for (ActionInfo actionInfo : offlineActionDescriptions) {
				if("wrap paragraph".equals(actionInfo.description.trim().toLowerCase())){
					actionInfo.action.run();
					return;
				}
			}
        	MessageDialog.openError(getShell(), "Error", "Wrap paragraph is still not available.");
        } catch (Exception e) {
            beep(e);
        }
    }
}
         
