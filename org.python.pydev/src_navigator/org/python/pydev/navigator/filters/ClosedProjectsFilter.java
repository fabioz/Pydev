package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class ClosedProjectsFilter extends ViewerFilter{

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if(element instanceof IAdaptable){
			IAdaptable adaptable = (IAdaptable) element;
			Object adapted = adaptable.getAdapter(IProject.class);
			if(adapted instanceof IProject){
				IProject resource = (IProject) adapted;
				if(resource.isOpen()){
					return true;
				}else{
					return false;
				}
			}
		}
		return true;
	}

}
