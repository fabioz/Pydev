/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.nature;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure;

/**
 * @author radim@kubacki.cz (Radim Kubacki)
 */
public interface IPythonPathHelper {

    /**
     * 
     * @param fullPath this is the full path of the module. Only for directories or py,pyd,dll,pyo files.
     * @return a String with the module that the file or folder should represent. E.g.: compiler.ast
     */
    public String resolveModule(String fullPath);

    /**
     * Sets the python path to operate on.
     * 
     * @param string with paths separated by {@code |}
     */
    public void setPythonPath(String string);

    /**
     * Getter for Python path.
     * @return list of Python path entries.
     */
    public List<String> getPythonpath();

    /**
     * This method should traverse the pythonpath passed and return a structure with the info that could be collected
     * about the files that are related to python modules.
     */
    public ModulesFoundStructure getModulesFoundStructure(IProgressMonitor monitor);
}
