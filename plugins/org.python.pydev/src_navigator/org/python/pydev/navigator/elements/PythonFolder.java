/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

import org.eclipse.core.resources.IFolder;

/**
 * Class representing a folder within the pythonpath.
 * 
 * @author Fabio
 */
public class PythonFolder extends WrappedResource<IFolder> {

    public PythonFolder(IWrappedResource parentElement, IFolder folder, PythonSourceFolder pythonSourceFolder) {
        super(parentElement, folder, pythonSourceFolder, IWrappedResource.RANK_PYTHON_FOLDER);
        //System.out.println("Created PythonFolder:"+this+" - "+actualObject+" parent:"+parentElement);
    }
}
