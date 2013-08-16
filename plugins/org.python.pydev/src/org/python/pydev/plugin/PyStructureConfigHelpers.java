/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;

public class PyStructureConfigHelpers {

    /**
     * Creates a project resource handle for the current project name field value.
     * <p>
     * This method does not create the project resource; this is the responsibility
     * of <code>IProject::create</code> invoked by the new project resource wizard.
     * </p>
     *
     * @return the new project resource handle
     */
    public static IProject getProjectHandle(String projectName) {
        return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    }

    /**
     * @see #createPydevProject(IProjectDescription, IProject, IProgressMonitor, String, String, ICallback, ICallback, ICallback)
     */
    public static void createPydevProject(IProjectDescription description, IProject projectHandle,
            IProgressMonitor monitor, String projectType, String projectInterpreter,
            ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback,
            ICallback<List<String>, IProject> getExternalSourceFolderHandlesCallback,
            ICallback<List<IPath>, IProject> getExistingSourceFolderHandlesCallback)
            throws OperationCanceledException, CoreException {
        createPydevProject(description, projectHandle, monitor, projectType, projectInterpreter,
                getSourceFolderHandlesCallback, getExternalSourceFolderHandlesCallback,
                getExistingSourceFolderHandlesCallback, null);
    }

    /**
     * Creates a project resource given the project handle and description.
     * 
     * @param description the project description to create a project resource for
     * 
     * @param projectHandle the project handle to create a project resource for
     * 
     * @param monitor the progress monitor to show visual progress with
     * 
     * @param projectType one of the PYTHON_VERSION_XXX or JYTHON_VERSION_XXX constants from {@link IPythonNature}
     * 
     * @param projectInterpreter This is the interpreter to be added. It's one of the interpreters available from 
     * IInterpreterManager.getInterpreters.
     * 
     * 
     * @param getSourceFolderHandlesCallback This is a callback that's called with the project and it should 
     * return a list of handles with the folders that should be created and added to the project as source folders.
     * (if null, no source folders should be created)
     * 
     * E.g.: To create a 'src' source folder, the callback should be:
     * 
     *      ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback = new ICallback<List<IContainer>, IProject>(){
     *      
     *           public List<IContainer> call(IProject projectHandle) {
     *               IContainer folder = projectHandle.getFolder("src");
     *               List<IContainer> ret = new ArrayList<IContainer>();
     *               ret.add(folder);
     *               return ret;
     *           }
     *       };
     *
     *
     * @param getExternalSourceFolderHandlesCallback Same as the getSourceFolderHandlesCallback, but returns a list of
     * Strings to the actual paths in the filesystem that should be added as external source folders.
     * (if null, no external source folders should be created)
     * 
     * @param getExistingSourceFolderHandlesCallback Same as the getExternalSourceFolderHandlesCallback, but the external
     * folders listed will be treated as source folders rather than external libraries. No folders will be created.
     * (if null, no external source folders will be referenced)
     * 
     * @param getVariableSubstitutionCallback Same as getSourceFolderHandlesCallback, but returns a map of String, String,
     * so that the keys in the map can be used to resolve the source folders paths (project and external).
     * 
     * @exception CoreException if the operation fails
     * @exception OperationCanceledException if the operation is canceled
     */
    public static void createPydevProject(IProjectDescription description, IProject projectHandle,
            IProgressMonitor monitor, String projectType, String projectInterpreter,
            ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback,
            ICallback<List<String>, IProject> getExternalSourceFolderHandlesCallback,
            ICallback<List<IPath>, IProject> getExistingSourceFolderHandlesCallback,
            ICallback<Map<String, String>, IProject> getVariableSubstitutionCallback) throws CoreException,
            OperationCanceledException {

        try {
            monitor.beginTask("", 2000); //$NON-NLS-1$

            projectHandle.create(description, new SubProgressMonitor(monitor, 1000));

            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }

            projectHandle.open(IResource.BACKGROUND_REFRESH, new SubProgressMonitor(monitor, 1000));

            String projectPythonpath = null;
            //also, after creating the project, create a default source folder and add it to the pythonpath.
            if (getSourceFolderHandlesCallback != null) {
                List<IContainer> sourceFolders = getSourceFolderHandlesCallback.call(projectHandle);

                if (sourceFolders != null && sourceFolders.size() > 0) {
                    String projectHandleName = projectHandle.getFullPath().toString();
                    StringBuffer buf = new StringBuffer();
                    for (IContainer container : sourceFolders) {
                        if (container instanceof IFolder) {
                            IFolder iFolder = (IFolder) container;
                            iFolder.create(true, true, monitor);
                        } else if (container instanceof IProject) {
                            //continue (must be the passed project which was already created)
                        } else {
                            throw new RuntimeException("Expected container to be an IFolder or IProject. Was: "
                                    + container);
                        }
                        if (buf.length() > 0) {
                            buf.append("|");
                        }
                        String containerPath = convertToProjectRelativePath(projectHandleName, container.getFullPath()
                                .toString());
                        buf.append(containerPath);
                    }

                    projectPythonpath = buf.toString();
                }
            }

            //external sources will be treated as source folders rather than external libraries, to provide PyDev features.
            if (getExistingSourceFolderHandlesCallback != null) {
                List<IPath> existingPaths = getExistingSourceFolderHandlesCallback.call(projectHandle);

                if (existingPaths != null && existingPaths.size() > 0) {
                    String projectHandleName = projectHandle.getFullPath().toString();
                    StringBuffer buf = new StringBuffer();
                    for (IPath iPath : existingPaths) {
                        if (!iPath.toFile().exists()) {
                            Log.log("Unable to create link to " + iPath.toString());
                            continue;
                        }
                        String pathName = iPath.toString();
                        IFolder iFolder = projectHandle.getFolder(pathName.substring(pathName.lastIndexOf("/") + 1));
                        iFolder.createLink(iPath, IResource.BACKGROUND_REFRESH, monitor);

                        if (buf.length() > 0 || projectPythonpath != null) {
                            buf.append("|");
                        }
                        String containerPath = convertToProjectRelativePath(projectHandleName, iFolder.getFullPath()
                                .toString());
                        buf.append(containerPath);
                    }

                    projectPythonpath = projectPythonpath != null ? projectPythonpath.concat(buf.toString()) : buf
                            .toString();
                }
            }

            String externalProjectPythonpath = null;
            if (getExternalSourceFolderHandlesCallback != null) {
                List<String> externalPaths = getExternalSourceFolderHandlesCallback.call(projectHandle);
                if (externalPaths != null && externalPaths.size() > 0) {
                    StringBuffer buf = new StringBuffer();
                    for (String path : externalPaths) {
                        if (buf.length() > 0) {
                            buf.append("|");
                        }
                        buf.append(path);
                    }

                    externalProjectPythonpath = buf.toString();
                }
            }

            Map<String, String> variableSubstitution = null;
            if (getVariableSubstitutionCallback != null) {
                variableSubstitution = getVariableSubstitutionCallback.call(projectHandle);
            }

            //we should rebuild the path even if there's no source-folder (this way we will re-create the astmanager)
            PythonNature.addNature(projectHandle, null, projectType, projectPythonpath, externalProjectPythonpath,
                    projectInterpreter, variableSubstitution);
        } finally {
            monitor.done();
        }
    }

    public static String convertToProjectRelativePath(IProject project, IContainer container) {
        String projectHandleName = project.getFullPath().toString();
        return convertToProjectRelativePath(projectHandleName, container.getFullPath().toString());
    }

    public static String convertToProjectRelativePath(String projectHandleName, String containerPath) {
        if (containerPath.startsWith(projectHandleName)) {
            containerPath = containerPath.substring(projectHandleName.length());
            containerPath = "/${PROJECT_DIR_NAME}" + containerPath;
        }
        return containerPath;
    }

    /**
     * Creates a new project resource with the entered name.
     * 
     * @param projectName The name of the project
     * @param projectLocationPath the location for the project. If null, the default location (in the workspace)
     * will be used to create the project.
     * @param references The projects that should be referenced from the newly created project
     * 
     * @return the created project resource, or <code>null</code> if the project was not created
     * @throws CoreException 
     * @throws OperationCanceledException 
     */
    public static IProject createPydevProject(String projectName, IPath projectLocationPath, IProject[] references,

            IProgressMonitor monitor, String projectType, String projectInterpreter,
            ICallback<List<IContainer>, IProject> getSourceFolderHandlesCallback,
            ICallback<List<String>, IProject> getExternalSourceFolderHandlesCallback,
            ICallback<List<IPath>, IProject> getExistingSourceFolderHandlesCallback,
            ICallback<Map<String, String>, IProject> getVariableSubstitutionCallback)
            throws OperationCanceledException, CoreException {

        // get a project handle
        final IProject projectHandle = getProjectHandle(projectName);

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProjectDescription description = workspace.newProjectDescription(projectHandle.getName());
        description.setLocation(projectLocationPath);

        // update the referenced project if provided
        if (references != null && references.length > 0) {
            description.setReferencedProjects(references);
        }

        createPydevProject(description, projectHandle, monitor, projectType, projectInterpreter,
                getSourceFolderHandlesCallback, getExternalSourceFolderHandlesCallback,
                getExistingSourceFolderHandlesCallback, getVariableSubstitutionCallback);
        return projectHandle;
    }
}
