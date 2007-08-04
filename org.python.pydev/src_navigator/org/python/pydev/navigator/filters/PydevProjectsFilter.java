package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.plugin.nature.PythonNature;

public class PydevProjectsFilter extends ViewerFilter{

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(element instanceof IAdaptable){
            IAdaptable adaptable = (IAdaptable) element;
            Object adapted = adaptable.getAdapter(IProject.class);
            if(adapted instanceof IProject){
                IProject project = (IProject) adapted;
                return PythonNature.getPythonNature(project) != null;
            }
        }
        return true;
    }

}
