package org.python.pydev.navigator.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.DeleteResourceAction;

/**
 * Overriden org.eclipse.ui.actions.DeleteResourceAction
 * 
 * with the following changes:
 * - isEnabled overriden to compute the changes accordingly
 * - in the run we update the selection correctly (by calling isEnabled), because this was not synched correctly in the 
 * eclipse version (because there could be a delay there).
 * 
 * @author Fabio
 */
public class PyDeleteResourceAction extends DeleteResourceAction {

    private ISelectionProvider provider;

    private ArrayList<IResource> selected;

    public PyDeleteResourceAction(Shell shell, ISelectionProvider selectionProvider) {
        super(shell);
        this.provider = selectionProvider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    public boolean isEnabled() {
        return true;
    }

    private boolean fillSelection() {
        selected = new ArrayList<IResource>();

        ISelection selection = provider.getSelection();
        if (!selection.isEmpty()) {
            IStructuredSelection sSelection = (IStructuredSelection) selection;
            if (sSelection.size() >= 1) {
                Iterator iterator = sSelection.iterator();
                while (iterator.hasNext()) {
                    Object element = iterator.next();
                    if (element instanceof IAdaptable) {
                        IAdaptable adaptable = (IAdaptable) element;
                        IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                        if (resource != null) {
                            selected.add(resource);
                            continue;
                        }
                    }
                    // one of the elements did not satisfy the condition
                    selected = null;
                    return false;
                }
            }
        }
        return true;
    }
    

	@Override
	public IStructuredSelection getStructuredSelection() {
		ISelection selection = provider.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			return sSelection;
		}
		return new StructuredSelection();
	}
	
    @Override
    protected List getSelectedResources() {
        return selected;
    }
    
    /*
     * (non-Javadoc) Method declared on IAction.
     */
    public void run() {
        if(!fillSelection()){ //will also update the list of resources (main change from the DeleteResourceAction)
            return;
        }
        super.run();
    }
}