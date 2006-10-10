package org.python.pydev.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Note that the python file here does not actually mean a .py or .pyw file (it can be
 * any file, such as .txt, .gif, etc)
 * 
 * @author fabioz
 */
public class PythonFile extends ChildResource<IFile> implements IAdaptable{

	public PythonFile(Object parentElement, IFile actualObject, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, actualObject, pythonSourceFolder);
	}

    public Object getAdapter(Class adapter) {
        return this.getActualObject().getAdapter(adapter);
    }

}
