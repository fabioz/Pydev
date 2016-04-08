/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IBuildConfiguration;
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class WorkspaceStub implements IWorkspace {
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public void addResourceChangeListener(IResourceChangeListener listener) {
    }

    @Override
    public void addResourceChangeListener(IResourceChangeListener listener, int eventMask) {
    }

    @Override
    public ISavedState addSaveParticipant(Plugin plugin, ISaveParticipant participant) throws CoreException {
        return null;
    }

    @Override
    public ISavedState addSaveParticipant(String pluginId, ISaveParticipant participant) throws CoreException {
        return null;
    }

    @Override
    public void build(int kind, IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public void checkpoint(boolean build) {
    }

    @Override
    public IProject[][] computePrerequisiteOrder(IProject[] projects) {
        return null;
    }

    @Override
    public ProjectOrder computeProjectOrder(IProject[] projects) {
        return null;
    }

    @Override
    public IStatus copy(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    @Override
    public IStatus copy(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    @Override
    public IStatus delete(IResource[] resources, boolean force, IProgressMonitor monitor) throws CoreException {
        return null;
    }

    @Override
    public IStatus delete(IResource[] resources, int updateFlags, IProgressMonitor monitor) throws CoreException {
        return null;
    }

    @Override
    public void deleteMarkers(IMarker[] markers) throws CoreException {
    }

    @Override
    public void forgetSavedTree(String pluginId) {
    }

    @Override
    public IFilterMatcherDescriptor[] getFilterMatcherDescriptors() {
        return null;
    }

    @Override
    public IFilterMatcherDescriptor getFilterMatcherDescriptor(String filterMatcherId) {
        return null;
    }

    @Override
    public IProjectNatureDescriptor[] getNatureDescriptors() {
        return null;
    }

    @Override
    public IProjectNatureDescriptor getNatureDescriptor(String natureId) {
        return null;
    }

    @Override
    public Map<IProject, IProject[]> getDanglingReferences() {
        return null;
    }

    @Override
    public IWorkspaceDescription getDescription() {
        return null;
    }

    @Override
    public IWorkspaceRoot getRoot() {
        return new WorkspaceRootStub();
    }

    @Override
    public IResourceRuleFactory getRuleFactory() {
        return null;
    }

    @Override
    public ISynchronizer getSynchronizer() {
        return null;
    }

    @Override
    public boolean isAutoBuilding() {
        return false;
    }

    @Override
    public boolean isTreeLocked() {
        return false;
    }

    @Override
    public IProjectDescription loadProjectDescription(IPath projectDescriptionFile) throws CoreException {
        return null;
    }

    @Override
    public IProjectDescription loadProjectDescription(InputStream projectDescriptionFile) throws CoreException {
        return null;
    }

    @Override
    public IStatus move(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    @Override
    public IStatus move(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    @Override
    public IBuildConfiguration newBuildConfig(String projectName, String configName) {
        return null;
    }

    @Override
    public IProjectDescription newProjectDescription(String projectName) {
        return null;
    }

    @Override
    public void removeResourceChangeListener(IResourceChangeListener listener) {
    }

    @Override
    public void removeSaveParticipant(Plugin plugin) {
    }

    @Override
    public void removeSaveParticipant(String pluginId) {
    }

    @Override
    public void run(IWorkspaceRunnable action, ISchedulingRule rule, int flags, IProgressMonitor monitor)
            throws CoreException {
    }

    @Override
    public void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public IStatus save(boolean full, IProgressMonitor monitor) throws CoreException {
        return null;
    }

    @Override
    public void setDescription(IWorkspaceDescription description) throws CoreException {
    }

    @Override
    public String[] sortNatureSet(String[] natureIds) {
        return null;
    }

    @Override
    public IStatus validateEdit(IFile[] files, Object context) {
        return null;
    }

    @Override
    public IStatus validateFiltered(IResource resource) {
        return null;
    }

    @Override
    public IStatus validateLinkLocation(IResource resource, IPath location) {
        return null;
    }

    @Override
    public IStatus validateLinkLocationURI(IResource resource, URI location) {
        return null;
    }

    @Override
    public IStatus validateName(String segment, int typeMask) {
        return null;
    }

    @Override
    public IStatus validateNatureSet(String[] natureIds) {
        return null;
    }

    @Override
    public IStatus validatePath(String path, int typeMask) {
        return null;
    }

    @Override
    public IStatus validateProjectLocation(IProject project, IPath location) {
        return null;
    }

    @Override
    public IStatus validateProjectLocationURI(IProject project, URI location) {
        return null;
    }

    @Override
    public IPathVariableManager getPathVariableManager() {
        return null;
    }

    @Override
    public void build(IBuildConfiguration[] buildConfigs, int kind, boolean buildReferences, IProgressMonitor monitor)
            throws CoreException {
    }

    public void run(ICoreRunnable action, ISchedulingRule rule, int flags, IProgressMonitor monitor)
            throws CoreException {
    }

    public void run(ICoreRunnable action, IProgressMonitor monitor) throws CoreException {
    }

}
