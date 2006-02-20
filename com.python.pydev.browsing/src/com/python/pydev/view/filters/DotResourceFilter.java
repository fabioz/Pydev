package com.python.pydev.view.filters;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.Viewer;

public class DotResourceFilter extends AbstractViewerFilter {
	public DotResourceFilter() {
		name = ".* resources";			
		description = "Hides resources with names that start with a '.'";
		initiallyEnabled = false;
		id = "com.python.pydev.browsing.view.actions.DotResourceFilter";
	}
	
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if( element instanceof IFile ) {
			IFile file = (IFile)element;
			return !file.getName().startsWith(".");
		}
		return true;
	}		
}
