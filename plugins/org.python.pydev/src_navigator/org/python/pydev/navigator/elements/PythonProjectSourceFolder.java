/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

import org.eclipse.core.resources.IProject;

/**
 * Basically, this class represents a project when it is specified as a source folder.
 * 
 * @author Fabio
 */
public class PythonProjectSourceFolder extends PythonSourceFolder {

    public PythonProjectSourceFolder(Object parentElement, IProject project) {
        super(parentElement, project);
    }

}
