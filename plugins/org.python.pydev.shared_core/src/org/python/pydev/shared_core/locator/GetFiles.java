package org.python.pydev.shared_core.locator;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.log.Log;

public class GetFiles {
    /**
     * This method is a workaround for w.getRoot().getFileForLocation(path); which does not work consistently because
     * it filters out files which should not be filtered (i.e.: if a project is not in the workspace but imported).
     * 
     * Also, it can fail to get resources in linked folders in the pythonpath.
     * 
     * @param project is optional (may be null): if given we'll search in it dependencies first.
     */
    public IFile getFileForLocation(IPath location, IProject project) {
        boolean stopOnFirst = true;
        IFile[] filesForLocation = getFilesForLocation(location, project, stopOnFirst);
        if (filesForLocation != null && filesForLocation.length > 0) {
            return filesForLocation[0];
        }
        return null;
    }

    /**
     * This method is a workaround for w.getRoot().getFilesForLocation(path); which does not work consistently because
     * it filters out files which should not be filtered (i.e.: if a project is not in the workspace but imported).
     * 
     * Also, it can fail to get resources in linked folders in the pythonpath.
     * 
     * @param project is optional (may be null): if given we'll search in it dependencies first.
     */
    public IFile[] getFilesForLocation(IPath location, IProject project, boolean stopOnFirst) {
        ArrayList<IFile> lst = new ArrayList<>();
        if (SharedCorePlugin.inTestMode()) {
            return lst.toArray(new IFile[0]);
        }
        HashSet<IProject> checked = new HashSet<>();
        IWorkspace w = ResourcesPlugin.getWorkspace();
        if (project != null) {
            checked.add(project);
            IFile f = getFileInProject(location, project);
            if (f != null) {
                if (stopOnFirst) {
                    return new IFile[] { f };
                } else {
                    lst.add(f);
                }
            }
            try {
                IProject[] referencedProjects = project.getDescription().getReferencedProjects();
                for (int i = 0; i < referencedProjects.length; i++) {
                    IProject p = referencedProjects[i];
                    checked.add(p);
                    f = getFileInProject(location, p);
                    if (f != null) {
                        if (stopOnFirst) {
                            return new IFile[] { f };
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
            IFile f = getFileInProject(location, p);
            if (f != null) {
                if (stopOnFirst) {
                    return new IFile[] { f };
                } else {
                    lst.add(f);
                }
            }
        }
        return lst.toArray(new IFile[0]);
    }

    /**
     * Gets an IFile inside a container given a path in the filesystem (resolves the full path of the container and
     * checks if the location given is under it).
     */
    protected IFile getFileInContainer(IPath location, IContainer container) {
        IPath projectLocation = container.getLocation();
        if (projectLocation != null) {
            if (projectLocation.isPrefixOf(location)) {
                int segmentsToRemove = projectLocation.segmentCount();
                IPath removingFirstSegments = location.removeFirstSegments(segmentsToRemove);
                if (removingFirstSegments.segmentCount() == 0) {
                    //It's equal: as we want a file in the container, and the path to the file is equal to the
                    //container, we have to return null (because it's equal to the container it cannot be a file).
                    return null;
                }
                IFile file = container.getFile(removingFirstSegments);
                if (file.exists()) {
                    return file;
                }
            }
        } else {
            if (container instanceof IProject) {
                Log.logInfo("Info: Project: " + container + " has no associated location.");
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
    protected IFile getFileInProject(IPath location, IProject project) {
        IFile file = getFileInContainer(location, project);
        if (file != null) {
            return file;
        }
        return null;
    }

}
