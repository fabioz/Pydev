package org.python.pydev.navigator;


public class ChildResource<X> implements IChildResource{

	protected Object parentElement;
	protected X actualObject;
	protected PythonSourceFolder pythonSourceFolder;

	public ChildResource(Object parentElement, X actualObject, PythonSourceFolder pythonSourceFolder) {
		this.parentElement = parentElement;
		this.actualObject = actualObject;
		this.pythonSourceFolder = pythonSourceFolder;
		this.pythonSourceFolder.addChild(actualObject, this);
	}
	
	public X getActualObject() {
		return actualObject;
	}

	public Object getParent() {
		return parentElement;
	}

	public PythonSourceFolder getSourceFolder() {
		return pythonSourceFolder;
	}

}
