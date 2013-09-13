/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

import org.eclipse.core.resources.IResource;

public class PythonResource extends WrappedResource<IResource> {

    public PythonResource(IWrappedResource parentElement, IResource object, PythonSourceFolder pythonSourceFolder) {
        super(parentElement, object, pythonSourceFolder, IWrappedResource.RANK_PYTHON_RESOURCE);
        //System.out.println("Created PythonResource:"+this+" - "+actualObject+" parent:"+parentElement);
    }

}
