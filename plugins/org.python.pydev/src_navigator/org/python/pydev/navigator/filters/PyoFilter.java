package org.python.pydev.navigator.filters;

import org.eclipse.jface.viewers.Viewer;

public class PyoFilter extends AbstractFilter{

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        String name = getName(element);
		if(name != null && name.endsWith(".pyo")){
            return false;
        }

        return true;
    }

}
