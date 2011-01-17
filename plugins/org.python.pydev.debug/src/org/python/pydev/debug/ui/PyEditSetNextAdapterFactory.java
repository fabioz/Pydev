package org.python.pydev.debug.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.python.pydev.debug.model.PySetNextTarget;
import org.python.pydev.debug.ui.actions.ISetNextTarget;
import org.python.pydev.editor.PyEdit;

public class PyEditSetNextAdapterFactory implements IAdapterFactory {

	private static PySetNextTarget pySetNextTarget = new PySetNextTarget();

	public Object getAdapter(Object adaptableObject, Class adapterType) {
        if(adaptableObject instanceof PyEdit && adapterType == ISetNextTarget.class){
            return pySetNextTarget;
            
        }
        return null;	}

	public Class[] getAdapterList() {
		return new Class[]{IRunToLineTarget.class};
	}

}
