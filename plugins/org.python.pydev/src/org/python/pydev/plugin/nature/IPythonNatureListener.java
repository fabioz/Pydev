/*
 * Created on Oct 28, 2006
 * @author Fabio
 */
package org.python.pydev.plugin.nature;

import org.eclipse.core.resources.IProject;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;

/**
 * Interface that should be used for clients that want to know:
 * 
 * - when the project pythonpath has been rebuilt
 * 
 * @author Fabio
 */
public interface IPythonNatureListener {

    /**
     * Notification that the pythonpath has been rebuilt.
     * 
     * @param project is the project that had the pythonpath rebuilt
     * @param nature the project pythonpath used when rebuilding {@link IPythonPathNature#getCompleteProjectPythonPath()}
     */
    void notifyPythonPathRebuilt(IProject project, IPythonNature nature);
    
    
}
