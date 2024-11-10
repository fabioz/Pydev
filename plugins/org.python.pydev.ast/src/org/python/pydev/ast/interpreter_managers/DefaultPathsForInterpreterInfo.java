/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ast.interpreter_managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;

public class DefaultPathsForInterpreterInfo {

    private final Set<IPath> rootPaths;

    public DefaultPathsForInterpreterInfo(boolean resolvingInterpreter) {
        boolean addInterpreterInfoSubstitutions = !resolvingInterpreter;
        // When resolving the interpreter, we can't try to resolve variables from the
        // interpreter itself as we could get into a recursion error.
        rootPaths = getRootPaths(addInterpreterInfoSubstitutions);

    }

    public boolean selectByDefault(String data) {
        return !isChildOfRootPath(data, rootPaths);
    }

    public boolean forceDeselect(String data) {
        return isRootPath(data, rootPaths);
    }

    public static boolean exists(String data) {
        return new File(data).exists();
    }

    /**
     * States whether or not a given path is the child of at least one root path of a set of root paths.
     * @param data The path that will be checked for child status.
     * @param rootPaths A set of root paths.
     * @return True if the path of data is a child of any of the paths of rootPaths.
     */
    public static boolean isChildOfRootPath(String data, Set<IPath> rootPaths) {
        IPath path = Path.fromOSString(data);
        java.nio.file.Path nativePath = new File(data).toPath();

        for (IPath p : rootPaths) {
            if (FileUtils.isPrefixOf(p, path)) {
                return true;
            }
            try {
                if (Files.isSameFile(nativePath, p.toFile().toPath())) {
                    return true;
                }
            } catch (IOException e) {
                Log.log(e);
            }
        }
        return false;
    }

    public static boolean isRootPath(String data, Set<IPath> rootPaths) {
        java.nio.file.Path nativePath = new File(data).toPath();

        for (IPath p : rootPaths) {
            try {
                if (Files.isSameFile(nativePath, p.toFile().toPath())) {
                    return true;
                }
            } catch (IOException e) {
                Log.log(e);
            }
        }
        return false;
    }

    /**
     * Creates a Set of the root paths of all projects (and the workspace root itself).
     * @return A HashSet of root paths.
     */
    public static HashSet<IPath> getRootPaths(boolean addInterpreterInfoSubstitutions) {
        HashSet<IPath> rootPaths = new HashSet<IPath>();
        if (SharedCorePlugin.inTestMode()) {
            return rootPaths;
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IPath rootLocation = root.getLocation().makeAbsolute();

        rootPaths.add(rootLocation);

        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            IPath location = iProject.getLocation();
            if (location != null) {
                IPath abs = location.makeAbsolute();
                rootPaths.add(abs);
            }

            PythonNature nature = PythonNature.getPythonNature(iProject);
            if (nature != null) {
                try {
                    List<String> splitted = nature.getPythonPathNature().getOnlyProjectPythonPathStr(true,
                            addInterpreterInfoSubstitutions);
                    for (String s : splitted) {
                        try {
                            rootPaths.add(Path.fromOSString(FileUtils.getFileAbsolutePath(s)));
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
        }
        return rootPaths;
    }
}
