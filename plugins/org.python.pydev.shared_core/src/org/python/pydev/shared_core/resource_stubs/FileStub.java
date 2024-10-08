/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class FileStub extends AbstractIFileStub implements IFile {

    private IProject project;
    protected File file;

    public FileStub(IProject project, File file) {
        this.project = project;
        this.file = file;
    }

    @Override
    public boolean exists() {
        return file.isFile();
    }

    @Override
    public URI getLocationURI() {
        return file.toURI();
    }

    @Override
    public boolean isAccessible() {
        return file.exists();
    }

    @Override
    public IPath getLocation() {
        return Path.fromOSString(file.toString());
    }

    @Override
    public String getFileExtension() {
        String name = this.file.getName();
        List<String> dotSplit = StringUtils.dotSplit(name);
        if (dotSplit.size() > 1) {
            return dotSplit.get(dotSplit.size() - 1);
        }
        return null;
    }

    @Override
    public String getName() {
        return this.file.getName();
    }

    @Override
    public IContainer getParent() {
        return new FolderStub(project, file.getParentFile());
    }

    @Override
    public long getModificationStamp() {
        try {
            FileTime ret = Files.getLastModifiedTime(this.file.toPath());
            return ret.to(TimeUnit.NANOSECONDS);
        } catch (IOException e) {
            return -1; // File does not exist.
        }
    }

    @Override
    public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        try {
            FastStringBuffer buffer = FileUtils.fillBufferWithStream(source, "utf-8", monitor);
            FileUtils.writeStrToFile(buffer.toString(), file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getContents() throws CoreException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getContents(boolean force) throws CoreException {
        return getContents();
    }

    @Override
    public IPath getFullPath() {
        IPath projectPath = project.getLocation();
        IPath filePath = Path.fromOSString(FileUtils.getFileAbsolutePath(file));
        IPath relativeToProject = filePath.makeRelativeTo(projectPath);
        // Important: the full path is relative to the workspace, so, we need to
        // add the project there too.
        return new Path(this.project.getName()).append(relativeToProject);
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
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FileStub other = (FileStub) obj;
        if (file == null) {
            if (other.file != null) {
                return false;
            }
        } else if (!file.equals(other.file)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "FileStub:" + this.file;
    }

    @Override
    public IProject getProject() {
        return this.project;

    }

}
