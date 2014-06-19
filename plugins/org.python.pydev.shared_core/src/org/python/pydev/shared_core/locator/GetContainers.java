package org.python.pydev.shared_core.locator;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.shared_core.log.Log;

public class GetContainers {
    /**
     * This method is a workaround for w.getRoot().getContainerForLocation(path); which does not work consistently because
     * it filters out files which should not be filtered (i.e.: if a project is not in the workspace but imported).
     * 
     * Also, it can fail to get resources in linked folders in the pythonpath.
     * 
     * @param project is optional (may be null): if given we'll search in it dependencies first.
     */
    public IContainer getContainerForLocation(IPath location, IProject project) {
        boolean stopOnFirst = true;
        IContainer[] filesForLocation = getContainersForLocation(location, project, stopOnFirst);
        if (filesForLocation != null && filesForLocation.length > 0) {
            return filesForLocation[0];
        }
        return null;
    }

    /**
     * This method is a workaround for w.getRoot().getContainersForLocation(path); which does not work consistently because
     * it filters out files which should not be filtered (i.e.: if a project is not in the workspace but imported).
     * 
     * Also, it can fail to get resources in linked folders in the pythonpath.
     * 
     * @param project is optional (may be null): if given we'll search in it dependencies first.
     */
    public IContainer[] getContainersForLocation(IPath location, IProject project, boolean stopOnFirst) {
        ArrayList<IContainer> lst = new ArrayList<>();
        HashSet<IProject> checked = new HashSet<>();
        IWorkspace w = ResourcesPlugin.getWorkspace();
        if (project != null) {
            checked.add(project);
            IContainer f = getContainerInProject(location, project);
            if (f != null) {
                if (stopOnFirst) {
                    return new IContainer[] { f };
                } else {
                    lst.add(f);
                }
            }
            try {
                IProject[] referencedProjects = project.getDescription().getReferencedProjects();
                for (int i = 0; i < referencedProjects.length; i++) {
                    IProject p = referencedProjects[i];
                    checked.add(p);
                    f = getContainerInProject(location, p);
                    if (f != null) {
                        if (stopOnFirst) {
                            return new IContainer[] { f };
                        } else {
                            lst.add(f);
                        }
                    }

                }
            } catch (CoreException e) {
                Log.log(e);
            }
        }

        IProject[] projects = w.getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
        for (int i = 0; i < projects.length; i++) {
            IProject p = projects[i];
            if (checked.contains(p)) {
                continue;
            }
            checked.add(p);
            IContainer f = getContainerInProject(location, p);
            if (f != null) {
                if (stopOnFirst) {
                    return new IContainer[] { f };
                } else {
                    lst.add(f);
                }
            }
        }
        return lst.toArray(new IContainer[lst.size()]);
    }

    /**
     * Gets an IContainer inside a container given a path in the filesystem (resolves the full path of the container and
     * checks if the location given is under it).
     */
    protected IContainer getContainerInContainer(IPath location, IContainer container) {
        IPath projectLocation = container.getLocation();
        if (projectLocation != null && projectLocation.isPrefixOf(location)) {
            int segmentsToRemove = projectLocation.segmentCount();
            IPath removeFirstSegments = location.removeFirstSegments(segmentsToRemove);
            if (removeFirstSegments.segmentCount() == 0) {
                return container; //I.e.: equal to container
            }
            IContainer file = container.getFolder(removeFirstSegments);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Tries to get a file from a project. Considers source folders (which could be linked) or resources directly beneath
     * the project.
     * @param location this is the path location to be gotten.
     * @param project this is the project we're searching.
     * @return the file found or null if it was not found.
     */
    protected IContainer getContainerInProject(IPath location, IProject project) {
        IContainer file = getContainerInContainer(location, project);
        if (file != null) {
            return file;
        }
        return null;
    }

}