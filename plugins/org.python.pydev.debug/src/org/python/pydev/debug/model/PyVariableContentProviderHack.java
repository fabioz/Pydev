package org.python.pydev.debug.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.model.elements.VariableContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;

/**
 * This class was created to bypass bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=238878
 * 
 * It should be removed when it's actually fixed.
 * @author Fabio
 */
public class PyVariableContentProviderHack extends VariableContentProvider{


    protected boolean hasChildren(Object element, IPresentationContext context, IViewerUpdate monitor) throws CoreException {
        if(element instanceof IWatchExpression){
            IWatchExpression watchExpression = (IWatchExpression) element;
            return super.hasChildren(watchExpression.getValue(), context, monitor);
        }
        if(element instanceof PyVariableCollection){
            PyVariableCollection pyVariableCollection = (PyVariableCollection) element;
            return pyVariableCollection.hasVariables();
        }
        return super.hasChildren(element, context, monitor);
    }

    
    @Override
    protected Object[] getAllChildren(Object parent,
            IPresentationContext context) throws CoreException {
        if(parent instanceof IWatchExpression){
            IWatchExpression watchExpression = (IWatchExpression) parent;
            return super.getAllChildren(watchExpression.getValue(), context);
        }
        if(parent instanceof PyVariableCollection){
            PyVariableCollection pyVariableCollection = (PyVariableCollection) parent;
            return pyVariableCollection.getVariables();
        }
        return super.getAllChildren(parent, context);
    }
}
