/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package org.python.pydev.plugin.nature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.shared_core.resource_stubs.AbstractIProjectStub;

public class ProjectStub2 extends AbstractIProjectStub implements IProject {

    public FileStub2 fileStub;
    private String name;

    public ProjectStub2(String name) {
        this.name = name;
    }

    public IFile getFile(String name) {
        fileStub = new FileStub2(name);
        return fileStub;
    }

    public boolean hasNature(String natureId) throws CoreException {
        if (PythonNature.PYTHON_NATURE_ID.equals(natureId)) {
            return true;
        }
        throw new RuntimeException("not expected");
    }

    public boolean isOpen() {
        return true;
    }

    public String getName() {
        return this.name;
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        if (key.getLocalName().equals("PYTHON_PROJECT_VERSION")) {
            // TODO the comment below says "always the latests", but it isn't!
            return IPythonNature.PYTHON_VERSION_2_5;//for tests, always the latest version
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

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        if (value == null) {
            return;
        }
        throw new RuntimeException("not expected");
    }

}
