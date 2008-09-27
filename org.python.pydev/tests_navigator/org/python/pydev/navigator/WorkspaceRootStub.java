package org.python.pydev.navigator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class WorkspaceRootStub implements IWorkspaceRoot, IWorkbenchAdapter{

    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IContainer[] findContainersForLocation(IPath location) {
        throw new RuntimeException("Not implemented");
    }

    public IContainer[] findContainersForLocationURI(URI location) {
        throw new RuntimeException("Not implemented");
    }

    public IFile[] findFilesForLocation(IPath location) {
        throw new RuntimeException("Not implemented");
    }

    public IFile[] findFilesForLocationURI(URI location) {
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

    public boolean exists(IPath path) {
        throw new RuntimeException("Not implemented");
    }

    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IResource findMember(String name) {
        throw new RuntimeException("Not implemented");
    }

    public IResource findMember(IPath path) {
        throw new RuntimeException("Not implemented");
    }

    public IResource findMember(String name, boolean includePhantoms) {
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

    public void setDefaultCharset(String charset) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IMarker createMarker(String type) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IResourceProxy createProxy() {
        throw new RuntimeException("Not implemented");
    }

    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public boolean exists() {
        throw new RuntimeException("Not implemented");
    }

    public IMarker findMarker(long id) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public String getFileExtension() {
        throw new RuntimeException("Not implemented");
    }

    public IPath getFullPath() {
        throw new RuntimeException("Not implemented");
    }

    public long getLocalTimeStamp() {
        throw new RuntimeException("Not implemented");
    }

    public IPath getLocation() {
        throw new RuntimeException("Not implemented");
    }

    public URI getLocationURI() {
        throw new RuntimeException("Not implemented");
    }

    public IMarker getMarker(long id) {
        throw new RuntimeException("Not implemented");
    }

    public long getModificationStamp() {
        throw new RuntimeException("Not implemented");
    }

    public String getName() {
        throw new RuntimeException("Not implemented");
    }

    public IContainer getParent() {
        return null;
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IProject getProject() {
        return null;
    }

    public IPath getProjectRelativePath() {
        throw new RuntimeException("Not implemented");
    }

    public IPath getRawLocation() {
        throw new RuntimeException("Not implemented");
    }

    public URI getRawLocationURI() {
        throw new RuntimeException("Not implemented");
    }

    public ResourceAttributes getResourceAttributes() {
        throw new RuntimeException("Not implemented");
    }

    public Object getSessionProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public int getType() {
        throw new RuntimeException("Not implemented");
    }

    public IWorkspace getWorkspace() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isAccessible() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isDerived() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isLinked() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isLinked(int options) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isLocal(int depth) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isPhantom() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isReadOnly() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isSynchronized(int depth) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isTeamPrivateMember() {
        throw new RuntimeException("Not implemented");
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void revertModificationStamp(long value) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setDerived(boolean isDerived) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public long setLocalTimeStamp(long value) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setReadOnly(boolean readOnly) {
        throw new RuntimeException("Not implemented");
    }

    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void touch(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public Object getAdapter(Class adapter) {
        if(adapter == IWorkbenchAdapter.class){
            return this;
        }
        throw new RuntimeException("Not implemented for: "+adapter);
    }

    public boolean contains(ISchedulingRule rule) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isConflicting(ISchedulingRule rule) {
        throw new RuntimeException("Not implemented");
    }
    
    
    //IWorkbenchAdapter
    List<Object> children = new ArrayList<Object>();
    public void addChild(Object child){
        children.add(child);
    }

    public Object[] getChildren(Object o) {
        return children.toArray();
    }

    public ImageDescriptor getImageDescriptor(Object object) {
        throw new RuntimeException("Not implemented");
    }

    public String getLabel(Object o) {
        throw new RuntimeException("Not implemented");
    }

    public Object getParent(Object o) {
        throw new RuntimeException("Not implemented");
    }

    public IProject[] getProjects(int memberFlags) {
        // TODO Auto-generated method stub
        return null;
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

}
