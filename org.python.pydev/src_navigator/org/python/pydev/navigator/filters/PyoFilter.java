package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class PyoFilter extends ViewerFilter{

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if(element instanceof IAdaptable){
			IAdaptable adaptable = (IAdaptable) element;
			Object adapted = adaptable.getAdapter(IFile.class);
			if(adapted instanceof IFile){
				IFile resource = (IFile) adapted;
				if(resource.getName().endsWith(".pyo")){
					return false;
				}
			}
		}
		return true;
	}

}
