/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.common;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.plugin.nature.PythonNature;

public class CustomizationCommons {

    public static IPythonPathNature getPythonPathNatureFromObject(Object receiver) {
        IResource resource = getResourceFromObject(receiver);

        if (resource == null) {
            return null;
        }

        IProject project = resource.getProject();
        if (project == null) {
            return null;
        }

        IPythonPathNature nature = PythonNature.getPythonPathNature(project);
        return nature;
    }

    public static IResource getResourceFromObject(Object receiver) {
        if (receiver instanceof IWrappedResource) {
            IWrappedResource wrappedResource = (IWrappedResource) receiver;
            Object actualObject = wrappedResource.getActualObject();
            if (actualObject instanceof IResource) {
                return (IResource) actualObject;
            }
        }
        if (receiver instanceof IResource) {
            return (IResource) receiver;
        }
        return null;
    }

    public static IContainer getContainerFromObject(Object receiver) {
        if (receiver instanceof IWrappedResource) {
            IWrappedResource wrappedResource = (IWrappedResource) receiver;
            Object actualObject = wrappedResource.getActualObject();
            if (actualObject instanceof IContainer) {
                return (IContainer) actualObject;
            }
        }
        if (receiver instanceof IContainer) {
            return (IContainer) receiver;
        }
        return null;
    }
}
