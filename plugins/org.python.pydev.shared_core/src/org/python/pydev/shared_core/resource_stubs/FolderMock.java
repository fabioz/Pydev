/******************************************************************************
* Copyright (C) 2012  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.resource_stubs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class FolderMock extends AbstractIFolderStub {

    private String name;
    private final List<IResource> members = new ArrayList<IResource>();
    private IContainer parent;

    public FolderMock(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void addMember(FolderMock resource) {
        this.members.add(resource);
        resource.setParent(this);
    }

    private void setParent(FolderMock folderMock) {
        this.parent = folderMock;
    }

    public void setParent(ProjectMock projectMock) {
        this.parent = projectMock;
    }

    public void addMember(FileMock resource) {
        this.members.add(resource);
        resource.setParent(this);
    }

    @Override
    public IResource[] members() throws CoreException {
        return members.toArray(new IResource[members.size()]);
    }

    @Override
    public IContainer getParent() {
        return this.parent;
    }

    @Override
    public IProject getProject() {
        return this.parent.getProject();
    }

}
