/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.model.*;

/**
 * The trick here is getting the outline... To do that, some refactorings had
 * to be done to the PyOutlinePage, to get the parsed items and the ParsedItem,
 * so that it is now public.
 * 
 * @author Fabio Zadrozny
 */
public abstract class PyMethodNavigation extends PyAction {

	/**
	 * This method gets the parsed model, discovers where we are in the
	 * document (through the visitor), and asks the implementing class
	 * to where we should go... 
	 */
	public void run(IAction action) {
		PyEdit pyEdit = getPyEdit();
		IDocument doc = pyEdit.getDocumentProvider().getDocument(pyEdit.getEditorInput());
		ITextSelection selection =
			(ITextSelection) pyEdit.getSelectionProvider().getSelection();

		Location loc = Location.offsetToLocation(doc, selection.getOffset());
		AbstractNode closest = ModelUtils.getLessOrEqualNode(pyEdit.getPythonModel(),loc);
	
		AbstractNode goHere = getSelect(closest);
		pyEdit.revealModelNode(goHere);
	}

	/**
	 * This method should return to where we should go, depending on
	 * the visitor passed as a parameter (it contains the node where we
	 * are, the next node and the previous node).
	 * 
	 * @param v
	 * @return where we should go depending on visitor
	 */
	public abstract AbstractNode getSelect(AbstractNode v);

}
