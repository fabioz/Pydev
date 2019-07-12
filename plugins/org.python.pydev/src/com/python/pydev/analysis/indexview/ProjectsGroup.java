/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.python.pydev.plugin.nature.PythonNature;

public class ProjectsGroup extends ElementWithChildren {

    public ProjectsGroup(ITreeElement indexRoot) {
        super(indexRoot);
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public String toString() {
        return "Projects";
    }

    @Override
    protected void calculateChildren() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            PythonNature nature = PythonNature.getPythonNature(iProject);
            if (nature != null) {
                addChild(new NatureGroup(this, nature));
            }
        }
    }

}
