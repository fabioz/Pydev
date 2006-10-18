package org.python.pydev.navigator;

import org.eclipse.core.resources.IFile;

/**
 * Note that the python file here does not actually mean a .py or .pyw file (it can be
 * any file, such as .txt, .gif, etc)
 * 
 * @author fabioz
 */
public class PythonFile extends WrappedResource<IFile> {

	public PythonFile(Object parentElement, IFile actualObject, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, actualObject, pythonSourceFolder, IWrappedResource.RANK_PYTHON_FILE);
	}

}
