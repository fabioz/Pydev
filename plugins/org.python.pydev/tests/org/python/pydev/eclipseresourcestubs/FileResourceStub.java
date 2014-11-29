/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 10, 2006
 * @author Fabio
 */
package org.python.pydev.eclipseresourcestubs;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.resource_stubs.AbstractIFileStub;

/**
 * A stub for a file that implements the IFile interface required by Eclipse.
 * 
 * @author Fabio
 */
public class FileResourceStub extends AbstractIFileStub implements IFile {

    private File actualFile;
    private IProject project;
    private String fileContents;

    public FileResourceStub(File file, IProject project) {
        this.actualFile = file;
        this.project = project;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FileResourceStub)) {
            return false;
        }
        FileResourceStub o = (FileResourceStub) obj;
        return this.actualFile.equals(o.actualFile);
    }

    /**
     * For testing purposes
     * @return
     */
    public String getFileContents() {
        if (this.fileContents == null) {
            this.fileContents = FileUtils.getFileContents(actualFile);
        }
        return this.fileContents;
    }

    @Override
    public int hashCode() {
        return actualFile.hashCode();
    }

    public String getName() {
        return this.actualFile.getName();
    }

    public boolean exists() {
        return actualFile.exists();
    }

    public IProject getProject() {
        return project;
    }

    public IPath getRawLocation() {
        return Path.fromOSString(FileUtils.getFileAbsolutePath(actualFile));
    }

}
