package org.python.pydev.navigator;

import java.net.URI;

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
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class PythonFolder extends WrappedResource<IFolder> implements IFolder{

    public PythonFolder(Object parentElement, IFolder folder, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, folder, pythonSourceFolder, IWrappedResource.RANK_PYTHON_FOLDER);
        //System.out.println("Created PythonFolder:"+this+" - "+actualObject+" parent:"+parentElement);
    }
    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        actualObject.accept(visitor, memberFlags);
    }
    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        actualObject.accept(visitor, depth, includePhantoms);
    }
    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        actualObject.accept(visitor, depth, memberFlags);
    }
    public void accept(IResourceVisitor visitor) throws CoreException {
        actualObject.accept(visitor);
    }
    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        actualObject.clearHistory(monitor);
    }
    public boolean contains(ISchedulingRule rule) {
        return actualObject.contains(rule);
    }
    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        actualObject.copy(destination, force, monitor);
    }
    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.copy(destination, updateFlags, monitor);
    }
    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        actualObject.copy(description, force, monitor);
    }
    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.copy(description, updateFlags, monitor);
    }
    public void create(boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
        actualObject.create(force, local, monitor);
    }
    public void create(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
        actualObject.create(updateFlags, local, monitor);
    }
    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.createLink(localLocation, updateFlags, monitor);
    }
    public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.createLink(location, updateFlags, monitor);
    }
    public IMarker createMarker(String type) throws CoreException {
        return actualObject.createMarker(type);
    }
    public IResourceProxy createProxy() {
        return actualObject.createProxy();
    }
    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        actualObject.delete(force, keepHistory, monitor);
    }
    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        actualObject.delete(force, monitor);
    }
    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.delete(updateFlags, monitor);
    }
    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        actualObject.deleteMarkers(type, includeSubtypes, depth);
    }
    public boolean exists() {
        return actualObject.exists();
    }
    public boolean exists(IPath path) {
        return actualObject.exists(path);
    }
    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        return actualObject.findDeletedMembersWithHistory(depth, monitor);
    }
    public IMarker findMarker(long id) throws CoreException {
        return actualObject.findMarker(id);
    }
    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        return actualObject.findMarkers(type, includeSubtypes, depth);
    }
    public IResource findMember(IPath path, boolean includePhantoms) {
        return actualObject.findMember(path, includePhantoms);
    }
    public IResource findMember(IPath path) {
        return actualObject.findMember(path);
    }
    public IResource findMember(String name, boolean includePhantoms) {
        return actualObject.findMember(name, includePhantoms);
    }
    public IResource findMember(String name) {
        return actualObject.findMember(name);
    }
    public Object getAdapter(Class adapter) {
        return actualObject.getAdapter(adapter);
    }
    public String getDefaultCharset() throws CoreException {
        return actualObject.getDefaultCharset();
    }
    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
        return actualObject.getDefaultCharset(checkImplicit);
    }
    public IFile getFile(IPath path) {
        return actualObject.getFile(path);
    }
    public IFile getFile(String name) {
        return actualObject.getFile(name);
    }
    public String getFileExtension() {
        return actualObject.getFileExtension();
    }
    public IFolder getFolder(IPath path) {
        return actualObject.getFolder(path);
    }
    public IFolder getFolder(String name) {
        return actualObject.getFolder(name);
    }
    public IPath getFullPath() {
        return actualObject.getFullPath();
    }
    public long getLocalTimeStamp() {
        return actualObject.getLocalTimeStamp();
    }
    public IPath getLocation() {
        return actualObject.getLocation();
    }
    public URI getLocationURI() {
        return actualObject.getLocationURI();
    }
    public IMarker getMarker(long id) {
        return actualObject.getMarker(id);
    }
    public long getModificationStamp() {
        return actualObject.getModificationStamp();
    }
    public String getName() {
        return actualObject.getName();
    }
    public IContainer getParent() {
        return actualObject.getParent();
    }
    public String getPersistentProperty(QualifiedName key) throws CoreException {
        return actualObject.getPersistentProperty(key);
    }
    public IProject getProject() {
        return actualObject.getProject();
    }
    public IPath getProjectRelativePath() {
        return actualObject.getProjectRelativePath();
    }
    public IPath getRawLocation() {
        return actualObject.getRawLocation();
    }
    public URI getRawLocationURI() {
        return actualObject.getRawLocationURI();
    }
    public ResourceAttributes getResourceAttributes() {
        return actualObject.getResourceAttributes();
    }
    public Object getSessionProperty(QualifiedName key) throws CoreException {
        return actualObject.getSessionProperty(key);
    }
    public int getType() {
        return actualObject.getType();
    }
    public IWorkspace getWorkspace() {
        return actualObject.getWorkspace();
    }
    public boolean isAccessible() {
        return actualObject.isAccessible();
    }
    public boolean isConflicting(ISchedulingRule rule) {
        return actualObject.isConflicting(rule);
    }
    public boolean isDerived() {
        return actualObject.isDerived();
    }
    public boolean isLinked() {
        return actualObject.isLinked();
    }
    public boolean isLinked(int options) {
        return actualObject.isLinked(options);
    }
    public boolean isLocal(int depth) {
        return actualObject.isLocal(depth);
    }
    public boolean isPhantom() {
        return actualObject.isPhantom();
    }
    public boolean isReadOnly() {
        return actualObject.isReadOnly();
    }
    public boolean isSynchronized(int depth) {
        return actualObject.isSynchronized(depth);
    }
    public boolean isTeamPrivateMember() {
        return actualObject.isTeamPrivateMember();
    }
    public IResource[] members() throws CoreException {
        return actualObject.members();
    }
    public IResource[] members(boolean includePhantoms) throws CoreException {
        return actualObject.members(includePhantoms);
    }
    public IResource[] members(int memberFlags) throws CoreException {
        return actualObject.members(memberFlags);
    }
    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        actualObject.move(destination, force, keepHistory, monitor);
    }
    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        actualObject.move(destination, force, monitor);
    }
    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.move(destination, updateFlags, monitor);
    }
    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        actualObject.move(description, force, keepHistory, monitor);
    }
    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.move(description, updateFlags, monitor);
    }
    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        actualObject.refreshLocal(depth, monitor);
    }
    public void revertModificationStamp(long value) throws CoreException {
        actualObject.revertModificationStamp(value);
    }
    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        actualObject.setDefaultCharset(charset, monitor);
    }
    public void setDefaultCharset(String charset) throws CoreException {
        actualObject.setDefaultCharset(charset);
    }
    public void setDerived(boolean isDerived) throws CoreException {
        actualObject.setDerived(isDerived);
    }
    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        actualObject.setLocal(flag, depth, monitor);
    }
    public long setLocalTimeStamp(long value) throws CoreException {
        return actualObject.setLocalTimeStamp(value);
    }
    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        actualObject.setPersistentProperty(key, value);
    }
    public void setReadOnly(boolean readOnly) {
        actualObject.setReadOnly(readOnly);
    }
    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        actualObject.setResourceAttributes(attributes);
    }
    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        actualObject.setSessionProperty(key, value);
    }
    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        actualObject.setTeamPrivateMember(isTeamPrivate);
    }
    public void touch(IProgressMonitor monitor) throws CoreException {
        actualObject.touch(monitor);
    }
}
