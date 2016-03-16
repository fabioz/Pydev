/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 19, 2006
 */
package org.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.python.copiedfromeclipsesrc.CopiedWorkbenchLabelProvider;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_ui.UIConstants;


/**
 * Dialog to select some source folder (in the workspace)
 */
public class PythonPackageSelectionDialog extends ElementTreeSelectionDialog {

    public boolean selectOnlySourceFolders;

    public PythonPackageSelectionDialog(Shell parent, final boolean selectOnlySourceFolders) {
        super(parent, new CopiedWorkbenchLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof Package) {
                    element = ((Package) element).folder;
                }
                if (element instanceof SourceFolder) {
                    SourceFolder f = (SourceFolder) element;
                    return f.folder.getProjectRelativePath().toString(); // we want the complete path here...
                }
                return super.getText(element);
            }

            @Override
            public Image getImage(Object element) {
                if (element instanceof Package) {
                    element = ((Package) element).folder;
                }
                if (element instanceof SourceFolder) {
                    return PydevPlugin.getImageCache().get(UIConstants.SOURCE_FOLDER_ICON);
                }
                return super.getImage(element);
            }
        }, new PackageContentProvider(selectOnlySourceFolders));
        setAllowMultiple(false);

        this.selectOnlySourceFolders = selectOnlySourceFolders;

        //let's set the validator
        this.setValidator(new ISelectionStatusValidator() {
            @Override
            public IStatus validate(Object selection[]) {
                if (selection.length == 1) {
                    if (selection[0] instanceof SourceFolder) {
                        SourceFolder folder = (SourceFolder) selection[0];
                        return new Status(IStatus.OK, PydevPlugin.getPluginID(), IStatus.OK, "Source Folder: "
                                + folder.folder.getProjectRelativePath().toString() + " selected", null);
                    }
                    if (!selectOnlySourceFolders) {
                        if (selection[0] instanceof Package) {
                            Package folder = (Package) selection[0];
                            return new Status(IStatus.OK, PydevPlugin.getPluginID(), IStatus.OK, "Package: "
                                    + folder.folder.getName() + " selected", null);
                        }
                    }
                }
                return new Status(IStatus.ERROR, PydevPlugin.getPluginID(), IStatus.ERROR, "No package selected", null);

            }
        });
        this.setInput(ResourcesPlugin.getWorkspace().getRoot());
    }

}

class PackageContentProvider implements ITreeContentProvider {

    private IWorkspaceRoot workspaceRoot;
    private boolean selectOnlySourceFolders;

    public PackageContentProvider(boolean selectOnlySourceFolders) {
        this.selectOnlySourceFolders = selectOnlySourceFolders;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        //workspace root
        if (parentElement instanceof IWorkspaceRoot) {
            this.workspaceRoot = (IWorkspaceRoot) parentElement;
            List<IProject> ret = new ArrayList<IProject>();

            IProject[] projects = workspaceRoot.getProjects();
            for (IProject project : projects) {
                PythonNature nature = PythonNature.getPythonNature(project);
                if (nature != null) {
                    ret.add(project);
                }
            }
            return ret.toArray(new IProject[0]);
        }

        //project
        if (parentElement instanceof IProject) {
            List<Object> ret = new ArrayList<Object>();
            IProject project = (IProject) parentElement;
            IPythonPathNature nature = PythonNature.getPythonPathNature(project);
            if (nature != null) {
                try {
                    String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath(true));
                    for (String str : srcPaths) {
                        IResource resource = this.workspaceRoot.findMember(new Path(str));
                        if (resource instanceof IFolder) {
                            IFolder folder = (IFolder) resource;
                            if (folder.exists()) {
                                if (folder != null) {
                                    ret.add(new SourceFolder(folder));
                                }
                            }
                        }
                        if (resource instanceof IProject) {
                            IProject folder = (IProject) resource;
                            if (folder.exists()) {
                                if (folder != null) {
                                    ret.add(new SourceFolder(folder));
                                }
                            }
                        }
                    }
                    return ret.toArray();
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
        }

        //source folder
        SourceFolder sourceFolder = null;
        if (parentElement instanceof SourceFolder) {
            sourceFolder = (SourceFolder) parentElement;
            parentElement = ((SourceFolder) parentElement).folder;
        }

        //package
        if (parentElement instanceof Package) {
            sourceFolder = ((Package) parentElement).sourceFolder;
            parentElement = ((Package) parentElement).folder;
        }

        if (parentElement instanceof IFolder) {
            IFolder f = (IFolder) parentElement;
            ArrayList<Package> ret = new ArrayList<Package>();
            try {
                IResource[] resources = f.members();
                for (IResource resource : resources) {
                    if (resource instanceof IFolder) {
                        ret.add(new Package((IFolder) resource, sourceFolder));
                    }
                }
            } catch (CoreException e) {
                Log.log(e);
            }
            return ret.toArray();
        }

        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof Package) {
            return ((Package) element).sourceFolder;
        }
        if (element instanceof IResource) {
            return ((IResource) element).getParent();
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (selectOnlySourceFolders) {
            if (element instanceof SourceFolder) {
                return false;
            }
        }
        return getChildren(element).length > 0;
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

}
