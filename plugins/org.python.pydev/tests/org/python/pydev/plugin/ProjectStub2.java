/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package org.python.pydev.plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.resource_stubs.AbstractIProjectStub;

public class ProjectStub2 extends AbstractIProjectStub implements IProject {

    public FileStub2 fileStub;
    private String name;

    public ProjectStub2(String name) {
        this.name = name;
    }

    @Override
    public IFile getFile(String name) {
        fileStub = new FileStub2(name);
        return fileStub;
    }

    @Override
    public boolean hasNature(String natureId) throws CoreException {
        if (PythonNature.PYTHON_NATURE_ID.equals(natureId)) {
            return true;
        }
        throw new RuntimeException("not expected");
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getPersistentProperty(QualifiedName key) throws CoreException {
        if (key.getLocalName().equals("PYTHON_PROJECT_VERSION")) {
            return IPythonNature.PYTHON_VERSION_3_8;
        }
        //this is just for backward-compatibility
        if (key.getLocalName().equals("PROJECT_SOURCE_PATH")) {
            return "/test";
        }
        if (key.getLocalName().equals("PROJECT_EXTERNAL_SOURCE_PATH")) {
            return "";
        }
        throw new RuntimeException(key.getLocalName());
    }

    @Override
    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        if (value == null) {
            return;
        }
        throw new RuntimeException("not expected");
    }

}
