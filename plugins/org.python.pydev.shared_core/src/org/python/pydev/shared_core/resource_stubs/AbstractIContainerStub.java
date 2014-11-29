/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
//Only for 3.6 -- comment if you want to compile on earlier eclipse version
import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

//End Only for 3.6

public class AbstractIContainerStub extends AbstractIResourceStub implements IContainer {

    //Only for 3.6 -- comment if you want to compile on earlier eclipse version
    public IResourceFilterDescription createFilter(int type, FileInfoMatcherDescription matcherDescription,
            int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IResourceFilterDescription[] getFilters() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    //End Only for 3.6

    public boolean exists(IPath path) {
        throw new RuntimeException("Not implemented");
    }

    public IResource findMember(String name) {
        throw new RuntimeException("Not implemented");
    }

    public IResource findMember(String name, boolean includePhantoms) {
        throw new RuntimeException("Not implemented");
    }

    public IResource findMember(IPath path) {
        throw new RuntimeException("Not implemented");
    }

    public IResource findMember(IPath path, boolean includePhantoms) {
        throw new RuntimeException("Not implemented");
    }

    public String getDefaultCharset() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IFile getFile(IPath path) {
        throw new RuntimeException("Not implemented");
    }

    public IFolder getFolder(IPath path) {
        throw new RuntimeException("Not implemented in: " + this.getClass());
    }

    public IResource[] members() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IResource[] members(boolean includePhantoms) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IResource[] members(int memberFlags) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setDefaultCharset(String charset) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

}
