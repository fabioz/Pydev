/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IContributorResourceAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * This class represents a resource that is wrapped for the python model.
 *
 * @author Fabio
 *
 * @param <X>
 */
public class WrappedResource<X extends IResource> implements IWrappedResource, IContributorResourceAdapter, IAdaptable {

    protected IWrappedResource parentElement;
    protected X actualObject;
    protected PythonSourceFolder pythonSourceFolder;
    protected int rank;

    public WrappedResource(IWrappedResource parentElement, X actualObject, PythonSourceFolder pythonSourceFolder,
            int rank) {
        this.parentElement = parentElement;
        this.actualObject = actualObject;
        this.pythonSourceFolder = pythonSourceFolder;
        this.pythonSourceFolder.addChild(this);
        this.rank = rank;
    }

    @Override
    public X getActualObject() {
        return actualObject;
    }

    @Override
    public IWrappedResource getParentElement() {
        return parentElement;
    }

    @Override
    public PythonSourceFolder getSourceFolder() {
        return pythonSourceFolder;
    }

    @Override
    public int getRank() {
        return rank;
    }

    @Override
    public IResource getAdaptedResource(IAdaptable adaptable) {
        return getActualObject();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof IWrappedResource) {
            if (other == this) {
                return true;
            }
            IWrappedResource w = (IWrappedResource) other;
            return this.actualObject.equals(w.getActualObject());
        }
        return false;

        //now returns always false because it could generate null things in the search page... the reason is that when the
        //decorator manager had an update and passed in the search page, it thought that a file/folder was the python file/folder,
        //and then, later when it tried to update it with that info, it ended up removing the element because it didn't know how
        //to handle it.
        //
        // -- and this was also not a correct equals, because other.equals(this) would not return true as this was returning
        // (basically we can't compare apples to oranges)
        //        return actualObject.equals(other);
    }

    @Override
    public int hashCode() {
        return this.getActualObject().hashCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IContributorResourceAdapter.class) {
            return (T) this;
        }
        return WrappedResource.getAdapterFromActualObject(this.getActualObject(), adapter);
    }

    @Override
    public String toString() {
        FastStringBuffer buf = new FastStringBuffer();
        buf.append(FullRepIterable.getLastPart(super.toString())); //something as org.eclipse.ui.internal.WorkingSet@2813 will become WorkingSet@2813
        buf.append(" (");
        buf.append(this.getActualObject().toString());
        buf.append(")");
        return buf.toString();
    }

    public static <T> T getAdapterFromActualObject(IResource actualObject2, Class<T> adapter) {
        if (IDeferredWorkbenchAdapter.class.equals(adapter) || IWorkbenchAdapter2.class.equals(adapter)
                || IWorkbenchAdapter.class.equals(adapter)) {
            return null;
        }
        return actualObject2.getAdapter(adapter);
    }
}
