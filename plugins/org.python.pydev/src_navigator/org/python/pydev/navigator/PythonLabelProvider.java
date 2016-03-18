/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 7, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.ProjectConfigError;
import org.python.pydev.navigator.elements.PythonFolder;
import org.python.pydev.navigator.elements.PythonNode;
import org.python.pydev.navigator.elements.PythonProjectSourceFolder;
import org.python.pydev.navigator.elements.PythonSourceFolder;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.PyTitlePreferencesPage;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.SharedUiPlugin;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

/**
 * Provides the labels for the pydev package explorer.
 * 
 * @author Fabio
 */
public class PythonLabelProvider implements ILabelProvider {

    private WorkbenchLabelProvider provider;

    private volatile Image projectWithError = null;

    private Object lock = new Object();

    public PythonLabelProvider() {
        provider = new WorkbenchLabelProvider();
    }

    /**
     * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
     */
    @Override
    public Image getImage(Object element) {
        if (element instanceof PythonProjectSourceFolder) {
            return SharedUiPlugin.getImageCache().get(UIConstants.PROJECT_SOURCE_FOLDER_ICON);
        }
        if (element instanceof PythonSourceFolder) {
            return SharedUiPlugin.getImageCache().get(UIConstants.SOURCE_FOLDER_ICON);
        }
        if (element instanceof PythonFolder) {
            PythonFolder folder = (PythonFolder) element;
            IFolder actualObject = folder.getActualObject();
            if (actualObject != null) {
                final String[] validInitFiles = FileTypesPreferencesPage.getValidInitFiles();

                for (String init : validInitFiles) {
                    if (actualObject.getFile(init).exists()) {
                        if (checkParentsHaveInit(folder, validInitFiles)) {
                            return SharedUiPlugin.getImageCache().get(UIConstants.FOLDER_PACKAGE_ICON);
                        } else {
                            break;
                        }
                    }
                }
            }
            return provider.getImage(actualObject);
        }
        if (element instanceof PythonNode) {
            PythonNode node = (PythonNode) element;
            return node.entry.getImage();
        }
        if (element instanceof IWrappedResource) {
            IWrappedResource resource = (IWrappedResource) element;
            Object actualObject = resource.getActualObject();
            if (actualObject instanceof IFile) {
                IFile iFile = (IFile) actualObject;
                final String name = iFile.getName();

                if (name.indexOf('.') == -1) {
                    try {
                        if (PythonPathHelper.markAsPyDevFileIfDetected(iFile)) {
                            if (FileTypesPreferencesPage.isCythonFile(name)) {
                                return SharedUiPlugin.getImageCache().get(UIConstants.CYTHON_FILE_ICON);
                            }
                            return SharedUiPlugin.getImageCache().get(UIConstants.PY_FILE_ICON);
                        }
                    } catch (Exception e) {
                        //Ignore
                    }
                }
                if (FileTypesPreferencesPage.isCythonFile(name)) {
                    return SharedUiPlugin.getImageCache().get(UIConstants.CYTHON_FILE_ICON);
                }

                if (name.startsWith("__init__.") && PythonPathHelper.isValidSourceFile(name)) {
                    return PyTitlePreferencesPage.getInitIcon();
                } else {
                    IProject project = iFile.getProject();
                    try {
                        if (project.hasNature(PythonNature.DJANGO_NATURE_ID)) {
                            String djangoModulesHandling = PyTitlePreferencesPage.getDjangoModulesHandling();
                            if (djangoModulesHandling == PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_SHOW_PARENT_AND_DECORATE
                                    || djangoModulesHandling == PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_DECORATE) {

                                if (PyTitlePreferencesPage.isDjangoModuleToDecorate(name)) {
                                    return PyTitlePreferencesPage.getDjangoModuleIcon(name);
                                }
                            }
                        }
                    } catch (CoreException e) {
                        Log.log(e);
                    }
                }
            }
            return provider.getImage(actualObject);
        }
        if (element instanceof ProjectConfigError) {
            return SharedUiPlugin.getImageCache().get(UIConstants.ERROR);
        }
        if (element instanceof TreeNode<?>) {
            TreeNode<?> treeNode = (TreeNode<?>) element;
            LabelAndImage data = (LabelAndImage) treeNode.getData();
            return data.image;
        }
        if (element instanceof IFile) {
            IFile iFile = (IFile) element;
            String name = iFile.getName();
            if (FileTypesPreferencesPage.isCythonFile(name)) {
                return SharedUiPlugin.getImageCache().get(UIConstants.CYTHON_FILE_ICON);
            }

        }
        if (element instanceof IProject) {
            IProject project = (IProject) element;
            if (!project.isOpen()) {
                return null;
            }
            IMarker[] markers;
            try {
                markers = project.findMarkers(PythonBaseModelProvider.PYDEV_PACKAGE_EXPORER_PROBLEM_MARKER, true, 0);
            } catch (CoreException e1) {
                Log.log(e1);
                return null;
            }
            if (markers == null || markers.length == 0) {
                return null;
            }

            //We have errors: make them explicit.
            if (projectWithError == null) {
                synchronized (lock) {
                    //we must recheck again (if 2 got here and 1 got the lock while the other was waiting, when
                    //the other enters the lock, it does not need to recalculated).
                    if (projectWithError == null) {
                        //Note on double-checked locking idiom: http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html.
                        //(would not work as expected on java 1.4)
                        Image image = provider.getImage(element);
                        try {
                            DecorationOverlayIcon decorationOverlayIcon = new DecorationOverlayIcon(image,
                                    SharedUiPlugin
                                            .getImageCache().getDescriptor(UIConstants.ERROR_SMALL),
                                    IDecoration.BOTTOM_LEFT);
                            projectWithError = decorationOverlayIcon.createImage();
                        } catch (Exception e) {
                            Log.log("Unable to create error decoration for project icon.", e);
                            projectWithError = image;
                        }
                    }
                }
            }

            return projectWithError;
        }

        if (element instanceof IWorkingSet) {
            return SharedUiPlugin.getImageCache().get(UIConstants.WORKING_SET);
        }
        return null;
    }

    /**
     * Checks if all the parents have the needed __init__ files (needed to consider some folder an actual python module)
     * 
     * @param pythonFolder the python folder whose hierarchy should be checked (note that the folder itself must have already
     * been checked at this point)
     * @param validInitFiles the valid names for the __init__ files (because we can have more than one matching extension)
     * 
     * @return true if all the parents have the __init__ files and false otherwise.
     */
    private final boolean checkParentsHaveInit(final PythonFolder pythonFolder, final String[] validInitFiles) {
        IWrappedResource parentElement = pythonFolder.getParentElement();
        while (parentElement != null) {
            if (parentElement instanceof PythonSourceFolder) {
                //gotten to the source folder: this one doesn't need to have an __init__.py
                return true;
            }

            Object actualObject = parentElement.getActualObject();
            if (actualObject instanceof IFolder) {
                IFolder folder = (IFolder) actualObject;
                boolean foundInit = false;
                for (String init : validInitFiles) {
                    final IFile file = folder.getFile(init);
                    if (file.exists()) {
                        foundInit = true;
                        break;
                    }
                }
                if (!foundInit) {
                    return false;
                }
            }

            Object tempParent = parentElement.getParentElement();
            if (!(tempParent instanceof IWrappedResource)) {
                break;
            }
            parentElement = (IWrappedResource) tempParent;

        }
        return true;
    }

    /**
     * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText(Object element) {
        if (element instanceof PythonNode) {
            PythonNode node = (PythonNode) element;
            return node.entry.toString();
        }

        if (element instanceof PythonSourceFolder) {
            PythonSourceFolder sourceFolder = (PythonSourceFolder) element;
            return provider.getText(sourceFolder.container);
        }

        if (element instanceof IWrappedResource) {
            IWrappedResource resource = (IWrappedResource) element;
            return provider.getText(resource.getActualObject());
        }
        if (element instanceof TreeNode<?>) {
            TreeNode<?> treeNode = (TreeNode<?>) element;
            LabelAndImage data = (LabelAndImage) treeNode.getData();
            return data.label;
        }
        if (element instanceof ProjectConfigError) {
            return ((ProjectConfigError) element).getLabel();
        }

        return provider.getText(element);
    }

    /**
     * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
     */
    @Override
    public void addListener(ILabelProviderListener listener) {
        provider.addListener(listener);
    }

    @Override
    public void dispose() {
        provider.dispose();
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return provider.isLabelProperty(element, property);
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        provider.removeListener(listener);
    }

}
