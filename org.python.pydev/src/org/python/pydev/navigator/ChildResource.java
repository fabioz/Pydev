package org.python.pydev.navigator;


public class ChildResource<X> implements IChildResource{

	protected Object parentElement;
	protected X actualObject;
	protected PythonSourceFolder pythonSourceFolder;
    protected int rank;

	public ChildResource(Object parentElement, X actualObject, PythonSourceFolder pythonSourceFolder, int rank) {
		this.parentElement = parentElement;
		this.actualObject = actualObject;
		this.pythonSourceFolder = pythonSourceFolder;
		this.pythonSourceFolder.addChild(actualObject, this);
        this.rank = rank;
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
    
    public int getRank() {
        return rank;
    }


}
