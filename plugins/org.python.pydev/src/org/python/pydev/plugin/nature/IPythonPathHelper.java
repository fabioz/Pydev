/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.nature;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure;

/**
 * @author radim@kubacki.cz (Radim Kubacki)
 */
public interface IPythonPathHelper {

    /**
     * Given the absolute file system location of a module, returns the qualified module name.
     *
     * @param absoluteModuleLocation this is the location of the module. Only for directories, or
     *      .py, .pyd, .dll, .so, .pyo files.
     * @return the dot-separated qualified name of the module that the file or folder should represent.
     *      E.g.: compiler.ast
     */
    public String resolveModule(String absoluteModuleLocation, IProject project);

    /**
     * Sets the python path to operate on.
     * 
     * @param string with paths separated by {@code |}
     */
    public void setPythonPath(String string);

    /**
     * Getter for Python path.
     *
     * @return list of Python path entries.
     */
    public List<String> getPythonpath();

    /**
     * This method should traverse the pythonpath passed and return a structure with the info that could be collected
     * about the files that are related to python modules.
     */
    public ModulesFoundStructure getModulesFoundStructure(IProgressMonitor monitor);
}
