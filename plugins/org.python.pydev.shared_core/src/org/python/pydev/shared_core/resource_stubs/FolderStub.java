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
    private IProject project;
    private IContainer parent;

    public FolderStub(IProject stub, File parentFile) {
        this(stub, null, parentFile);
    }

    public FolderStub(IProject stub, IContainer parent, File parentFile) {
        this(stub, parent, parentFile, true);
    }

    public FolderStub(IProject stub, IContainer parent, File parentFile, boolean mustExist) {
        if (mustExist) {
            Assert.isTrue(parentFile.exists() && parentFile.isDirectory());
        }
        this.project = stub;
        this.folder = parentFile;
        this.parent = parent;
    }

    @Override
    public boolean isAccessible() {
        return this.folder.exists();
    }

    @Override
    public IContainer getParent() {
        if (parent != null) {
            return parent;
        }
        File parentFile = this.folder.getParentFile();
        if (parentFile == null) {
            return null;
        }
        if (Path.fromOSString(FileUtils.getFileAbsolutePath(parentFile)).equals(project.getLocation())) {
            return project;
        }

        return new FolderStub(this.project, parentFile);
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
        File f = this.folder;
        for (String s : segments) {
            f = new File(f, s);
        }
        // At this point it doesn't need to exist!
        boolean mustExist = false;
        return new FolderStub(project, null, f, mustExist);
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

        IPath fromOSString = Path.fromOSString(fileAbsolutePath);
        IPath project = this.project.getLocation();
        IPath relativeToProject = fromOSString.makeRelativeTo(project);

        // Important: the full path is relative to the workspace, so, we need to
        // add the project there too.
        return new Path(this.project.getName()).append(relativeToProject);

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
