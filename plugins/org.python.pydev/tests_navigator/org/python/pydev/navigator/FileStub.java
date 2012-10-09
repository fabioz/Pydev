/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.resource_stubs.AbstractIFileStub;

public class FileStub extends AbstractIFileStub implements IFile {

    private ProjectStub project;
    private File file;

    public FileStub(ProjectStub project, File file) {
        Assert.isTrue(file.exists() && file.isFile());
        this.project = project;
        this.file = file;
    }

    @Override
    public String getName() {
        return this.file.getName();
    }

    public IContainer getParent() {
        return project.getFolder(this.file.getParentFile());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final FileStub other = (FileStub) obj;
        if (file == null) {
            if (other.file != null)
                return false;
        } else if (!file.equals(other.file))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FileStub:" + this.file;
    }

    public IProject getProject() {
        return this.project;

    }

}
