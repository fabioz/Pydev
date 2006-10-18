package org.python.pydev.navigator;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Note that the python file here does not actually mean a .py or .pyw file (it can be
 * any file, such as .txt, .gif, etc)
 * 
 * @author fabioz
 */
public class PythonFile extends WrappedResource<IFile> implements IFile{
    
	public PythonFile(Object parentElement, IFile actualObject, PythonSourceFolder pythonSourceFolder) {
		super(parentElement, actualObject, pythonSourceFolder, IWrappedResource.RANK_PYTHON_FILE);
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
    public void appendContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        actualObject.appendContents(source, force, keepHistory, monitor);
    }
    public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.appendContents(source, updateFlags, monitor);
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
    public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
        actualObject.create(source, force, monitor);
    }
    public void create(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.create(source, updateFlags, monitor);
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
    public boolean equals(Object other) {
        return actualObject.equals(other);
    }
    public boolean exists() {
        return actualObject.exists();
    }
    public IMarker findMarker(long id) throws CoreException {
        return actualObject.findMarker(id);
    }
    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        return actualObject.findMarkers(type, includeSubtypes, depth);
    }
    public Object getAdapter(Class adapter) {
        return actualObject.getAdapter(adapter);
    }
    public String getCharset() throws CoreException {
        return actualObject.getCharset();
    }
    public String getCharset(boolean checkImplicit) throws CoreException {
        return actualObject.getCharset(checkImplicit);
    }
    public String getCharsetFor(Reader reader) throws CoreException {
        return actualObject.getCharsetFor(reader);
    }
    public IContentDescription getContentDescription() throws CoreException {
        return actualObject.getContentDescription();
    }
    public InputStream getContents() throws CoreException {
        return actualObject.getContents();
    }
    public InputStream getContents(boolean force) throws CoreException {
        return actualObject.getContents(force);
    }
    public int getEncoding() throws CoreException {
        return actualObject.getEncoding();
    }
    public String getFileExtension() {
        return actualObject.getFileExtension();
    }
    public IPath getFullPath() {
        return actualObject.getFullPath();
    }
    public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
        return actualObject.getHistory(monitor);
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
    public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
        actualObject.setCharset(newCharset, monitor);
    }
    public void setCharset(String newCharset) throws CoreException {
        actualObject.setCharset(newCharset);
    }
    public void setContents(IFileState source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        actualObject.setContents(source, force, keepHistory, monitor);
    }
    public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.setContents(source, updateFlags, monitor);
    }
    public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        actualObject.setContents(source, force, keepHistory, monitor);
    }
    public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        actualObject.setContents(source, updateFlags, monitor);
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
