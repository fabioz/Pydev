package org.python.pydev.navigator.filters;

import org.eclipse.jface.viewers.Viewer;

public class DotStartFilter extends AbstractFilter{

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        String name = getName(element);
		if(name != null && name.startsWith(".")){
            return false;
        }

        return true;
    }

}
