/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class AbstractIWorkspaceRootStub extends AbstractIContainerStub implements IWorkspaceRoot {

    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IContainer[] findContainersForLocation(IPath location) {
        throw new RuntimeException("Not implemented");
    }

    public IContainer[] findContainersForLocationURI(URI location) {
        throw new RuntimeException("Not implemented");
    }

    public IContainer[] findContainersForLocationURI(URI location, int memberFlags) {
        throw new RuntimeException("Not implemented");
    }

    public IFile[] findFilesForLocation(IPath location) {
        throw new RuntimeException("Not implemented");
    }

    public IFile[] findFilesForLocationURI(URI location) {
        throw new RuntimeException("Not implemented");
    }

    public IFile[] findFilesForLocationURI(URI location, int memberFlags) {
        throw new RuntimeException("Not implemented");
    }

    public IContainer getContainerForLocation(IPath location) {
        throw new RuntimeException("Not implemented");
    }

    public IFile getFileForLocation(IPath location) {
        throw new RuntimeException("Not implemented");
    }

    public IProject getProject(String name) {
        throw new RuntimeException("Not implemented");
    }

    public IProject[] getProjects() {
        throw new RuntimeException("Not implemented");
    }

    public IProject[] getProjects(int memberFlags) {
        throw new RuntimeException("Not implemented");
    }

}
