/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.OrderedMap;

/**
 * Action used to add a file folder to its project's PYTHONPATH
 * (or a project itself to its own PYTHONPATH),
 * as an alternative to the Project Properties menu.  
 *  
 * @author Andrew
 */
public class PyAddSrcFolder extends PyContainerAction {

    @Override
    protected boolean confirmRun() {
        return true;
    }

    @Override
    protected void afterRun(int resourcesAffected) {

    }

    @Override
    protected int doActionOnContainer(IContainer container, IProgressMonitor monitor) {
        try {
            IProject project = container.getProject();
            IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);
            if (pythonPathNature == null) {
                Log.log("Unable to get PythonNature on project: " + project);
                return 0;
            }
            OrderedMap<String, String> projectSourcePathMap = pythonPathNature
                    .getProjectSourcePathResolvedToUnresolvedMap();
            String pathToAdd = container.getFullPath().toString();
            if (projectSourcePathMap.containsKey(pathToAdd)) {
                return 0;
            }

            pathToAdd = PyStructureConfigHelpers.convertToProjectRelativePath(project.getFullPath().toString(),
                    pathToAdd);

            Collection<String> values = new ArrayList<String>(projectSourcePathMap.values());
            values.add(pathToAdd);
            pythonPathNature.setProjectSourcePath(StringUtils.join("|", values));
            PythonNature.getPythonNature(project).rebuildPath();
            return 1;
        } catch (CoreException e) {
            Log.log(IStatus.ERROR, "Unexpected error setting project properties", e);
        }
        return 0;
    }

}
