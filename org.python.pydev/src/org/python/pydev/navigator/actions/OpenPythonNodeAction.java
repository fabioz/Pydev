/*
 * Created on Oct 9, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.navigator.PythonNode;
import org.python.pydev.outline.ParsedItem;

public class OpenPythonNodeAction extends Action {

    private IWorkbenchPage page;

    private PythonNode data;

    private ISelectionProvider provider;

    public OpenPythonNodeAction(IWorkbenchPage page, ISelectionProvider selectionProvider) {
        this.setText("Open python node");
        this.page = page;
        this.provider = selectionProvider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    public boolean isEnabled() {
        ISelection selection = provider.getSelection();
        if (!selection.isEmpty()) {
            IStructuredSelection sSelection = (IStructuredSelection) selection;
            if (sSelection.size() == 1 && sSelection.getFirstElement() instanceof PythonNode) {
                data = ((PythonNode) sSelection.getFirstElement());
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        if (isEnabled()) {
            ParsedItem actualObject = data.getActualObject();
            new PyOpenAction().run(new ItemPointer( data.getPythonFile().getActualObject(), actualObject.astThis.node));
        }
    }

}
