/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Dialog to choose a Python module from a project
 * 
 * @author Mikko Ohtamaa
 */
public class PythonModulePickerDialog extends ElementTreeSelectionDialog {

    public PythonModulePickerDialog(Shell parent, String title, String message, IProject project, boolean unitTesting) {
        super(parent, new WorkbenchLabelProvider(), new PythonModuleContentProvider());
        setAllowMultiple(unitTesting);
        this.setEmptyListMessage("No Python modules in project " + project.getName());
        this.setInput(project);
        this.setTitle(title);
        this.setMessage(message);

        // Do not allow multiple selection of packages / modules for non-Unittest runs
        if (!unitTesting) {
            this.setValidator(new ISelectionStatusValidator() {
                public IStatus validate(Object selection[]) {
                    if (selection.length >= 1) {
                        if (selection[0] instanceof IFile) {
                            IFile file = (IFile) selection[0];
                            return new Status(IStatus.OK, PydevPlugin.getPluginID(), IStatus.OK, "Module  "
                                    + file.getName() + " selected", null);

                        } else if (selection[0] instanceof IFolder) {
                            IFolder folder = (IFolder) selection[0];

                            if (folder.findMember("__main__.py") == null) {
                                return new Status(IStatus.ERROR, PydevPlugin.getPluginID(), IStatus.ERROR,
                                        "Can't find '__main__' module in this folder.",
                                        null);
                            } else {
                                return new Status(IStatus.OK, PydevPlugin.getPluginID(), IStatus.OK, "Module "
                                        + folder.getName() + " selected", null);
                            }
                        }
                    }
                    return new Status(IStatus.ERROR, PydevPlugin.getPluginID(), IStatus.ERROR,
                            "No Python module selected",
                            null);

                }
            });
        }
        else {
            this.setValidator(new ISelectionStatusValidator() {
                public IStatus validate(Object selection[]) {
                    for (int i = 0; i < selection.length; i++) {
                        if (!(selection[i] instanceof IFolder)) {
                            continue;
                        }
                        IFolder folder = (IFolder) selection[i];
                        // Ignore .* folders, like .settings
                        if (folder.getFileExtension() != null)
                        {
                            return new Status(IStatus.ERROR, PydevPlugin.getPluginID(), IStatus.ERROR,
                                    "Invalid selection: " + folder.getFileExtension(), null);
                        }
                    }
                    if (selection.length == 1) {
                        if (selection[0] instanceof IFile) {
                            IFile file = (IFile) selection[0];
                            return new Status(IStatus.OK, PydevPlugin.getPluginID(), IStatus.OK, "Module "
                                    + file.getName() + " selected", null);
                        }
                        else if (selection[0] instanceof IFolder) {
                            IFolder folder = (IFolder) selection[0];
                            return new Status(IStatus.OK, PydevPlugin.getPluginID(), IStatus.OK, "Package "
                                    + folder.getName() + " selected", null);
                        }
                    }
                    else if (selection.length > 1) {
                        return new Status(IStatus.OK, PydevPlugin.getPluginID(), IStatus.OK, "Multiple items selected",
                                null);
                    }
                    return new Status(IStatus.ERROR, PydevPlugin.getPluginID(), IStatus.ERROR, "Nothing selected",
                            null);

                }
            });
        }
    }
}

class PythonModuleContentProvider implements ITreeContentProvider {

    /**
     * Creates a new ContainerContentProvider.
     */
    PythonModuleContentProvider() {
    }

    /**
     * The visual part that is using this content provider is about
     * to be disposed. Deallocate all allocated SWT resources.
     */
    public void dispose() {
    }

    /*
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object element) {

        if (element instanceof IContainer) {
            IContainer container = (IContainer) element;

            if (container.isAccessible()) {
                try {
                    List<IResource> children = new ArrayList<IResource>();

                    IResource[] members = container.members();

                    for (int i = 0; i < members.length; i++) {

                        if (members[i] instanceof IFile) {

                            IFile file = (IFile) members[i];

                            if (PythonPathHelper.isValidSourceFile(file)) {
                                children.add(file);
                            }
                        } else if (members[i] instanceof IContainer) {
                            children.add(members[i]);
                        }
                    }
                    return children.toArray();
                } catch (CoreException e) {
                    // this should never happen because we call #isAccessible before invoking #members
                }
            }
        }

        return new Object[0];
    }

    /*
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object element) {
        return getChildren(element);
    }

    /*
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
        if (element instanceof IResource) {
            return ((IResource) element).getParent();
        }
        return null;
    }

    /*
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

    /*
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
