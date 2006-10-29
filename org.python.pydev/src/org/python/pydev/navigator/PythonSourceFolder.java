/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.ui.IContributorResourceAdapter;

/**
 * @author Fabio
 */
public class PythonSourceFolder implements IWrappedResource, IAdaptable, IFolder{

    public IFolder folder;
    public Object parentElement;
    public Map<Object, IWrappedResource> children = new HashMap<Object, IWrappedResource>();
    public Map<IContainer, List<IResource>> childrenForContainer = new HashMap<IContainer, List<IResource>>();

    public PythonSourceFolder(Object parentElement, IFolder folder) {
        this.parentElement = parentElement;
        this.folder = folder;
        //System.out.println("Created PythonSourceFolder:"+this+" - "+folder+" parent:"+parentElement);
    }

	public Object getParentElement() {
		return parentElement;
	}

	public Object getActualObject() {
		return folder;
	}

	public PythonSourceFolder getSourceFolder() {
		return this;
	}
	
	public void addChild(IResource actualObject, IWrappedResource child){
	    //System.out.println("Adding child:"+child+" for resource:"+actualObject);
		children.put(actualObject, child);
        
        IContainer container = actualObject.getParent();
        List<IResource> l = childrenForContainer.get(container);
        if(l == null){
            l = new ArrayList<IResource>();
            childrenForContainer.put(container, l);
        }
        l.add(actualObject);
	}
	
	public void removeChild(IResource actualObject){
	    //System.out.println("Removing child:"+actualObject);
        children.remove(actualObject);
        if(actualObject instanceof IContainer){
            List<IResource> l = childrenForContainer.get(actualObject);
            if(l != null){
                for (IResource resource : l) {
                    removeChild(resource);
                }
                childrenForContainer.remove(actualObject);
            }
        }
	}
	
	public Object getChild(IResource actualObject){
		IWrappedResource ret = children.get(actualObject);
		//System.out.println("Gotten child:"+ret+" for resource:"+actualObject);
        return ret;
	}
    
    
	
	public int getRank() {
	    return IWrappedResource.RANK_SOURCE_FOLDER;
	}
    

    public IResource getAdaptedResource(IAdaptable adaptable) {
        return (IResource) getActualObject();
    }

    public Object getAdapter(Class adapter) {
        if(adapter == IContributorResourceAdapter.class){
            return this;
        }
        Object ret = ((IResource)this.getActualObject()).getAdapter(adapter);
        return ret;
    }

    public boolean equals(Object other) {
        if(other instanceof PythonSourceFolder){
            return this == other;
        }
        return folder.equals(other);
    }
    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        folder.accept(visitor, memberFlags);
    }

    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        folder.accept(visitor, depth, includePhantoms);
    }

    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        folder.accept(visitor, depth, memberFlags);
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
        folder.accept(visitor);
    }

    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        folder.clearHistory(monitor);
    }

    public boolean contains(ISchedulingRule rule) {
        return folder.contains(rule);
    }

    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        folder.copy(destination, force, monitor);
    }

    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        folder.copy(destination, updateFlags, monitor);
    }

    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        folder.copy(description, force, monitor);
    }

    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        folder.copy(description, updateFlags, monitor);
    }

    public void create(boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
        folder.create(force, local, monitor);
    }

    public void create(int updateFlags, boolean local, IProgressMonitor monitor) throws CoreException {
        folder.create(updateFlags, local, monitor);
    }

    public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
        folder.createLink(localLocation, updateFlags, monitor);
    }

    public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
        folder.createLink(location, updateFlags, monitor);
    }

    public IMarker createMarker(String type) throws CoreException {
        return folder.createMarker(type);
    }

    public IResourceProxy createProxy() {
        return folder.createProxy();
    }

    public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        folder.delete(force, keepHistory, monitor);
    }

    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        folder.delete(force, monitor);
    }

    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        folder.delete(updateFlags, monitor);
    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        folder.deleteMarkers(type, includeSubtypes, depth);
    }


    public boolean exists() {
        return folder.exists();
    }

    public boolean exists(IPath path) {
        return folder.exists(path);
    }

    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor) throws CoreException {
        return folder.findDeletedMembersWithHistory(depth, monitor);
    }

    public IMarker findMarker(long id) throws CoreException {
        return folder.findMarker(id);
    }

    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        return folder.findMarkers(type, includeSubtypes, depth);
    }

    public IResource findMember(IPath path, boolean includePhantoms) {
        return folder.findMember(path, includePhantoms);
    }

    public IResource findMember(IPath path) {
        return folder.findMember(path);
    }

    public IResource findMember(String name, boolean includePhantoms) {
        return folder.findMember(name, includePhantoms);
    }

    public IResource findMember(String name) {
        return folder.findMember(name);
    }

    public String getDefaultCharset() throws CoreException {
        return folder.getDefaultCharset();
    }

    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
        return folder.getDefaultCharset(checkImplicit);
    }

    public IFile getFile(IPath path) {
        return folder.getFile(path);
    }

    public IFile getFile(String name) {
        return folder.getFile(name);
    }

    public String getFileExtension() {
        return folder.getFileExtension();
    }

    public IFolder getFolder(IPath path) {
        return folder.getFolder(path);
    }

    public IFolder getFolder(String name) {
        return folder.getFolder(name);
    }

    public IPath getFullPath() {
        return folder.getFullPath();
    }

    public long getLocalTimeStamp() {
        return folder.getLocalTimeStamp();
    }

    public IPath getLocation() {
        return folder.getLocation();
    }

    public URI getLocationURI() {
        return folder.getLocationURI();
    }

    public IMarker getMarker(long id) {
        return folder.getMarker(id);
    }

    public long getModificationStamp() {
        return folder.getModificationStamp();
    }

    public String getName() {
        return folder.getName();
    }

    public IContainer getParent() {
        return folder.getParent();
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        return folder.getPersistentProperty(key);
    }

    public IProject getProject() {
        return folder.getProject();
    }

    public IPath getProjectRelativePath() {
        return folder.getProjectRelativePath();
    }

    public IPath getRawLocation() {
        return folder.getRawLocation();
    }

    public URI getRawLocationURI() {
        return folder.getRawLocationURI();
    }

    public ResourceAttributes getResourceAttributes() {
        return folder.getResourceAttributes();
    }

    public Object getSessionProperty(QualifiedName key) throws CoreException {
        return folder.getSessionProperty(key);
    }

    public int getType() {
        return folder.getType();
    }

    public IWorkspace getWorkspace() {
        return folder.getWorkspace();
    }

    public boolean isAccessible() {
        return folder.isAccessible();
    }

    public boolean isConflicting(ISchedulingRule rule) {
        return folder.isConflicting(rule);
    }

    public boolean isDerived() {
        return folder.isDerived();
    }

    public boolean isLinked() {
        return folder.isLinked();
    }

    public boolean isLinked(int options) {
        return folder.isLinked(options);
    }

    public boolean isLocal(int depth) {
        return folder.isLocal(depth);
    }

    public boolean isPhantom() {
        return folder.isPhantom();
    }

    public boolean isReadOnly() {
        return folder.isReadOnly();
    }

    public boolean isSynchronized(int depth) {
        return folder.isSynchronized(depth);
    }

    public boolean isTeamPrivateMember() {
        return folder.isTeamPrivateMember();
    }

    public IResource[] members() throws CoreException {
        return folder.members();
    }

    public IResource[] members(boolean includePhantoms) throws CoreException {
        return folder.members(includePhantoms);
    }

    public IResource[] members(int memberFlags) throws CoreException {
        return folder.members(memberFlags);
    }

    public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        folder.move(destination, force, keepHistory, monitor);
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        folder.move(destination, force, monitor);
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        folder.move(destination, updateFlags, monitor);
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
        folder.move(description, force, keepHistory, monitor);
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        folder.move(description, updateFlags, monitor);
    }

    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
        folder.refreshLocal(depth, monitor);
    }

    public void revertModificationStamp(long value) throws CoreException {
        folder.revertModificationStamp(value);
    }

    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
        folder.setDefaultCharset(charset, monitor);
    }

    public void setDefaultCharset(String charset) throws CoreException {
        folder.setDefaultCharset(charset);
    }

    public void setDerived(boolean isDerived) throws CoreException {
        folder.setDerived(isDerived);
    }

    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
        folder.setLocal(flag, depth, monitor);
    }

    public long setLocalTimeStamp(long value) throws CoreException {
        return folder.setLocalTimeStamp(value);
    }

    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        folder.setPersistentProperty(key, value);
    }

    public void setReadOnly(boolean readOnly) {
        folder.setReadOnly(readOnly);
    }

    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
        folder.setResourceAttributes(attributes);
    }

    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
        folder.setSessionProperty(key, value);
    }

    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
        folder.setTeamPrivateMember(isTeamPrivate);
    }

    public void touch(IProgressMonitor monitor) throws CoreException {
        folder.touch(monitor);
    }

}
