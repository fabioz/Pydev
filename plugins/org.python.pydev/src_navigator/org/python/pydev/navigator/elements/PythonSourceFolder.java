/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.IContributorResourceAdapter;
import org.python.pydev.core.log.Log;

/**
 * This is the the model for a source folder that exists within a project.
 *
 * @author Fabio
 */
public class PythonSourceFolder implements IWrappedResource, IAdaptable, IContributorResourceAdapter {

    public IContainer container;
    public Object parentElement;

    /**
     * Maps the 'actual objects' to their python counterparts
     */
    public Map<IResource, IWrappedResource> children = new HashMap<IResource, IWrappedResource>();

    /**
     * Maps from a wrapped resource (that must be a container) to its children
     */
    public Map<IResource, List<IWrappedResource>> childrenForContainer = new HashMap<IResource, List<IWrappedResource>>();

    protected PythonSourceFolder(Object parentElement, IContainer container) {
        this.parentElement = parentElement;
        this.container = container;
    }

    public PythonSourceFolder(Object parentElement, IFolder folder) {
        this(parentElement, (IContainer) folder);
        //        System.out.println("Created PythonSourceFolder:"+this+" - "+folder+" parent:"+parentElement);
    }

    @Override
    public Object getParentElement() {
        return parentElement;
    }

    @Override
    public IResource getActualObject() {
        return container;
    }

    @Override
    public PythonSourceFolder getSourceFolder() {
        return this;
    }

    public void addChild(IWrappedResource child) {
        IResource actualObject = (IResource) child.getActualObject();

        Object p = child.getParentElement();
        if (p != null && p instanceof IWrappedResource) {
            IWrappedResource pWrapped = (IWrappedResource) p;
            if (pWrapped.getActualObject().equals(actualObject)) {
                Log.log("Trying to add an element that has itself as parent: " + actualObject);
            }
        }

        //if there was already a child to the given object, remove it before adding this one.
        IWrappedResource existing = children.get(actualObject);
        if (existing != null) {
            removeChild(actualObject);
        }

        children.put(actualObject, child);

        IContainer container = actualObject.getParent();
        Assert.isNotNull(container);
        List<IWrappedResource> l = childrenForContainer.get(container);
        if (l == null) {
            l = new ArrayList<IWrappedResource>();
            childrenForContainer.put(container, l);
        }
        l.add(child);
    }

    public void removeChild(IResource actualObject) {
        //System.out.println("Removing child:"+actualObject);
        children.remove(actualObject);
        if (actualObject instanceof IContainer) {
            List<IWrappedResource> l = childrenForContainer.get(actualObject);
            if (l != null) {
                for (IWrappedResource resource : l) {
                    removeChild((IResource) resource.getActualObject());
                }
                childrenForContainer.remove(actualObject);
            }
        }
    }

    public Object getChild(IResource actualObject) {
        if (actualObject == null) {
            return null;
        }
        if (this.getActualObject().equals(actualObject)) {
            return this;
        }
        IWrappedResource ret = children.get(actualObject);
        //System.out.println("Gotten child:"+ret+" for resource:"+actualObject);
        return ret;
    }

    @Override
    public int getRank() {
        return IWrappedResource.RANK_SOURCE_FOLDER;
    }

    @Override
    public IResource getAdaptedResource(IAdaptable adaptable) {
        return getActualObject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IActionFilter.class) {
            IActionFilter platformActionFilter = (IActionFilter) this.getActualObject().getAdapter(adapter);
            return (T) new PythonSourceFolderActionFilter(platformActionFilter);
        }
        if (adapter == IContributorResourceAdapter.class) {
            return (T) this;
        }
        return WrappedResource.getAdapterFromActualObject(this.getActualObject(), adapter);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "PythonSourceFolder [" + this.getActualObject() + "]";
    }

}
