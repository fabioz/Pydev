package org.python.pydev.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;


public class PythonResource extends ChildResource<Object> implements IAdaptable{

	public PythonResource(Object parentElement, Object object, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, object, pythonSourceFolder);
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IResource.class) {
            return this.getActualObject();
        }
        return null;
    }


}
