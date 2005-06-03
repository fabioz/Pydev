/*
 * License: Common Public License v1.0
 * Created on Jun 2, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin.nature;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * @author Fabio Zadrozny
 */
public interface IPythonPathNature {

    /**
     * Sets the project this python path nature is associated with
     * @param project
     */
    public void setProject(IProject project);

    /**
     * @return the pythonpath (source and externals) for the project as a list of strings
     */
    public List getCompleteProjectPythonPath();

    /**
     * @return the pythonpath (source and externals) as a string (paths separated by | )
     * @throws CoreException
     */
    public String getOnlyProjectPythonPathStr() throws CoreException;

    /**
     * Sets the project source path (paths are relative to the project location and are separated by | ) 
     * 
     * @param newSourcePath
     * @throws CoreException
     */
    public void setProjectSourcePath(String newSourcePath) throws CoreException;

    /**
     * Sets the project external source paths (those are full-paths for mapping to a file in the filesystem, separated by | ).
     * 
     * @param newExternalSourcePath
     * @throws CoreException
     */
    public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException;

    /**
     * @return only the project source paths (paths are relative to the project location and are separated by | )
     * @throws CoreException
     */
    public String getProjectSourcePath() throws CoreException;

    /**
     * @return only the project external source paths (those are full-paths for mapping to a file in the filesystem, separated by | ).
     * @throws CoreException
     */
    public String getProjectExternalSourcePath() throws CoreException;

}
