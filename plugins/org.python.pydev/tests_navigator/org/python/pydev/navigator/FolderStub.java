package org.python.pydev.navigator;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileInfoMatcherDescription;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.python.pydev.core.REF;

public class FolderStub implements IFolder{

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
        if(parent != null){
            return parent;
        }
        return project.getFolder(this.folder.getParentFile());
    }
    
    @Override
    public String toString() {
        return "FolderStub:"+this.folder;
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

    public void create(boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void create(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IFile getFile(String name) {
        throw new RuntimeException("Not impl");
        
    }

    public IFolder getFolder(String name) {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public boolean exists(IPath path) {
        throw new RuntimeException("Not impl");
        
    }

    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IResource findMember(String name) {
        throw new RuntimeException("Not impl");
        
    }

    public IResource findMember(IPath path) {
        throw new RuntimeException("Not impl");
        
    }

    public IResource findMember(String name, boolean includePhantoms) {
        throw new RuntimeException("Not impl");
        
    }

    public IResource findMember(IPath path, boolean includePhantoms) {
        throw new RuntimeException("Not impl");
        
    }

    public String getDefaultCharset() throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IFile getFile(IPath path) {
        throw new RuntimeException("Not impl");
        
    }

    public IFolder getFolder(IPath path) {
        throw new RuntimeException("Not impl");
        
    }

    public IResource[] members() throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IResource[] members(boolean includePhantoms) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IResource[] members(int memberFlags) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setDefaultCharset(String charset) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IMarker createMarker(String type) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IResourceProxy createProxy() {
        throw new RuntimeException("Not impl");
        
    }

    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public boolean exists() {
        throw new RuntimeException("Not impl");
        
    }

    public IMarker findMarker(long id) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public String getFileExtension() {
        throw new RuntimeException("Not impl");
        
    }

    public IPath getFullPath() {
        return Path.fromOSString(REF.getFileAbsolutePath(this.folder));
    }

    public long getLocalTimeStamp() {
        throw new RuntimeException("Not impl");
        
    }

    public IPath getLocation() {
        return Path.fromOSString(REF.getFileAbsolutePath(this.folder));
    }

    public URI getLocationURI() {
        throw new RuntimeException("Not impl");
        
    }

    public IMarker getMarker(long id) {
        throw new RuntimeException("Not impl");
        
    }

    public long getModificationStamp() {
        throw new RuntimeException("Not impl");
        
    }

    public String getName() {
        throw new RuntimeException("Not impl");
        
    }


    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public IProject getProject() {
        return this.project;
        
    }

    public IPath getProjectRelativePath() {
        throw new RuntimeException("Not impl");
        
    }

    public IPath getRawLocation() {
        throw new RuntimeException("Not impl");
        
    }

    public URI getRawLocationURI() {
        throw new RuntimeException("Not impl");
        
    }

    public ResourceAttributes getResourceAttributes() {
        throw new RuntimeException("Not impl");
        
    }

    public Object getSessionProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public int getType() {
        throw new RuntimeException("Not impl");
        
    }

    public IWorkspace getWorkspace() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isAccessible() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isDerived() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isLinked() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isLinked(int options) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isLocal(int depth) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isPhantom() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isReadOnly() {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isSynchronized(int depth) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isTeamPrivateMember() {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void revertModificationStamp(long value) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setDerived(boolean isDerived) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public long setLocalTimeStamp(long value) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setReadOnly(boolean readOnly) {
        throw new RuntimeException("Not impl");
        
    }

    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public void touch(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
        
    }

    public Object getAdapter(Class adapter) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean contains(ISchedulingRule rule) {
        throw new RuntimeException("Not impl");
        
    }

    public boolean isConflicting(ISchedulingRule rule) {
        throw new RuntimeException("Not impl");
        
    }

    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public Map getPersistentProperties() throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getSessionProperties() throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isDerived(int options) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isHidden() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setHidden(boolean isHidden) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    public boolean isHidden(int options) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isTeamPrivateMember(int options) {
        throw new RuntimeException("Not implemented");
    }

    public IResourceFilterDescription createFilter(int type, IFileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void removeFilter(IResourceFilterDescription filterDescription, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IResourceFilterDescription[] getFilters() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public boolean isGroup() {
        throw new RuntimeException("Not implemented");
    }

    public boolean hasFilters() {
        throw new RuntimeException("Not implemented");
    }

    public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void createGroup(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

}
