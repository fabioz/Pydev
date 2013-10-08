/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.plugin.nature.PythonNature;

public class PydevProjectsFilter extends AbstractFilter {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) element;
            Object adapted = adaptable.getAdapter(IProject.class);
            if (adapted instanceof IProject) {
                IProject project = (IProject) adapted;
                if (!project.isOpen()) {
                    return true; //As we don't know about the nature of closed projects, don't filter it out.
                }
                return PythonNature.getPythonNature(project) != null;
            }
        }
        return true;
    }

}
