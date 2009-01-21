package org.python.pydev.navigator.elements;

import org.eclipse.core.resources.IProject;

/**
 * Basically, this class represents a project when it is specified as a source folder.
 * 
 * @author Fabio
 */
public class PythonProjectSourceFolder extends PythonSourceFolder{

    public PythonProjectSourceFolder(Object parentElement, IProject project) {
        super(parentElement, project);
    }

}
