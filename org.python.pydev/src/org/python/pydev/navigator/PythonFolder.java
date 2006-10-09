package org.python.pydev.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

public class PythonFolder extends ChildResource<IFolder> implements IAdaptable{

    public PythonFolder(Object parentElement, IFolder folder, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, folder, pythonSourceFolder);
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IFolder.class) {
            return this.getActualObject();
        }
        if (adapter == IContainer.class) {
            return this.getActualObject();
        }
        if (adapter == IResource.class) {
            return this.getActualObject();
        }
        return null;
    }

}
