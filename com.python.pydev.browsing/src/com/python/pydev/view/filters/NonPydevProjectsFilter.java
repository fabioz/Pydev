package com.python.pydev.view.filters;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;

public class NonPydevProjectsFilter extends AbstractViewerFilter {
	public NonPydevProjectsFilter() {
		name = "Non-Pydev projects";
		description = "Shows only Pydev projects";
		initiallyEnabled = false;
		id = "com.python.pydev.browsing.view.actions.Non-Pydev projects";
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if( element instanceof IProject ) {
			IProject project = (IProject)element;
			try {
				return project.getNature("org.python.pydev")!=null;
			} catch (CoreException e) {			
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}
}
