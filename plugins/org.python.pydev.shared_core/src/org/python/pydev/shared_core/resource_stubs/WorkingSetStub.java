/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkingSet;

public class WorkingSetStub implements IWorkingSet {

    private List<IAdaptable> elements = new ArrayList<IAdaptable>();

    public IAdaptable[] adaptElements(IAdaptable[] objects) {
        throw new RuntimeException("Not implemented");
    }

    public IAdaptable[] getElements() {
        return elements.toArray(new IAdaptable[0]);
    }

    public String getId() {
        throw new RuntimeException("Not implemented");
    }

    public ImageDescriptor getImage() {
        throw new RuntimeException("Not implemented");
    }

    public ImageDescriptor getImageDescriptor() {
        throw new RuntimeException("Not implemented");
    }

    public String getLabel() {
        throw new RuntimeException("Not implemented");
    }

    public String getName() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isAggregateWorkingSet() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isEditable() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isEmpty() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isSelfUpdating() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isVisible() {
        throw new RuntimeException("Not implemented");
    }

    public void setElements(IAdaptable[] elements) {
        throw new RuntimeException("Not implemented");
    }

    public void setId(String id) {
        throw new RuntimeException("Not implemented");
    }

    public void setLabel(String label) {
        throw new RuntimeException("Not implemented");
    }

    public void setName(String name) {
        throw new RuntimeException("Not implemented");
    }

    public String getFactoryId() {
        throw new RuntimeException("Not implemented");
    }

    public void saveState(IMemento memento) {
        throw new RuntimeException("Not implemented");
    }

    public Object getAdapter(Class adapter) {
        throw new RuntimeException("Not implemented");
    }

    public void addElement(ProjectStub project) {
        this.elements.add(project);
    }
}
