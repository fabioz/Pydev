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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;

public class DefaultPathsForInterpreterInfo {

    private final Set<IPath> rootPaths;

    public DefaultPathsForInterpreterInfo() {
        rootPaths = getRootPaths();

    }

    public boolean selectByDefault(String data) {
        return !isChildOfRootPath(data, rootPaths);
    }

    public boolean exists(String data) {
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
        for (IPath p : rootPaths) {
            if (FileUtils.isPrefixOf(p, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a Set of the root paths of all projects (and the workspace root itself).
     * @return A HashSet of root paths.
     */
    public static HashSet<IPath> getRootPaths() {
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
                if (!FileUtils.isPrefixOf(rootLocation, abs)) {
                    rootPaths.add(abs);
                }
            }
        }
        return rootPaths;
    }
}
