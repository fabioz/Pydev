package org.python.pydev.navigator.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.RenameResourceAction;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;

public class PyRenameResourceAction extends RenameResourceAction{
    
    private ISelectionProvider provider;

    private ArrayList<IResource> selected;

    private Shell shell;

    public PyRenameResourceAction(Shell shell, ISelectionProvider selectionProvider) {
        super(shell);
        this.shell = shell;
        this.provider = selectionProvider;
    }
    

    /**
     * Return the new name to be given to the target resource.
     *
     * @return java.lang.String
     * @param resource the resource to query status on
     * 
     * Fix from platform: was not checking return from dialog.open
     */
    @Override
    protected String queryNewResourceName(final IResource resource) {
        final IWorkspace workspace = IDEWorkbenchPlugin.getPluginWorkspace();
        final IPath prefix = resource.getFullPath().removeLastSegments(1);
        IInputValidator validator = new IInputValidator() {
            public String isValid(String string) {
                if (resource.getName().equals(string)) {
                    return IDEWorkbenchMessages.RenameResourceAction_nameMustBeDifferent;
                }
                IStatus status = workspace.validateName(string, resource
                        .getType());
                if (!status.isOK()) {
                    return status.getMessage();
                }
                if (workspace.getRoot().exists(prefix.append(string))) {
                    return IDEWorkbenchMessages.RenameResourceAction_nameExists;
                }
                return null;
            }
        };

        InputDialog dialog = new InputDialog(shell, IDEWorkbenchMessages.RenameResourceAction_inputDialogTitle,
                IDEWorkbenchMessages.RenameResourceAction_inputDialogMessage,
                resource.getName(), validator);
        dialog.setBlockOnOpen(true);
        if(dialog.open() == dialog.OK){
            return dialog.getValue();
        }else{
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    public boolean isEnabled() {
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
    protected List getSelectedResources() {
        return selected;
    }
    
    @Override
    public IStructuredSelection getStructuredSelection() {
        return new StructuredSelection(selected);
    }
    
    /*
     * (non-Javadoc) Method declared on IAction.
     */
    public void run() {
        if(!isEnabled()){ //will also update the list of resources (main change from the DeleteResourceAction)
            return;
        }
        super.run();
    }


}
