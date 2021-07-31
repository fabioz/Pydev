/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.navigator.elements.ProjectConfigError;
import org.python.pydev.navigator.elements.PythonSourceFolder;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.SharedUiPlugin;

/**
 * This class contains information about the project (info we need to show in the tree).
 */
public class ProjectInfoForPackageExplorer {

    /**
     * These are the source folders that can be found in this file provider. The way we
     * see things in this provider, the python model starts only after some source folder
     * is found.
     */
    private static final Map<IProject, ProjectInfoForPackageExplorer> projectToSourceFolders = new HashMap<IProject, ProjectInfoForPackageExplorer>();
    private static final Object lockProjectToSourceFolders = new Object();

    /**
     * @return the information on a project. Can create it if it's not available.
     */
    public static ProjectInfoForPackageExplorer getProjectInfo(final IProject project) {
        if (project == null) {
            return null;
        }
        synchronized (lockProjectToSourceFolders) {
            ProjectInfoForPackageExplorer projectInfo = projectToSourceFolders.get(project);
            if (projectInfo == null) {
                if (!project.isOpen()) {
                    return null;
                }
                //No project info: create it
                projectInfo = projectToSourceFolders.get(project);
                if (projectInfo == null) {
                    projectInfo = new ProjectInfoForPackageExplorer(project);
                    projectToSourceFolders.put(project, projectInfo);
                }
            } else {
                if (!project.isOpen()) {
                    projectToSourceFolders.remove(project);
                    projectInfo = null;
                }
            }
            return projectInfo;
        }
    }

    /**
     * Note that the source folders are added/removed lazily (not when the info is recreated)
     */
    public final Set<PythonSourceFolder> sourceFolders = new HashSet<PythonSourceFolder>();

    /**
     * Whenever the info is recreated this is also recreated.
     */
    public final List<ProjectConfigError> configErrors = new ArrayList<ProjectConfigError>();

    /**
     * The interpreter info available (may be null)
     */
    public IInterpreterInfo interpreterInfo;

    /**
     * Cache for the interpreter info tree root (so, if asked more than once this one will be reused).
     */
    private InterpreterInfoTreeNodeRoot<LabelAndImage> interpreterInfoTreeRoot;

    /**
     * Creates the info for the passed project.
     */
    private ProjectInfoForPackageExplorer(IProject project) {
        this.recreateInfo(project);
    }

    /**
     * Recreates the information about the project.
     */
    public void recreateInfo(IProject project) {
        interpreterInfoTreeRoot = null;
        configErrors.clear();
        Tuple<List<ProjectConfigError>, IInterpreterInfo> configErrorsAndInfo = getConfigErrorsAndInfo(project);
        configErrors.addAll(configErrorsAndInfo.o1);
        this.interpreterInfo = configErrorsAndInfo.o2;
    }

    public synchronized InterpreterInfoTreeNodeRoot<LabelAndImage> getProjectInfoTreeStructure(IProject project,
            Object parent) {
        if (parent == null || this.interpreterInfo == null) {
            return null;
        }

        PythonNature nature = PythonNature.getPythonNature(project);
        if (interpreterInfoTreeRoot != null) {
            if (interpreterInfoTreeRoot.getParent().equals(parent)
                    && interpreterInfoTreeRoot.interpreterInfo.equals(interpreterInfo)) {
                return interpreterInfoTreeRoot;
            }
        }
        interpreterInfoTreeRoot = null;

        try {
            IImageCache imageCache = SharedUiPlugin.getImageCache();

            //The root will create its children automatically.
            String nameForUI = interpreterInfo.getNameForUI();
            nameForUI = StringUtils.shorten(nameForUI, 40);
            interpreterInfoTreeRoot = new InterpreterInfoTreeNodeRoot<LabelAndImage>(interpreterInfo, nature, parent,
                    new LabelAndImage(nameForUI, imageCache.get(UIConstants.PY_INTERPRETER_ICON)));

        } catch (Throwable e) {
            Log.log(e);
            return null;
        }

        return interpreterInfoTreeRoot;
    }

    /**
     * Do the update of the markers in a separate job so that we don't do that in the ui-thread.
     *
     * See: #PyDev-88: Eclipse freeze on project import and project creation (presumable cause: virtualenvs as custom interpreters)
     */
    private static final class UpdatePydevPackageExplorerProblemMarkers extends Job {
        private IProject fProject;
        private ProjectConfigError[] fProjectConfigErrors;
        private final Object lockJob = new Object();

        private UpdatePydevPackageExplorerProblemMarkers(String name) {
            super(name);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            final IProject project;
            final ProjectConfigError[] projectConfigErrors;
            synchronized (lockJob) {
                project = this.fProject;
                projectConfigErrors = this.fProjectConfigErrors;
                this.fProject = null;
                this.fProjectConfigErrors = null;

                //In a racing condition it's possible that it was scheduled again when the projectConfigErrors was already
                //set to null.
                if (projectConfigErrors == null) {
                    return Status.OK_STATUS;
                }
            }

            ArrayList lst = new ArrayList(projectConfigErrors.length);
            for (ProjectConfigError error : projectConfigErrors) {
                try {
                    Map attributes = new HashMap();
                    attributes.put(IMarker.MESSAGE, error.getLabel());
                    attributes.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
                    lst.add(attributes);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            PyMarkerUtils.replaceMarkers((Map<String, Object>[]) lst.toArray(new Map[lst.size()]), project,
                    PythonBaseModelProvider.PYDEV_PACKAGE_EXPORER_PROBLEM_MARKER, true, monitor);

            synchronized (ProjectInfoForPackageExplorer.lock) {
                //Only need to lock at the outer lock as it's the same one needed for the job creation.
                if (this.fProject == null) {
                    //if it's not null, it means it was rescheduled!
                    ProjectInfoForPackageExplorer.projectToJob.remove(project);
                }
            }
            return Status.OK_STATUS;
        }

        public void setInfo(IProject project, ProjectConfigError[] projectConfigErrors) {
            synchronized (lockJob) {
                this.fProject = project;
                this.fProjectConfigErrors = projectConfigErrors;
            }
        }
    }

    private static final Map<IProject, UpdatePydevPackageExplorerProblemMarkers> projectToJob = new HashMap<IProject, UpdatePydevPackageExplorerProblemMarkers>();
    private static final Object lock = new Object();

    /**
     * Never returns null.
     *
     * This method should only be called through recreateInfo.
     */
    private Tuple<List<ProjectConfigError>, IInterpreterInfo> getConfigErrorsAndInfo(IProject project) {
        if (project == null || !project.isOpen()) {
            return new Tuple<List<ProjectConfigError>, IInterpreterInfo>(new ArrayList<ProjectConfigError>(), null);
        }
        PythonNature nature = PythonNature.getPythonNature(project);
        if (nature == null) {
            return new Tuple<List<ProjectConfigError>, IInterpreterInfo>(new ArrayList<ProjectConfigError>(), null);
        }

        //If the info is not readily available, we try to get some more times... after that, if still not available,
        //we just return as if it's all OK.
        Tuple<List<ProjectConfigError>, IInterpreterInfo> configErrorsAndInfo = null;
        boolean goodToGo = false;
        for (int i = 0; i < 10 && !goodToGo; i++) {
            try {
                configErrorsAndInfo = getConfigErrorsAndInfo(nature, project);
                goodToGo = true;
            } catch (PythonNatureWithoutProjectException e1) {
                goodToGo = false;
                synchronized (this) {
                    try {
                        wait(100);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        if (configErrorsAndInfo == null) {
            return new Tuple<List<ProjectConfigError>, IInterpreterInfo>(new ArrayList<ProjectConfigError>(), null);
        }

        if (nature != null) {
            synchronized (lock) {
                UpdatePydevPackageExplorerProblemMarkers job = projectToJob
                        .get(project);
                if (job == null) {
                    job = new UpdatePydevPackageExplorerProblemMarkers(
                            "Update pydev package explorer markers for: " + project);
                    projectToJob.put(project, job);
                }
                job.setInfo(project,
                        configErrorsAndInfo.o1.toArray(new ProjectConfigError[configErrorsAndInfo.o1.size()]));
                job.schedule();
            }
        }

        return configErrorsAndInfo;
    }

    /**
     * @return a list of configuration errors and the interpreter info for the project (the interpreter info can be null)
     * @throws PythonNatureWithoutProjectException
     */
    public Tuple<List<ProjectConfigError>, IInterpreterInfo> getConfigErrorsAndInfo(IPythonNature nature,
            final IProject relatedToProject)
            throws PythonNatureWithoutProjectException {
        if (PythonNature.IN_TESTS) {
            return new Tuple<List<ProjectConfigError>, IInterpreterInfo>(new ArrayList<ProjectConfigError>(), null);
        }
        ArrayList<ProjectConfigError> lst = new ArrayList<ProjectConfigError>();
        if (nature.getProject() == null) {
            lst.add(new ProjectConfigError(relatedToProject, "The configured nature has no associated project."));
        }
        IInterpreterInfo info = null;
        try {
            info = nature.getProjectInterpreter();

            String executableOrJar = info.getExecutableOrJar();
            //Ok, if the user did a quick config, it's possible that the final executable is simply 'python', so,
            //in this case, don't check if it actually exists (as it's found on the PATH).
            //Note: this happened after we let the user keep the original name instead of getting it from the
            //interpreterInfo.py output.
            if (executableOrJar.contains("/") || executableOrJar.contains("\\")) {
                if (!FileUtils.enhancedIsFile(new File(executableOrJar))) {
                    lst.add(new ProjectConfigError(relatedToProject,
                            "The interpreter configured does not exist in the filesystem: " + executableOrJar));
                }
            }

            List<String> projectSourcePathSet = new ArrayList<String>(nature.getPythonPathNature()
                    .getProjectSourcePathSet(true));
            Collections.sort(projectSourcePathSet);
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

            for (String path : projectSourcePathSet) {
                if (path.trim().length() > 0) {
                    IPath p = new Path(path);
                    IResource resource = root.findMember(p);
                    if (resource == null) {
                        relatedToProject.refreshLocal(p.segmentCount(), null);
                        resource = root.findMember(p); //2nd attempt (after refresh)
                    }
                    if (resource == null || !resource.exists()) {
                        lst.add(new ProjectConfigError(relatedToProject, "Source folder: " + path + " not found"));
                    }
                }
            }

            List<String> externalPaths = nature.getPythonPathNature().getProjectExternalSourcePathAsList(true);
            Collections.sort(externalPaths);
            for (String path : externalPaths) {
                if (!new File(path).exists()) {
                    lst.add(new ProjectConfigError(relatedToProject, "Invalid external source folder specified: "
                            + path));
                }
            }

            Tuple<String, String> versionAndError = nature.getVersionAndError(true);
            if (versionAndError.o2 != null) {
                lst.add(new ProjectConfigError(relatedToProject, org.python.pydev.shared_core.string.StringUtils
                        .replaceNewLines(versionAndError.o2, " ")));
            }

        } catch (MisconfigurationException e) {
            lst.add(new ProjectConfigError(relatedToProject, org.python.pydev.shared_core.string.StringUtils
                    .replaceNewLines(e.getMessage(), " ")));

        } catch (Throwable e) {
            lst.add(new ProjectConfigError(relatedToProject, org.python.pydev.shared_core.string.StringUtils
                    .replaceNewLines(
                            "Unexpected error:" + e.getMessage(), " ")));
        }
        return new Tuple<List<ProjectConfigError>, IInterpreterInfo>(lst, info);
    }

}