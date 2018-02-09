package org.python.pydev.ast.location;

import java.io.File;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.locator.GetContainers;
import org.python.pydev.shared_core.locator.GetFiles;

/**
 * Helpers to get files in the workspace from files/paths in the filesystem.
 */
public class FindWorkspaceFiles {

    public final static GetFiles getFiles = new GetFiles() {

        @Override
        protected IFile getFileInProject(IPath location, IProject project) {
            IFile file = super.getFileInProject(location, project);
            if (file != null) {
                return file;
            }
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature != null) {
                IPythonPathNature pythonPathNature = nature.getPythonPathNature();
                try {
                    //Paths
                    Set<IResource> projectSourcePathSet = pythonPathNature.getProjectSourcePathFolderSet();
                    for (IResource iResource : projectSourcePathSet) {
                        if (iResource instanceof IContainer) {
                            //I.e.: don't consider zip files
                            IContainer iContainer = (IContainer) iResource;
                            file = getFileInContainer(location, iContainer);
                            if (file != null) {
                                return file;
                            }
                        }
                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
            return null;
        };
    };

    public final static GetContainers getContainers = new GetContainers() {

        @Override
        protected IContainer getContainerInProject(IPath location, IProject project) {
            IContainer file = super.getContainerInProject(location, project);
            if (file != null) {
                return file;
            }
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature != null) {
                IPythonPathNature pythonPathNature = nature.getPythonPathNature();
                try {
                    //Paths
                    Set<IResource> projectSourcePathSet = pythonPathNature.getProjectSourcePathFolderSet();
                    for (IResource iResource : projectSourcePathSet) {
                        if (iResource instanceof IContainer) {
                            //I.e.: don't consider zip files
                            IContainer iContainer = (IContainer) iResource;
                            file = getContainerInContainer(location, iContainer);
                            if (file != null) {
                                return file;
                            }
                        }
                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
            return null;
        };
    };

    /**
     * @param file the file we want to get in the workspace
     * @return a workspace file that matches the given file.
     */
    public static IFile getWorkspaceFile(File file, IProject project) {
        return getFileForLocation(Path.fromOSString(file.getAbsolutePath()), project);
    }

    /**
     * @param file the file we want to get in the workspace
     * @return a workspace file that matches the given file.
     */
    public static IFile[] getWorkspaceFiles(File file) {
        boolean stopOnFirst = false;
        IFile[] files = getFilesForLocation(Path.fromOSString(file.getAbsolutePath()), null, stopOnFirst);
        if (files == null || files.length == 0) {
            return null;
        }

        return files;
    }

    public static IContainer getContainerForLocation(IPath location, IProject project) {
        return getContainers.getContainerForLocation(location, project);
    }

    public static IContainer[] getContainersForLocation(IPath location) {
        boolean stopOnFirst = false;
        return getContainers.getContainersForLocation(location, null, stopOnFirst);
    }

    public static IFile getFileForLocation(IPath location, IProject project) {
        return getFiles.getFileForLocation(location, project);
    }

    public static IFile[] getFilesForLocation(IPath location, IProject project, boolean stopOnFirst) {
        return getFiles.getFilesForLocation(location, project, stopOnFirst);

    }
}
