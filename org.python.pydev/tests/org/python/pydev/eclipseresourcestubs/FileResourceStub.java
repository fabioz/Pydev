/*
 * Created on Dec 10, 2006
 * @author Fabio
 */
package org.python.pydev.eclipseresourcestubs;

import java.io.File;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.python.pydev.core.REF;

/**
 * A stub for a fil that implements the IFile interface required by Eclipse.
 * 
 * @author Fabio
 */
public class FileResourceStub implements IFile {

    private File actualFile;
    private IProject project;
    private String fileContents;

    public FileResourceStub(File file, IProject project) {
        this.actualFile = file;
        this.project = project;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof FileResourceStub)){
            return false;
        }
        FileResourceStub o = (FileResourceStub) obj;
        return this.actualFile.equals(o.actualFile);
    }
    
    /**
     * For testing purposes
     * @return
     */
    public String getFileContents(){
        if(this.fileContents == null){
            this.fileContents = REF.getFileContents(actualFile);
        }
        return this.fileContents;
    }
    
    @Override
    public int hashCode() {
        return actualFile.hashCode();
    }

    public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void appendContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void create(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
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

    public String getCharset() throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public String getCharset(boolean checkImplicit) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public String getCharsetFor(Reader reader) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public IContentDescription getContentDescription() throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public InputStream getContents() throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public InputStream getContents(boolean force) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public int getEncoding() throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public IPath getFullPath() {
        throw new RuntimeException("Not impl");
    }

    public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public String getName() {
        return this.actualFile.getName();
    }

    public boolean isReadOnly() {
        throw new RuntimeException("Not impl");
    }

    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void setCharset(String newCharset) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public void setContents(IFileState source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
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
        return actualFile.exists();
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

    public long getLocalTimeStamp() {
        throw new RuntimeException("Not impl");
    }

    public IPath getLocation() {
        throw new RuntimeException("Not impl");
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

    public IContainer getParent() {
        throw new RuntimeException("Not impl");
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public IProject getProject() {
        return project;
    }

    public IPath getProjectRelativePath() {
        throw new RuntimeException("Not impl");
    }

    public IPath getRawLocation() {
        return Path.fromOSString(REF.getFileAbsolutePath(actualFile));
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


}
