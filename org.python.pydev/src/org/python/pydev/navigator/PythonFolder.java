package org.python.pydev.navigator;

import org.eclipse.core.resources.IFolder;

public class PythonFolder implements IChildResource{

    public IFolder folder;
    public Object parentElement;
    //it knows its direct parent and the source folder
	public PythonSourceFolder pythonSourceFolder;

    public PythonFolder(Object parentElement, IFolder folder, PythonSourceFolder pythonSourceFolder) {
        this.parentElement = parentElement;
        this.folder = folder;
        this.pythonSourceFolder = pythonSourceFolder;
    }

	public Object getParent() {
		return parentElement;
	}

	public Object getActualObject() {
		return folder;
	}

	public PythonSourceFolder getSourceFolder() {
		return pythonSourceFolder;
	}


}
