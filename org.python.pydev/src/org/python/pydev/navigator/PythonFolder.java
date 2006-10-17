package org.python.pydev.navigator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IAdaptable;

public class PythonFolder extends ChildResource<IFolder> implements IAdaptable{

    public PythonFolder(Object parentElement, IFolder folder, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, folder, pythonSourceFolder, IChildResource.RANK_PYTHON_FOLDER);
    }

    public Object getAdapter(Class adapter) {
        return this.getActualObject().getAdapter(adapter);
    }

}
