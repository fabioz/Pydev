package com.python.pydev.view.filters;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.Viewer;

public class EmptyPackageFilter extends AbstractViewerFilter {
	public EmptyPackageFilter() {
		name = "Empty packages";
		description = "Hides all empty packages";
		initiallyEnabled = false;
		id = "com.python.pydev.browsing.view.actions.EmptyPackageFilter";
	}
	
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof IFolder) {			
			try {
				return ((IFolder)element).members().length > 0;
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}
}
