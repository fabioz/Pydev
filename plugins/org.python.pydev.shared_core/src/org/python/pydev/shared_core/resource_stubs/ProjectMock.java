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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class ProjectMock extends AbstractIProjectStub {

    private IProjectNature nature;

    public void addMember(FolderMock mod1) {
        mod1.setParent(this);
    }

    public void setNature(IProjectNature pythonNatureStub) {
        this.nature = pythonNatureStub;
    }

    @Override
    public IProjectNature getNature(String natureId) throws CoreException {
        return this.nature;
    }

    @Override
    public IProject getProject() {
        return this;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

}
