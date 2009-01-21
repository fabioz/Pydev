package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class DotStartFilter extends ViewerFilter{

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(element instanceof IAdaptable){
            IAdaptable adaptable = (IAdaptable) element;
            Object adapted = adaptable.getAdapter(IResource.class);
            if(adapted instanceof IResource){
                IResource resource = (IResource) adapted;
                if(resource.getName().startsWith(".")){
                    return false;
                }
            }
        }
        return true;
    }

}
