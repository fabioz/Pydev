/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class WorkspaceRootStub extends AbstractIWorkspaceRootStub implements IWorkbenchAdapter {

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == IWorkbenchAdapter.class) {
            return this;
        }
        throw new RuntimeException("Not implemented for: " + adapter);
    }

    //IWorkbenchAdapter
    List<Object> children = new ArrayList<Object>();

    public void addChild(Object child) {
        children.add(child);
    }

    public Object[] getChildren(Object o) {
        return children.toArray();
    }

    public ImageDescriptor getImageDescriptor(Object object) {
        throw new RuntimeException("Not implemented");
    }

    public String getLabel(Object o) {
        throw new RuntimeException("Not implemented");
    }

    public Object getParent(Object o) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IProject getProject() {
        return null;
    }

    @Override
    public IContainer getParent() {
        return null;
    }

}
