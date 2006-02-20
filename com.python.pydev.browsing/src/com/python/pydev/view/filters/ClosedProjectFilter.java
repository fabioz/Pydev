package com.python.pydev.view.filters;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.Viewer;

public class ClosedProjectFilter extends AbstractViewerFilter {
	public ClosedProjectFilter() {
		name = "Closed projects";
		description = "Hides closed projects";
		initiallyEnabled = false;
		id = "com.python.pydev.browsing.view.actions.ClosedProjectFilter";
	}
	
	/*
	 * @see ViewerFilter
	 */
	public boolean select(Viewer viewer, Object parent, Object element) {	
		if (element instanceof IResource)
			return ((IResource)element).getProject().isOpen();
		return true;
	}
}
