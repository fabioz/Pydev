/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.REF;
import org.python.pydev.core.resource_stubs.AbstractIFolderStub;

public class FolderStub extends AbstractIFolderStub implements IFolder {

    private File folder;
    private ProjectStub project;
    private IContainer parent;

    public FolderStub(ProjectStub stub, File parentFile) {
        this(stub, null, parentFile);
    }

    public FolderStub(ProjectStub stub, IContainer parent, File parentFile) {
        Assert.isTrue(parentFile.exists() && parentFile.isDirectory());
        this.project = stub;
        this.folder = parentFile;
        this.parent = parent;
    }

    public IContainer getParent() {
        if (parent != null) {
            return parent;
        }
        return project.getFolder(this.folder.getParentFile());
    }

    @Override
    public String toString() {
        return "FolderStub:" + this.folder;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((folder == null) ? 0 : folder.hashCode());
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
        final FolderStub other = (FolderStub) obj;
        if (folder == null) {
            if (other.folder != null)
                return false;
        } else if (!folder.equals(other.folder))
            return false;
        return true;
    }

    public IPath getFullPath() {
        return Path.fromOSString(REF.getFileAbsolutePath(this.folder));
    }

    public IPath getLocation() {
        return Path.fromOSString(REF.getFileAbsolutePath(this.folder));
    }

    public IProject getProject() {
        return this.project;

    }

}
