package org.python.pydev.navigator.elements;

import org.eclipse.core.resources.IFolder;

/**
 * Class representing a folder within the pythonpath.
 * 
 * @author Fabio
 */
public class PythonFolder extends WrappedResource<IFolder>{

    public PythonFolder(IWrappedResource parentElement, IFolder folder, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, folder, pythonSourceFolder, IWrappedResource.RANK_PYTHON_FOLDER);
        //System.out.println("Created PythonFolder:"+this+" - "+actualObject+" parent:"+parentElement);
    }
}
