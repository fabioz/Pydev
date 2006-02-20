package com.python.pydev.view.filters;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.team.core.RepositoryProvider;

public class NonSharedProjectFilter extends AbstractViewerFilter {
	
	public NonSharedProjectFilter() {
		name = "Non-shared projects";			
		description = "Shows only shared projects";
		initiallyEnabled = false;
		id = "com.python.pydev.browsing.view.actions.NonSharedProjectFilter";
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IProject)
			return isSharedProject((IProject)element);
		return true;
	}
	
	private boolean isSharedProject(IProject project) {
		return !project.isAccessible() || RepositoryProvider.isShared(project);
	}
}
