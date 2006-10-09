/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.core.resources.IFolder;

/**
 * Simply works as a delegate for the folder.
 * 
 * @author Fabio
 */
public class PythonSourceFolder{

    public IFolder folder;
    public Object parentElement;

    public PythonSourceFolder(Object parentElement, IFolder folder) {
        this.parentElement = parentElement;
        this.folder = folder;
    }
}
