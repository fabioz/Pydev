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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class WorkspaceStub implements IWorkspace {
    public Object getAdapter(Class adapter) {
        return null;
    }

    public void addResourceChangeListener(IResourceChangeListener listener) {
    }

    public void addResourceChangeListener(IResourceChangeListener listener, int eventMask) {
    }

    public ISavedState addSaveParticipant(Plugin plugin, ISaveParticipant participant) throws CoreException {
        return null;
    }

    public ISavedState addSaveParticipant(String pluginId, ISaveParticipant participant) throws CoreException {
        return null;
    }

    public void build(int kind, IProgressMonitor monitor) throws CoreException {
    }

    public void checkpoint(boolean build) {
    }

    public IProject[][] computePrerequisiteOrder(IProject[] projects) {
        return null;
    }

    public ProjectOrder computeProjectOrder(IProject[] projects) {
        return null;
    }

    public IStatus copy(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    public IStatus copy(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    public IStatus delete(IResource[] resources, boolean force, IProgressMonitor monitor) throws CoreException {
        return null;
    }

    public IStatus delete(IResource[] resources, int updateFlags, IProgressMonitor monitor) throws CoreException {
        return null;
    }

    public void deleteMarkers(IMarker[] markers) throws CoreException {
    }

    public void forgetSavedTree(String pluginId) {
    }

    public IFilterMatcherDescriptor[] getFilterMatcherDescriptors() {
        return null;
    }

    public IFilterMatcherDescriptor getFilterMatcherDescriptor(String filterMatcherId) {
        return null;
    }

    public IProjectNatureDescriptor[] getNatureDescriptors() {
        return null;
    }

    public IProjectNatureDescriptor getNatureDescriptor(String natureId) {
        return null;
    }

    public Map<IProject, IProject[]> getDanglingReferences() {
        return null;
    }

    public IWorkspaceDescription getDescription() {
        return null;
    }

    public IWorkspaceRoot getRoot() {
        return new WorkspaceRootStub();
    }

    public IResourceRuleFactory getRuleFactory() {
        return null;
    }

    public ISynchronizer getSynchronizer() {
        return null;
    }

    public boolean isAutoBuilding() {
        return false;
    }

    public boolean isTreeLocked() {
        return false;
    }

    public IProjectDescription loadProjectDescription(IPath projectDescriptionFile) throws CoreException {
        return null;
    }

    public IProjectDescription loadProjectDescription(InputStream projectDescriptionFile) throws CoreException {
        return null;
    }

    public IStatus move(IResource[] resources, IPath destination, boolean force, IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    public IStatus move(IResource[] resources, IPath destination, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    public IBuildConfiguration newBuildConfig(String projectName, String configName) {
        return null;
    }

    public IProjectDescription newProjectDescription(String projectName) {
        return null;
    }

    public void removeResourceChangeListener(IResourceChangeListener listener) {
    }

    public void removeSaveParticipant(Plugin plugin) {
    }

    public void removeSaveParticipant(String pluginId) {
    }

    public void run(IWorkspaceRunnable action, ISchedulingRule rule, int flags, IProgressMonitor monitor)
            throws CoreException {
    }

    public void run(IWorkspaceRunnable action, IProgressMonitor monitor) throws CoreException {
    }

    public IStatus save(boolean full, IProgressMonitor monitor) throws CoreException {
        return null;
    }

    public void setDescription(IWorkspaceDescription description) throws CoreException {
    }

    public String[] sortNatureSet(String[] natureIds) {
        return null;
    }

    public IStatus validateEdit(IFile[] files, Object context) {
        return null;
    }

    public IStatus validateFiltered(IResource resource) {
        return null;
    }

    public IStatus validateLinkLocation(IResource resource, IPath location) {
        return null;
    }

    public IStatus validateLinkLocationURI(IResource resource, URI location) {
        return null;
    }

    public IStatus validateName(String segment, int typeMask) {
        return null;
    }

    public IStatus validateNatureSet(String[] natureIds) {
        return null;
    }

    public IStatus validatePath(String path, int typeMask) {
        return null;
    }

    public IStatus validateProjectLocation(IProject project, IPath location) {
        return null;
    }

    public IStatus validateProjectLocationURI(IProject project, URI location) {
        return null;
    }

    public IPathVariableManager getPathVariableManager() {
        return null;
    }

    public void build(IBuildConfiguration[] buildConfigs, int kind, boolean buildReferences, IProgressMonitor monitor)
            throws CoreException {
    }

}
