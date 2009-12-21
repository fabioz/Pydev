package org.python.pydev.debug.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.python.pydev.debug.model.PyRunToLineTarget;
import org.python.pydev.editor.PyEdit;

public class PyEditRunToLineAdapterFactory implements IAdapterFactory{
    
    private static PyRunToLineTarget pyRunToLineTarget = new PyRunToLineTarget();

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if(adaptableObject instanceof PyEdit && adapterType == IRunToLineTarget.class){
            return pyRunToLineTarget;
            
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[]{IRunToLineTarget.class};
    }
}