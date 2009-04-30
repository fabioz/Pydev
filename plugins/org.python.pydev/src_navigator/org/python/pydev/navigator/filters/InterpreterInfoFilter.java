package org.python.pydev.navigator.filters;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.navigator.InterpreterInfoTreeNode;

public class InterpreterInfoFilter extends ViewerFilter{

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(element instanceof InterpreterInfoTreeNode<?>){
            return false;
        }
        return true;
    }

}
