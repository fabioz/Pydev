package com.python.pydev.view.filters;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.Viewer;

public class PycFilter extends AbstractViewerFilter {
	public PycFilter() {
		name = "*.pyc resources";			
		description = "Hides resources with names that end with 'pyc'";
		initiallyEnabled = false;
		id = "com.python.pydev.browsing.view.actions.PycFilter";
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if( element instanceof IFile ) {
			IFile file = (IFile)element;
			return !file.getName().endsWith(".pyc");
		}
		return true;
	}
}
