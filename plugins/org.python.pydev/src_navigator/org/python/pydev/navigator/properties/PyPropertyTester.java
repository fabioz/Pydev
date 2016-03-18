/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.properties;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.PythonProjectSourceFolder;

/**
 * 
 * @author Fabio
 * @see org.eclipse.core.internal.propertytester#ProjectPropertyTester
 * 
 * If not done, we get:
 * 
 * No property tester contributes a property org.eclipse.core.resources.open to 
 * type class org.python.pydev.navigator.elements.PythonProjectSourceFolder
 */
public class PyPropertyTester extends PropertyTester {

    protected boolean toBoolean(Object expectedValue) {
        if (expectedValue instanceof Boolean) {
            return ((Boolean) expectedValue).booleanValue();
        }
        return true;
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (expectedValue == null) {
            return false;
        }
        if ("open".equals(property)) {
            if (receiver instanceof PythonProjectSourceFolder) {
                PythonProjectSourceFolder pythonProjectSourceFolder = (PythonProjectSourceFolder) receiver;
                IResource actualObject = pythonProjectSourceFolder.getActualObject();
                if (actualObject instanceof IProject) {
                    return ((IProject) actualObject).isOpen() == toBoolean(expectedValue);
                }
            }
        } else if ("name".equals(property)) {
            if (receiver instanceof IWrappedResource) {
                IWrappedResource wrappedResource = (IWrappedResource) receiver;
                IResource resource = (IResource) wrappedResource.getAdapter(IResource.class);
                if (resource != null) {
                    return expectedValue.toString().equals(resource.getName());
                }
            }
        }
        return false;
    }

}
