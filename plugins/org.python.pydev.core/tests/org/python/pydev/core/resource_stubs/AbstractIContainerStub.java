package org.python.pydev.core.resource_stubs;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

//Only for 3.6 -- comment if you want to compile on earlier eclipse version
import org.eclipse.core.resources.IFileInfoMatcherDescription;
import org.eclipse.core.resources.IResourceFilterDescription;
//End Only for 3.6

public class AbstractIContainerStub extends AbstractIResourceStub implements IContainer{

    
    
    //Only for 3.6 -- comment if you want to compile on earlier eclipse version
    public IResourceFilterDescription createFilter(int type, IFileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void removeFilter(IResourceFilterDescription filterDescription, int updateFlags, IProgressMonitor monitor) throws CoreException {
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
        throw new RuntimeException("Not implemented");
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
