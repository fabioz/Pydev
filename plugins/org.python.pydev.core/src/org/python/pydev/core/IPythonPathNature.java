/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 2, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.shared_core.structure.OrderedMap;

/**
 * @author Fabio Zadrozny
 */
public interface IPythonPathNature {

    /**
     * Sets the project this python path nature is associated with
     * @param project
     */
    public void setProject(IProject project, IPythonNature nature);

    /**
     * @param interpreter: this is the interpreter that should be used for getting the pythonpathString interpreter
     *     (if it is null, the default interpreter is used)
     *     
     * @param manager this is the interpreter manager that contains the interpreter passed. It's needed so that we
     *   can get the actual pythonpath for the interpreter passed (needed for the system pythonpath info).
     *   
     * @return the pythonpath (source and externals) for the project as a list of strings (always as paths
     * in the filesystem properly substituted)
     */
    public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter, IInterpreterManager info);

    /**
     * @param addExternal if true, the external libraries will also be added (and not only the project 
     * source folders)
     * 
     * @return the pythonpath (source and externals) as a string (paths separated by | ), and always as
     * complete paths in the filesystem.
     * @throws CoreException
     */
    public String getOnlyProjectPythonPathStr(boolean addExternal) throws CoreException;

    /**
     * Sets the project source path (paths are relative to the project location and are separated by | ) 
     * It can contain variables to be substituted.
     * 
     * @param newSourcePath
     * @throws CoreException
     */
    public void setProjectSourcePath(String newSourcePath) throws CoreException;

    /**
     * Sets the project external source paths (those are full-paths for mapping to a file in the filesystem, separated by | ).
     * It can contain variables to be substituted.
     * 
     * @param newExternalSourcePath
     * @throws CoreException
     */
    public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException;

    /**
     * @param replaceVariables if true, any variables must be substituted (note that the return should still be always 
     * interpreted relative to the project location)
     * @return only the project source paths (paths are relative to the project location and are separated by | )
     * @throws CoreException
     */
    public String getProjectSourcePath(boolean replaceVariables) throws CoreException;

    /**
     * @param replaceVariables if true, any variables must be substituted
     * @return only the project external source paths (those are full-paths for mapping to a file in the filesystem, separated by | ).
     * @throws CoreException
     */
    public String getProjectExternalSourcePath(boolean replaceVariables) throws CoreException;

    public List<String> getProjectExternalSourcePathAsList(boolean replaceVariables) throws CoreException;

    /**
     * @param replaceVariables if true, any variables must be substituted (note that the return should still be always 
     * interpreted relative to the project location)
     * @return only the project source paths as a list of strings (paths are relative to the project location)
     * @throws CoreException
     */
    public Set<String> getProjectSourcePathSet(boolean replaceVariables) throws CoreException;

    /**
     * This is a method akin to getProjectSourcePathSet, but it will return an ordered map where
     * we map the version with variables resolved to the version without variables resolved.
     * 
     * It should be used when doing some PYTHONPATH manipulation based on the current values, so,
     * we can keep the values with the variables when doing some operation while being able to check
     * for the resolved paths to check if some item should be actually added or not.
     */
    public OrderedMap<String, String> getProjectSourcePathResolvedToUnresolvedMap() throws CoreException;

    /**
     * Can be called to force the cleaning of the caches (needed when the nature is rebuilt)
     */
    public void clearCaches();

    /**
     * This method sets a variable substitution so that the source folders (project and external) can be set 
     * based on those variables.
     * 
     * E.g.: If a variable PLATFORM maps to win32, setting a source folder as /libs/${PLATFORM}/dlls, it will be
     * resolved in the project as /libs/win32/dlls.
     * 
     * Another example would be creating a varible MY_APP that maps to d:\bin\my_app, so, ${MY_APP}/libs would point to
     * d:\bin\my_app/libs
     * 
     * Note that this variables are set at the project level and are resolved later than at the system level, so,
     * when performing the substitution, it should get the variables from the interpreter and override those with
     * the project variables before actually resolving anything.
     */
    public void setVariableSubstitution(Map<String, String> variableSubstitution) throws CoreException;

    /**
     * Same as getVariableSubstitution(true);
     */
    public Map<String, String> getVariableSubstitution() throws CoreException, MisconfigurationException,
            PythonNatureWithoutProjectException;

    /**
     * @param addInterpreterInfoSubstitutions if true the substitutions in the interpreter will also be added.
     * Otherwise, only the substitutions from this nature will be returned.
     */
    public Map<String, String> getVariableSubstitution(boolean addInterpreterInfoSubstitutions) throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException;

    /**
     * The nature that contains this pythonpath nature.
     * @return
     */
    public IPythonNature getNature();

    /**
     * Gets the folders or zip files which are added to the pythonpath relative to the project. Won't add external files
     * (as it's made only to get what's inside the workspace).
     */
    public Set<IResource> getProjectSourcePathFolderSet() throws CoreException;

}
