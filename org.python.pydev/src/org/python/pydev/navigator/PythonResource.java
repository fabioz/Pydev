package org.python.pydev.navigator;


public class PythonResource implements IChildResource{

    public Object object;
    public Object parentElement;
    //it knows its direct parent and the source folder
	public PythonSourceFolder pythonSourceFolder;

	public PythonResource(Object parentElement, Object object, PythonSourceFolder pythonSourceFolder) {
        this.parentElement = parentElement;
        this.object = object;
        this.pythonSourceFolder = pythonSourceFolder;
    }

	public Object getParent() {
		return parentElement;
	}

	public Object getActualObject() {
		return object;
	}

	public PythonSourceFolder getSourceFolder() {
		return pythonSourceFolder;
	}


}
