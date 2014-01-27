package org.python.pydev.navigator.filters;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.Viewer;

/**
 * Hide __pycache__ folders.
 */
public class PyCacheFilter extends AbstractFilter {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        String name = getName(element);
        if (name != null
                && name.equals("__pycache__")
                && (element instanceof IContainer || (element instanceof IAdaptable && ((IAdaptable) element)
                        .getAdapter(IContainer.class) != null))) {
            return false;
        }

        return true;
    }

}
