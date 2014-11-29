/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.python.pydev.shared_core.io.FileUtils;

public class FolderStub extends AbstractIFolderStub implements IFolder {

    private File folder;
    private ProjectStub project;
    private IContainer parent;

    public FolderStub(ProjectStub stub, File parentFile) {
        this(stub, null, parentFile);
    }

    public FolderStub(ProjectStub stub, IContainer parent, File parentFile) {
        this(stub, parent, parentFile, true);
    }

    public FolderStub(ProjectStub stub, IContainer parent, File parentFile, boolean mustExist) {
        if (mustExist) {
            Assert.isTrue(parentFile.exists() && parentFile.isDirectory());
        }
        this.project = stub;
        this.folder = parentFile;
        this.parent = parent;
    }

    @Override
    public IContainer getParent() {
        if (parent != null) {
            return parent;
        }
        return project.getFolder(this.folder.getParentFile());
    }

    @Override
    public IFile getFile(IPath path) {
        if (path.segmentCount() != 1) {
            throw new RuntimeException("finish implementing");
        }
        return new FileStub(project, new File(folder, path.segment(0)));
    }

    @Override
    public IFile getFile(String name) {
        return getFile(new Path(name));
    }

    @Override
    public IFolder getFolder(IPath path) {
        String[] segments = path.segments();

        IFolder f = null;
        File curr = this.folder;
        for (String string : segments) {
            File parentFile = new File(curr, string);
            f = (IFolder) project.getFolder(parentFile);
            curr = parentFile;
        }
        return f;
    }

    @Override
    public String getName() {
        return this.folder.getName();
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FolderStub other = (FolderStub) obj;
        if (folder == null) {
            if (other.folder != null) {
                return false;
            }
        } else if (!folder.equals(other.folder)) {
            return false;
        }
        return true;
    }

    @Override
    public IPath getFullPath() {
        //        return Path.fromOSString(FileUtils.getFileAbsolutePath(this.folder));
        String fileAbsolutePath = FileUtils.getFileAbsolutePath(this.folder);
        String workspaceAbsolutePath = FileUtils.getFileAbsolutePath(this.project.projectRoot.getParentFile());

        IPath fromOSString = Path.fromOSString(fileAbsolutePath);
        IPath workspace = Path.fromOSString(workspaceAbsolutePath);
        return fromOSString.makeRelativeTo(workspace);
    }

    @Override
    public IPath getLocation() {
        return Path.fromOSString(FileUtils.getFileAbsolutePath(this.folder));
    }

    @Override
    public IProject getProject() {
        return this.project;

    }

}
