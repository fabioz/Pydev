package org.python.pydev.navigator;

import org.eclipse.core.resources.IFile;

/**
 * Note that the python file here does not actually mean a .py or .pyw file (it can be
 * any file, such as .txt, .gif, etc)
 * 
 * @author fabioz
 */
public class PythonFile implements IChildResource{

    public IFile file;
    public Object parentElement;
    //it knows its direct parent and the source folder
	public PythonSourceFolder pythonSourceFolder;

	public PythonFile(Object parentElement, IFile file, PythonSourceFolder pythonSourceFolder) {
        this.parentElement = parentElement;
        this.file = file;
        this.pythonSourceFolder = pythonSourceFolder;
    }

	public Object getParent() {
		return parentElement;
	}

	public Object getActualObject() {
		return file;
	}

	public PythonSourceFolder getSourceFolder() {
		return pythonSourceFolder;
	}



}
