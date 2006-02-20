package com.python.pydev.view.filters;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.Viewer;

public class PythonFileFilter extends AbstractViewerFilter {

	public PythonFileFilter() {	
		name = "Python files";			
		description = "Hides all Python files";
		initiallyEnabled = false;
		id = "com.python.pydev.browsing.view.actions.PythonFileFilter";
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if( element instanceof IFile ) {
			IFile file = (IFile)element;
			return !file.getName().endsWith(".py");
		}
		return true;
	}
}
