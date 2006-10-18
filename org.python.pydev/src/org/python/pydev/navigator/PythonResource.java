package org.python.pydev.navigator;

import org.eclipse.core.resources.IResource;


public class PythonResource extends WrappedResource<IResource> {

	public PythonResource(Object parentElement, IResource object, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, object, pythonSourceFolder, IWrappedResource.RANK_PYTHON_RESOURCE);
    }

}
