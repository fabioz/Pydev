package org.python.pydev.navigator;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFilterMatcherDescriptor;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ISavedState;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.WorkspaceLock;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class WorkspaceStub implements IWorkspace{

    
    public Object getAdapter(Class adapter) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public void addResourceChangeListener(IResourceChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    
    public void addResourceChangeListener(IResourceChangeListener listener, int eventMask) {
        // TODO Auto-generated method stub
        
    }

    
    public ISavedState addSaveParticipant(Plugin plugin, ISaveParticipant participant) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public ISavedState addSaveParticipant(String pluginId, ISaveParticipant participant) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public void build(int kind, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    
    public void checkpoint(boolean build) {
        // TODO Auto-generated method stub
        
    }

    
    public IProject[][] computePrerequisiteOrder(IProject[] projects) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public ProjectOrder computeProjectOrder(IProject[] projects) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus copy(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus copy(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus delete(IResource[] resources, boolean force, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus delete(IResource[] resources, int updateFlags, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public void deleteMarkers(IMarker[] markers) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    
    public void forgetSavedTree(String pluginId) {
        // TODO Auto-generated method stub
        
    }

    
    public IFilterMatcherDescriptor[] getFilterMatcherDescriptors() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IFilterMatcherDescriptor getFilterMatcherDescriptor(String filterMatcherId) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IProjectNatureDescriptor[] getNatureDescriptors() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IProjectNatureDescriptor getNatureDescriptor(String natureId) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public Map getDanglingReferences() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IWorkspaceDescription getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IWorkspaceRoot getRoot() {
        return new WorkspaceRootStub();
    }

    
    public IResourceRuleFactory getRuleFactory() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public ISynchronizer getSynchronizer() {
        // TODO Auto-generated method stub
        return null;
    }

    
    public boolean isAutoBuilding() {
        // TODO Auto-generated method stub
        return false;
    }

    
    public boolean isTreeLocked() {
        // TODO Auto-generated method stub
        return false;
    }

    
    public IProjectDescription loadProjectDescription(IPath projectDescriptionFile) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IProjectDescription loadProjectDescription(InputStream projectDescriptionFile) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus move(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus move(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IProjectDescription newProjectDescription(String projectName) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public void removeResourceChangeListener(IResourceChangeListener listener) {
        // TODO Auto-generated method stub
        
    }

    
    public void removeSaveParticipant(Plugin plugin) {
        // TODO Auto-generated method stub
        
    }

    
    public void removeSaveParticipant(String pluginId) {
        // TODO Auto-generated method stub
        
    }

    
    public void run(IWorkspaceRunnable action, ISchedulingRule rule, int flags, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    
    public void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    
    public IStatus save(boolean full, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    
    public void setDescription(IWorkspaceDescription description) throws CoreException {
        // TODO Auto-generated method stub
        
    }

    
    public void setWorkspaceLock(WorkspaceLock lock) {
        // TODO Auto-generated method stub
        
    }

    
    public String[] sortNatureSet(String[] natureIds) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validateEdit(IFile[] files, Object context) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validateFiltered(IResource resource) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validateLinkLocation(IResource resource, IPath location) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validateLinkLocationURI(IResource resource, URI location) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validateName(String segment, int typeMask) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validateNatureSet(String[] natureIds) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validatePath(String path, int typeMask) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validateProjectLocation(IProject project, IPath location) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IStatus validateProjectLocationURI(IProject project, URI location) {
        // TODO Auto-generated method stub
        return null;
    }

    
    public IPathVariableManager getPathVariableManager() {
        // TODO Auto-generated method stub
        return null;
    }

}
