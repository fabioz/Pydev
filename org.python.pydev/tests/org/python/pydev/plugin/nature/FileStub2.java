/*
 * Created on Oct 28, 2006
 * @author Fabio
 */
package org.python.pydev.plugin.nature;

import java.io.IOException;
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

public class FileStub2 implements IFile {

    private String name;
    private String contents;
    public boolean created;

    public FileStub2(String name) {
        this.name = name;
    }

    public String getStrContents() {
        return contents;
    }
    
    public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void appendContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
    }

    public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
        created = true;
        try {
            int i = source.available();
            byte[] bs = new byte[i];
            source.read(bs);
            this.contents = new String(bs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void create(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
    }

    public String getCharset() throws CoreException {
        return null;
    }

    public String getCharset(boolean checkImplicit) throws CoreException {
        return null;
    }

    public String getCharsetFor(Reader reader) throws CoreException {
        return null;
    }

    public IContentDescription getContentDescription() throws CoreException {
        return null;
    }

    public InputStream getContents() throws CoreException {
        return null;
    }

    public InputStream getContents(boolean force) throws CoreException {
        return null;
    }

    public int getEncoding() throws CoreException {
        return 0;
    }

    public IPath getFullPath() {
        return null;
    }

    public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
        return null;
    }

    public String getName() {
        return null;
    }

    public boolean isReadOnly() {
        return false;
    }

    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
    }

    public void setCharset(String newCharset) throws CoreException {
    }

    public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
    }

    public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        try {
            int i = source.available();
            byte[] bs = new byte[i];
            source.read(bs);
            this.contents = new String(bs);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setContents(IFileState source, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
    }

    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
    }

    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
    }

    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
    }

    public void clearHistory(IProgressMonitor monitor) throws CoreException {
    }

    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
    }

    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
    }

    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public IMarker createMarker(String type) throws CoreException {
        return null;
    }

    public IResourceProxy createProxy() {
        return null;
    }

    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
    }

    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
    }

    public boolean exists() {
        return created;
    }

    public IMarker findMarker(long id) throws CoreException {
        return null;
    }

    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        return null;
    }

    public String getFileExtension() {
        return null;
    }

    public long getLocalTimeStamp() {
        return 0;
    }

    public IPath getLocation() {
        return null;
    }

    public URI getLocationURI() {
        return null;
    }

    public IMarker getMarker(long id) {
        return null;
    }

    public long getModificationStamp() {
        return 0;
    }

    public IContainer getParent() {
        return null;
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        return null;
    }

    public IProject getProject() {
        return null;
    }

    public IPath getProjectRelativePath() {
        return null;
    }

    public IPath getRawLocation() {
        return null;
    }

    public URI getRawLocationURI() {
        return null;
    }

    public ResourceAttributes getResourceAttributes() {
        return null;
    }

    public Object getSessionProperty(QualifiedName key) throws CoreException {
        return null;
    }

    public int getType() {
        return 0;
    }

    public IWorkspace getWorkspace() {
        return null;
    }

    public boolean isAccessible() {
        return false;
    }

    public boolean isDerived() {
        return false;
    }

    public boolean isLinked() {
        return false;
    }

    public boolean isLinked(int options) {
        return false;
    }

    public boolean isLocal(int depth) {
        return false;
    }

    public boolean isPhantom() {
        return false;
    }

    public boolean isSynchronized(int depth) {
        return false;
    }

    public boolean isTeamPrivateMember() {
        return false;
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
    }

    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
    }

    public void revertModificationStamp(long value) throws CoreException {
    }

    public void setDerived(boolean isDerived) throws CoreException {
    }

    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
    }

    public long setLocalTimeStamp(long value) throws CoreException {
        return 0;
    }

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
    }

    public void setReadOnly(boolean readOnly) {
    }

    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
    }

    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
    }

    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
    }

    public void touch(IProgressMonitor monitor) throws CoreException {
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

    public boolean contains(ISchedulingRule rule) {
        return false;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        return false;
    }

    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not implemented");
    }


}
