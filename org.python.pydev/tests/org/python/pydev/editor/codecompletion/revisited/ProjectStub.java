/*
 * Created on 24/09/2005
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.python.pydev.plugin.nature.PythonNature;

public class ProjectStub implements IProject {

    String name;
	public IProject[] referencedProjects;
	public IProject[] referencingProjects;
	private PythonNature nature;
	private String path;
    
    public ProjectStub(String name, String path2, IProject[] referencedProjects, IProject[] referencingProjects) {
    	this.path = path2;
    	this.name = name;
    	this.referencedProjects = referencedProjects;
        this.referencingProjects = referencingProjects;
    }

    public void build(int kind, String builderName, Map args, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void build(int kind, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void close(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void create(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IProjectDescription getDescription() throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IFile getFile(String name) {
        throw new RuntimeException("not impl");
    }

    public IFolder getFolder(String name) {
        throw new RuntimeException("not impl");
    }

    public IProjectNature getNature(String natureId) throws CoreException {
    	if(nature == null){
    		throw new RuntimeException("not expected");
    	}
    	return nature;
    }

    public IPath getPluginWorkingLocation(IPluginDescriptor plugin) {
        throw new RuntimeException("not impl");
    }

    public IPath getWorkingLocation(String id) {
        return new Path(path);
    }

    public IProject[] getReferencedProjects() throws CoreException {
        //no referenced projects
        return referencedProjects;
    }

    public IProject[] getReferencingProjects() {
        return referencingProjects;
    }

    public boolean hasNature(String natureId) throws CoreException {
    	if(PythonNature.PYTHON_NATURE_ID.equals(natureId)){
    		return true;
    	}
        throw new RuntimeException("not expected");
    }

    public boolean isNatureEnabled(String natureId) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public boolean isOpen() {
        throw new RuntimeException("not impl");
    }

    public void move(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
    }

    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void open(IProgressMonitor monitor) throws CoreException {
    }

    public void setDescription(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
    }

    public void setDescription(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public boolean exists(IPath path) {
        throw new RuntimeException("not impl");
    }

    public IResource findMember(String name) {
        throw new RuntimeException("not impl");
    }

    public IResource findMember(String name, boolean includePhantoms) {
        throw new RuntimeException("not impl");
    }

    public IResource findMember(IPath path) {
        throw new RuntimeException("not impl");
    }

    public IResource findMember(IPath path, boolean includePhantoms) {
        throw new RuntimeException("not impl");
    }

    public String getDefaultCharset() throws CoreException {
        throw new RuntimeException("not impl");
    }

    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IFile getFile(IPath path) {
        throw new RuntimeException("not impl");
    }

    public IFolder getFolder(IPath path) {
        throw new RuntimeException("not impl");
    }

    public IResource[] members() throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IResource[] members(boolean includePhantoms) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IResource[] members(int memberFlags) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void setDefaultCharset(String charset) throws CoreException {
    }

    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
    }

    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
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
        throw new RuntimeException("not impl");
    }

    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
    }

    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
    }

    public boolean exists() {
        throw new RuntimeException("not impl");
    }

    public IMarker findMarker(long id) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public String getFileExtension() {
        throw new RuntimeException("not impl");
    }

    public IPath getFullPath() {
        throw new RuntimeException("not impl");
    }

    public long getLocalTimeStamp() {
        throw new RuntimeException("not impl");
    }

    public IPath getLocation() {
        throw new RuntimeException("not impl");
    }

    public IMarker getMarker(long id) {
        throw new RuntimeException("not impl");
    }

    public long getModificationStamp() {
        throw new RuntimeException("not impl");
    }

    public String getName() {
        return name;
    }

    public IContainer getParent() {
        throw new RuntimeException("not impl");
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public IProject getProject() {
        throw new RuntimeException("not impl");
    }

    public IPath getProjectRelativePath() {
        throw new RuntimeException("not impl");
    }

    public IPath getRawLocation() {
        throw new RuntimeException("not impl");
    }

    public ResourceAttributes getResourceAttributes() {
        throw new RuntimeException("not impl");
    }

    public Object getSessionProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public int getType() {
        throw new RuntimeException("not impl");
    }

    public IWorkspace getWorkspace() {
        throw new RuntimeException("not impl");
    }

    public boolean isAccessible() {
        throw new RuntimeException("not impl");
    }

    public boolean isDerived() {
        throw new RuntimeException("not impl");
    }

    public boolean isLocal(int depth) {
        throw new RuntimeException("not impl");
    }

    public boolean isLinked() {
        throw new RuntimeException("not impl");
    }

    public boolean isPhantom() {
        throw new RuntimeException("not impl");
    }

    public boolean isReadOnly() {
        throw new RuntimeException("not impl");
    }

    public boolean isSynchronized(int depth) {
        throw new RuntimeException("not impl");
    }

    public boolean isTeamPrivateMember() {
        throw new RuntimeException("not impl");
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void revertModificationStamp(long value) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void setDerived(boolean isDerived) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public long setLocalTimeStamp(long value) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void setReadOnly(boolean readOnly) {
        throw new RuntimeException("not impl");
    }

    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public void touch(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("not impl");
    }

    public Object getAdapter(Class adapter) {
        throw new RuntimeException("not impl");
    }

    public boolean contains(ISchedulingRule rule) {
        throw new RuntimeException("not impl");
    }

    public boolean isConflicting(ISchedulingRule rule) {
        throw new RuntimeException("not impl");
    }

	public void setNature(PythonNature nature) {
		this.nature = nature;
	}

	public IResourceProxy createProxy() {
		return null;
	}

	public URI getLocationURI() {
		return null;
	}

	public URI getRawLocationURI() {
		return null;
	}

	public boolean isLinked(int options) {
		return false;
	}

}
