/*
 * Created on May 21, 2004
 *
 */
package org.python.pydev.editor.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.model.ModelUtils;
import org.python.pydev.editor.refactoring.PyRefactoring;

/**
 * @author Fabio Zadrozny
 *  
 */
public class PyGoToDefinition extends PyRefactorAction {

    protected boolean areRefactorPreconditionsOK(PyEdit edit) {

        if(edit.isDirty())
            edit.doSave(null);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        try {

            ps = new PySelection(getTextEditor());
            PyEdit pyEdit = getPyEdit();
            areRefactorPreconditionsOK(pyEdit);

            PyOpenAction openAction = (PyOpenAction) pyEdit
                    .getAction(PyEdit.ACTION_OPEN);

            List where = findDefinition(pyEdit);

            if (where == null) {
                return;
            }

            if (where.size() > 0)
                openAction.run((ItemPointer) where.get(0));
            else
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
                        .getDisplay().beep();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param node
     * @return
     */
    private List findDefinition(PyEdit pyEdit) {
        if(true){
            PyRefactoring pyRefactoring = PyRefactoring.getPyRefactoring();
            return pyRefactoring.findDefinition(pyEdit.getEditorFile(), getStartLine(), getStartCol(), null);
            
        }else{ //kept earlier version (may be useful).
	        IDocument doc = pyEdit.getDocumentProvider().getDocument(
	                pyEdit.getEditorInput());
	        ITextSelection selection = (ITextSelection) pyEdit
	                .getSelectionProvider().getSelection();
	
	        Location loc = Location.offsetToLocation(doc, selection.getOffset());
	        AbstractNode node = ModelUtils.getElement(pyEdit.getPythonModel(), loc,
	                AbstractNode.PROP_CLICKABLE);
	
	        if (node == null)
	            return null;
	        return ModelUtils.findDefinition(node);
        }
    }

    protected String perform(IAction action, String name, Operation operation)
            throws Exception {
        return null;
    }

    protected String getInputMessage() {
        return null;
    }

}