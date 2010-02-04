package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;

public class ClosedProjectsFilter extends AbstractFilter{

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(element instanceof IAdaptable){
            IAdaptable adaptable = (IAdaptable) element;
            Object adapted = adaptable.getAdapter(IProject.class);
            if(adapted instanceof IProject){
                IProject project = (IProject) adapted;
                if(project.isOpen()){
                    return true;
                }else{
                    return false;
                }
            }
        }
        return true;
    }

}
