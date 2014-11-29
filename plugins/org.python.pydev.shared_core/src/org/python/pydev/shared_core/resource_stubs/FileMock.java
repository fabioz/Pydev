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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;

public class FileMock extends AbstractIFileStub {

    private String name;
    private FolderMock parent;

    public FileMock(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setParent(FolderMock folderMock) {
        this.parent = folderMock;

    }

    @Override
    public IProject getProject() {
        return this.parent.getProject();
    }

    @Override
    public IContainer getParent() {
        return this.parent;
    }

}
