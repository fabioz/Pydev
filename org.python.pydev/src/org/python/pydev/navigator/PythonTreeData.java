/*
 * Created on Oct 7, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.core.resources.IFile;

public class PythonTreeData {

    public IFile parentElement;

    public PythonTreeData(IFile parentElement) {
        this.parentElement = parentElement;
    }
    
    @Override
    public String toString() {
        return parentElement.toString();
    }

}
