/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IContributorResourceAdapter;
import org.python.pydev.navigator.elements.IWrappedResource;

/**
 * This adapter factory is needed for the pydev package explorer (no, it's not enough that the objects themselves are
 * adaptable, there must be a factory to make that visible to it).
 *
 * @author Fabio
 */
public class ModelAdapter implements IAdapterFactory {

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adaptableObject instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) adaptableObject;
            T adapter = adaptable.getAdapter(adapterType);
            //System.out.println("Returning adapter:"+adapter+"\tfor:"+adapterType+"\tfor object:"+adaptableObject);
            return adapter;

        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return new Class[] { IWrappedResource.class, ResourceMapping.class, IResource.class, IFolder.class,
                IFile.class, IContainer.class, IContributorResourceAdapter.class, IProject.class };
    }

}
