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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.python.pydev.core.log.Log;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.ProjectConfigError;
import org.python.pydev.navigator.elements.PythonFile;
import org.python.pydev.navigator.elements.PythonFolder;
import org.python.pydev.navigator.elements.PythonProjectSourceFolder;
import org.python.pydev.navigator.elements.PythonResource;
import org.python.pydev.navigator.elements.PythonSourceFolder;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.TreeNode;

/**
 * This is the Model provider for python elements.
 *
 * It intercepts the adds/removes and changes the original elements for elements
 * that actually reflect the python model (with source folder, etc).
 *
 *
 * Tests for package explorer:
 * 1. Start eclipse with a file deep in the structure and without having anything expanded in the tree, make a 'show in'
 *
 * @author Fabio
 */
public final class PythonModelProvider extends PythonBaseModelProvider implements IPipelinedTreeContentProvider {

    /* (non-Javadoc)
     * @see org.python.pydev.navigator.PythonBaseModelProvider#getChildren(java.lang.Object)
     */
    @Override
    public Object[] getChildren(Object parentElement) {
        Object[] ret = super.getChildren(parentElement);
        if (parentElement instanceof PythonProjectSourceFolder) {
            PythonProjectSourceFolder projectSourceFolder = (PythonProjectSourceFolder) parentElement;
            Set<Object> set = new HashSet<Object>();
            fillChildrenForProject(set, (IProject) projectSourceFolder.getActualObject(), projectSourceFolder);
            if (set.size() > 0) {
                Object[] newRet = new Object[ret.length + set.size()];
                System.arraycopy(ret, 0, newRet, 0, ret.length);
                int i = ret.length;
                for (Object o : set) {
                    newRet[i] = o;
                    i++;
                }
                ret = newRet;
            }
        }
        return ret;
    }

    /**
     * This method basically replaces all the elements for other resource elements
     * or for wrapped elements.
     *
     * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedChildren(java.lang.Object, java.util.Set)
     */
    @SuppressWarnings("unchecked")
    public void getPipelinedChildren(Object parent, Set currentElements) {
        if (DEBUG) {
            System.out.println("getPipelinedChildren for: " + parent);
        }

        if (parent instanceof IWrappedResource) {
            //Note: It seems that this NEVER happens (IWrappedResources only have getChildren called, not getPipelinedChildren)
            Object[] children = getChildren(parent);
            currentElements.clear();
            currentElements.addAll(Arrays.asList(children));
            if (DEBUG) {
                System.out.println("getPipelinedChildren RETURN: " + currentElements);
            }
            if (parent instanceof PythonProjectSourceFolder) {
                PythonProjectSourceFolder projectSourceFolder = (PythonProjectSourceFolder) parent;
                IProject project = (IProject) projectSourceFolder.getActualObject();
                fillChildrenForProject(currentElements, project, parent);
            }
            return;

        } else if (parent instanceof IWorkspaceRoot) {
            switch (topLevelChoice.getRootMode()) {
                case TopLevelProjectsOrWorkingSetChoice.WORKING_SETS:
                    currentElements.clear();
                    currentElements.addAll(getWorkingSetsCallback.call((IWorkspaceRoot) parent));
                    if (currentElements.size() == 0) {
                        currentElements.add(createErrorNoWorkingSetsDefined(parent));
                    }
                case TopLevelProjectsOrWorkingSetChoice.PROJECTS:
                    //Just go on...
            }

        } else if (parent instanceof IWorkingSet) {
            IWorkingSet workingSet = (IWorkingSet) parent;
            currentElements.clear();
            currentElements.addAll(Arrays.asList(workingSet.getElements()));
            if (currentElements.size() == 0) {
                currentElements.add(createErrorNoWorkingSetsDefined(workingSet));
            }

        } else if (parent instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) parent;
            currentElements.addAll(treeNode.getChildren());

        } else if (parent instanceof IProject) {
            IProject project = (IProject) parent;
            fillChildrenForProject(currentElements, project, getResourceInPythonModel(project));
        }

        PipelinedShapeModification modification = new PipelinedShapeModification(parent, currentElements);
        convertToPythonElementsAddOrRemove(modification, true);
        if (DEBUG) {
            System.out.println("getPipelinedChildren RETURN: " + modification.getChildren());
        }
    }

    @SuppressWarnings("unchecked")
    private void fillChildrenForProject(Set currentElements, IProject project, Object parent) {
        ProjectInfoForPackageExplorer projectInfo = ProjectInfoForPackageExplorer.getProjectInfo(project);
        if (projectInfo != null) {
            currentElements.addAll(projectInfo.configErrors);
            InterpreterInfoTreeNode<LabelAndImage> projectInfoTreeStructure = projectInfo.getProjectInfoTreeStructure(
                    project, parent);
            if (projectInfoTreeStructure != null) {
                currentElements.add(projectInfoTreeStructure);
            }
        }
    }

    /**
     * This method basically replaces all the elements for other resource elements
     * or for wrapped elements.
     *
     * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedElements(java.lang.Object, java.util.Set)
     */
    public void getPipelinedElements(Object input, Set currentElements) {
        if (DEBUG) {
            System.out.println("getPipelinedElements for: " + input);
        }
        getPipelinedChildren(input, currentElements);
    }

    /**
     * This method basically get the actual parent for the resource or the parent
     * for a wrapped element (which may be a resource or a wrapped resource).
     *
     * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#getPipelinedParent(java.lang.Object, java.lang.Object)
     */
    public Object getPipelinedParent(Object object, Object aSuggestedParent) {
        if (DEBUG) {
            System.out.println("getPipelinedParent for: " + object);
        }
        //Now, we got the parent for the resources correctly at this point, but there's one last thing we may need to
        //do: the actual parent may be a working set!
        Object p = this.topLevelChoice.getWorkingSetParentIfAvailable(object, getWorkingSetsCallback);
        if (p != null) {
            aSuggestedParent = p;

        } else if (object instanceof IWrappedResource) {
            IWrappedResource resource = (IWrappedResource) object;
            Object parentElement = resource.getParentElement();
            if (parentElement != null) {
                aSuggestedParent = parentElement;
            }

        } else if (object instanceof TreeNode<?>) {
            TreeNode<?> treeNode = (TreeNode<?>) object;
            return treeNode.getParent();

        } else if (object instanceof ProjectConfigError) {
            ProjectConfigError configError = (ProjectConfigError) object;
            return configError.getParent();

        }

        if (DEBUG) {
            System.out.println("getPipelinedParent RETURN: " + aSuggestedParent);
        }
        return aSuggestedParent;
    }

    /**
     * This method intercepts some addition to the tree and converts its elements to python
     * elements.
     *
     * @see org.eclipse.ui.navigator.IPipelinedTreeContentProvider#interceptAdd(org.eclipse.ui.navigator.PipelinedShapeModification)
     */
    public PipelinedShapeModification interceptAdd(PipelinedShapeModification addModification) {
        if (DEBUG) {
            System.out.println("interceptAdd");
        }
        convertToPythonElementsAddOrRemove(addModification, true);
        return addModification;
    }

    public boolean interceptRefresh(PipelinedViewerUpdate refreshSynchronization) {
        if (DEBUG) {
            System.out.println("interceptRefresh");
        }
        return convertToPythonElementsUpdateOrRefresh(refreshSynchronization.getRefreshTargets());
    }

    public PipelinedShapeModification interceptRemove(PipelinedShapeModification removeModification) {
        if (DEBUG) {
            System.out.println("interceptRemove");
        }
        convertToPythonElementsAddOrRemove(removeModification, false);
        return removeModification;
    }

    public boolean interceptUpdate(PipelinedViewerUpdate updateSynchronization) {
        if (DEBUG) {
            debug("Before interceptUpdate", updateSynchronization);
        }
        boolean ret = convertToPythonElementsUpdateOrRefresh(updateSynchronization.getRefreshTargets());
        if (DEBUG) {
            debug("After interceptUpdate", updateSynchronization);
        }
        return ret;
    }

    /**
     * Helper for debugging the things we have in an update
     */
    private void debug(String desc, PipelinedViewerUpdate updateSynchronization) {
        System.out.println("\nDesc:" + desc);
        System.out.println("Refresh targets:");
        for (Object o : updateSynchronization.getRefreshTargets()) {
            System.out.println(o);
        }
    }

    /**
     * Helper for debugging the things we have in a modification
     */
    private void debug(String desc, PipelinedShapeModification modification) {
        System.out.println("\nDesc:" + desc);
        Object parent = modification.getParent();
        System.out.println("Parent:" + parent);
        System.out.println("Children:");
        for (Object o : modification.getChildren()) {
            System.out.println(o);
        }
    }

    /**
     * This is the function that is responsible for restoring the paths in the tree.
     */
    public void restoreState(IMemento memento) {
        new PyPackageStateSaver(this, viewer, memento).restoreState();
    }

    /**
     * This is the function that is responsible for saving the paths in the tree.
     */
    public void saveState(IMemento memento) {
        new PyPackageStateSaver(this, viewer, memento).saveState();
    }

    /**
     * Converts the shape modification to use Python elements.
     *
     * @param modification: the shape modification to convert
     * @param isAdd: boolean indicating whether this convertion is happening in an add operation
     */
    private void convertToPythonElementsAddOrRemove(PipelinedShapeModification modification, boolean isAdd) {
        if (DEBUG) {
            debug("Before", modification);
        }
        Object parent = modification.getParent();
        if (parent instanceof IContainer) {
            IContainer parentContainer = (IContainer) parent;
            Object pythonParent = getResourceInPythonModel(parentContainer, true);

            if (pythonParent instanceof IWrappedResource) {
                IWrappedResource parentResource = (IWrappedResource) pythonParent;
                modification.setParent(parentResource);
                wrapChildren(parentResource, parentResource.getSourceFolder(), modification.getChildren(), isAdd);

            } else if (pythonParent == null) {

                Object parentInWrap = parentContainer;
                PythonSourceFolder sourceFolderInWrap = null;

                //this may happen when a source folder is added or some element that still doesn't have it's parent in the model...
                //so, we have to get the parent's parent until we actually 'know' that it is not in the model (or until we run
                //out of parents to try)
                //the case in which we reproduce this is Test 1 (described in the class)
                FastStack<Object> found = new FastStack<Object>(20);
                while (true) {

                    //add the current to the found
                    if (parentContainer == null) {
                        break;
                    }

                    found.push(parentContainer);
                    if (parentContainer instanceof IProject) {
                        //we got to the project without finding any part of a python model already there, so, let's see
                        //if any of the parts was actually a source folder (that was still not added)
                        tryCreateModelFromProject((IProject) parentContainer, found);
                        //and now, if it was created, try to convert it to the python model (without any further add)
                        convertToPythonElementsUpdateOrRefresh(modification.getChildren());
                        return;
                    }

                    Object p = getResourceInPythonModel(parentContainer, true);

                    if (p instanceof IWrappedResource) {
                        IWrappedResource wrappedResource = (IWrappedResource) p;
                        sourceFolderInWrap = wrappedResource.getSourceFolder();

                        while (found.size() > 0) {
                            Object f = found.pop();
                            if (f instanceof IResource) {
                                //no need to create it if it's already in the model!
                                Object child = sourceFolderInWrap.getChild((IResource) f);
                                if (child != null && child instanceof IWrappedResource) {
                                    wrappedResource = (IWrappedResource) child;
                                    continue;
                                }
                            }
                            //creating is enough to add it to the model
                            if (f instanceof IFile) {
                                wrappedResource = new PythonFile(wrappedResource, (IFile) f, sourceFolderInWrap);
                            } else if (f instanceof IFolder) {
                                wrappedResource = new PythonFolder(wrappedResource, (IFolder) f, sourceFolderInWrap);
                            }
                        }
                        parentInWrap = wrappedResource;
                        break;
                    }

                    parentContainer = parentContainer.getParent();
                }

                wrapChildren(parentInWrap, sourceFolderInWrap, modification.getChildren(), isAdd);
            }

        } else if (parent == null) {
            wrapChildren(null, null, modification.getChildren(), isAdd);
        }

        if (DEBUG) {
            debug("After", modification);
        }
    }

    /**
     * Given a Path from the 1st child of the project, will try to create that path in the python model.
     * @param project the project
     * @param found a stack so that the last element added is the leaf of the path we want to discover
     */
    private void tryCreateModelFromProject(IProject project, FastStack<Object> found) {
        PythonNature nature = PythonNature.getPythonNature(project);
        if (nature == null) {
            return;//if the python nature is not available, we won't have any python elements here
        }
        Set<String> sourcePathSet = new HashSet<String>();
        try {
            sourcePathSet = nature.getPythonPathNature().getProjectSourcePathSet(true);
        } catch (CoreException e) {
            Log.log(e);
        }

        Object currentParent = project;
        PythonSourceFolder pythonSourceFolder = null;
        for (Iterator<Object> it = found.topDownIterator(); it.hasNext();) {
            Object child = it.next();
            if (child instanceof IFolder || child instanceof IProject) {
                if (pythonSourceFolder == null) {
                    pythonSourceFolder = tryWrapSourceFolder(currentParent, (IContainer) child, sourcePathSet);

                    if (pythonSourceFolder != null) {
                        currentParent = pythonSourceFolder;

                    } else if (child instanceof IContainer) {
                        currentParent = child;

                    }

                    //just go on (if we found the source folder or not, because if we found, that's ok, and if
                    //we didn't, then the children will not be in the python model anyway)
                    continue;
                }
            }

            if (pythonSourceFolder != null) {
                IWrappedResource r = doWrap(currentParent, pythonSourceFolder, child);
                if (r != null) {
                    child = r;
                }
            }
            currentParent = child;
        }
    }

    /**
     * Actually wraps some resource into a wrapped resource.
     *
     * @param parent this is the parent
     *        it may be null -- in the case of a remove
     *        it may be a wrapped resource (if it is in the python model)
     *        it may be a resource (if it is a source folder)
     *
     *
     * @param pythonSourceFolder this is the python source folder for the resource (it may be null if the resource itself is a source folder
     *        or if it is actually a resource that has already been removed)
     * @param currentChildren those are the children that should be wrapped
     * @param isAdd whether this is an add operation or not
     * @return
     */
    @SuppressWarnings("unchecked")
    protected boolean wrapChildren(Object parent, PythonSourceFolder pythonSourceFolder, Set currentChildren,
            boolean isAdd) {
        LinkedHashSet convertedChildren = new LinkedHashSet();

        for (Iterator childrenItr = currentChildren.iterator(); childrenItr.hasNext();) {
            Object child = childrenItr.next();

            if (child == null) {
                //only case when a child is removed and another one is not added (null)
                childrenItr.remove();
                continue;
            }

            //yeap, it may be an object that's not an actual resource (created by some other plugin... just continue)
            if (!(child instanceof IResource)) {
                continue;
            }
            Object existing = getResourceInPythonModel((IResource) child, true);

            if (existing == null) {
                if (isAdd) {
                    //add
                    IWrappedResource w = doWrap(parent, pythonSourceFolder, child);
                    if (w != null) { //if it is null, it is not below a python source folder
                        childrenItr.remove();
                        convertedChildren.add(w);
                    }
                } else {
                    continue; //it has already been removed
                }

            } else { //existing != null
                childrenItr.remove();
                convertedChildren.add(existing);
                if (!isAdd) {
                    //also remove it from the model
                    IWrappedResource wrapped = (IWrappedResource) existing;
                    wrapped.getSourceFolder().removeChild((IResource) child);
                }
            }
        }

        //if we did have some wrapping... go on and add them to the out list (and return true)
        if (!convertedChildren.isEmpty()) {
            currentChildren.addAll(convertedChildren);
            return true;
        }

        //nothing happened, so, just say it
        return false;
    }

    /**
     * This method tries to wrap a given resource as a wrapped resource (if possible)
     *
     * @param parent the parent of the wrapped resource
     * @param pythonSourceFolder the source folder that contains this resource
     * @param child the object that should be wrapped
     * @return the object as an object from the python model
     */
    protected IWrappedResource doWrap(Object parent, PythonSourceFolder pythonSourceFolder, Object child) {
        if (child instanceof IProject) {
            //ok, let's see if the child is a source folder (as the default project can be the actual source folder)
            if (pythonSourceFolder == null && parent != null) {
                PythonSourceFolder f = doWrapPossibleSourceFolder(parent, (IProject) child);
                if (f != null) {
                    return f;
                }
            }

        } else if (child instanceof IFolder) {
            IFolder folder = (IFolder) child;

            //it may be a PythonSourceFolder
            if (pythonSourceFolder == null && parent != null) {
                PythonSourceFolder f = doWrapPossibleSourceFolder(parent, folder);
                if (f != null) {
                    return f;
                }
            }
            if (pythonSourceFolder != null) {
                return new PythonFolder((IWrappedResource) parent, folder, pythonSourceFolder);
            }

        } else if (child instanceof IFile) {
            if (pythonSourceFolder != null) {
                //if the python source folder is null, that means that this is a file that is not actually below a source folder -- so, don't wrap it
                return new PythonFile((IWrappedResource) parent, (IFile) child, pythonSourceFolder);
            }

        } else if (child instanceof IResource) {
            if (pythonSourceFolder != null) {
                return new PythonResource((IWrappedResource) parent, (IResource) child, pythonSourceFolder);
            }

        } else {
            throw new RuntimeException("Unexpected class:" + child.getClass());
        }

        return null;
    }

    /**
     * Try to wrap a folder or project as a source folder...
     */
    private PythonSourceFolder doWrapPossibleSourceFolder(Object parent, IContainer container) {
        try {
            IProject project;
            if (!(container instanceof IProject)) {
                project = ((IContainer) parent).getProject();
            } else {
                project = (IProject) container;
            }
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature != null) {
                //check for source folder
                Set<String> sourcePathSet = nature.getPythonPathNature().getProjectSourcePathSet(true);
                PythonSourceFolder newSourceFolder = tryWrapSourceFolder(parent, container, sourcePathSet);
                if (newSourceFolder != null) {
                    return newSourceFolder;
                }
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * This method checks if the given folder can be wrapped as a source-folder, and if that's possible, creates and returns
     * it
     * @return a created source folder or null if it couldn't be created.
     */
    private PythonSourceFolder tryWrapSourceFolder(Object parent, IContainer container, Set<String> sourcePathSet) {
        IPath fullPath = container.getFullPath();
        if (sourcePathSet.contains(fullPath.toString())) {
            PythonSourceFolder sourceFolder;
            if (container instanceof IFolder) {
                sourceFolder = new PythonSourceFolder(parent, (IFolder) container);
            } else if (container instanceof IProject) {
                sourceFolder = new PythonProjectSourceFolder(parent, (IProject) container);
            } else {
                return null; //some other container we don't know how to treat!
            }
            //System.out.println("Created source folder: "+ret[i]+" - "+folder.getProject()+" - "+folder.getProjectRelativePath());
            Set<PythonSourceFolder> sourceFolders = getProjectSourceFolders(container.getProject());
            sourceFolders.add(sourceFolder);
            return sourceFolder;
        }
        return null;
    }

    /**
     * Converts elements to the python model -- but only creates it if it's parent is found in the python model
     */
    @SuppressWarnings("unchecked")
    protected boolean convertToPythonElementsUpdateOrRefresh(Set currentChildren) {
        LinkedHashSet convertedChildren = new LinkedHashSet();
        for (Iterator childrenItr = currentChildren.iterator(); childrenItr.hasNext();) {
            Object child = childrenItr.next();

            if (child == null) {
                //only case when a child is removed and another one is not added (null)
                childrenItr.remove();
                continue;
            }

            if (child instanceof IResource && !(child instanceof IWrappedResource)) {
                IResource res = (IResource) child;

                Object resourceInPythonModel = getResourceInPythonModel(res, true);
                if (resourceInPythonModel != null) {
                    //if it is in the python model, just go on
                    childrenItr.remove();
                    convertedChildren.add(resourceInPythonModel);

                } else {
                    //now, if it's not but its parent is, go on and create it
                    IContainer p = res.getParent();
                    if (p == null) {
                        continue;
                    }

                    Object pythonParent = getResourceInPythonModel(p, true);
                    if (pythonParent instanceof IWrappedResource) {
                        IWrappedResource parent = (IWrappedResource) pythonParent;

                        if (res instanceof IProject) {
                            throw new RuntimeException("A project's parent should never be an IWrappedResource!");

                        } else if (res instanceof IFolder) {
                            childrenItr.remove();
                            convertedChildren.add(new PythonFolder(parent, (IFolder) res, parent.getSourceFolder()));

                        } else if (res instanceof IFile) {
                            childrenItr.remove();
                            convertedChildren.add(new PythonFile(parent, (IFile) res, parent.getSourceFolder()));

                        } else if (child instanceof IResource) {
                            childrenItr.remove();
                            convertedChildren.add(new PythonResource(parent, (IResource) child, parent
                                    .getSourceFolder()));
                        }

                    } else if (res instanceof IFolder) {
                        //ok, still not in the model... could it be a PythonSourceFolder
                        IFolder folder = (IFolder) res;
                        IProject project = folder.getProject();
                        if (project == null) {
                            continue;
                        }
                        PythonNature nature = PythonNature.getPythonNature(project);
                        if (nature == null) {
                            continue;
                        }
                        Set<String> sourcePathSet = new HashSet<String>();
                        try {
                            sourcePathSet = nature.getPythonPathNature().getProjectSourcePathSet(true);
                        } catch (CoreException e) {
                            Log.log(e);
                        }
                        PythonSourceFolder wrapped = tryWrapSourceFolder(p, folder, sourcePathSet);
                        if (wrapped != null) {
                            childrenItr.remove();
                            convertedChildren.add(wrapped);
                        }
                    }
                }

            }
        }
        if (!convertedChildren.isEmpty()) {
            currentChildren.addAll(convertedChildren);
            return true;
        }
        return false;

    }
}
