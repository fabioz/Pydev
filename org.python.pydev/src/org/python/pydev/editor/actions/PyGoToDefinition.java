/*
 * Created on May 21, 2004
 *
 */
package org.python.pydev.editor.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.model.ModelUtils;

/**
 * @author Fabio Zadrozny
 *
 */
public class PyGoToDefinition extends PyAction{

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		try{
			PyEdit pyEdit = getPyEdit();
			IDocument doc = pyEdit.getDocumentProvider().getDocument(pyEdit.getEditorInput());
			ITextSelection selection =
				(ITextSelection) pyEdit.getSelectionProvider().getSelection();
	
			Location loc = Location.offsetToLocation(doc, selection.getOffset());
			AbstractNode node = ModelUtils.getElement(pyEdit.getPythonModel(),loc, AbstractNode.PROP_CLICKABLE);
			
			if(node == null)
				return;
				
			PyOpenAction openAction = (PyOpenAction)pyEdit.getAction(PyEdit.ACTION_OPEN);
	
			ArrayList where = ModelUtils.findDefinition(node);
			if (where.size() > 0)
				openAction.run((ItemPointer)where.get(0));
			else
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay().beep();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

}
