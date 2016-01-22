/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
//Only for 3.6 -- comment if you want to compile on earlier eclipse version
import org.eclipse.core.resources.IPathVariableManager;
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

//End Only for 3.6

public class AbstractIResourceStub implements IResource {

    //Only for 3.6 -- comment if you want to compile on earlier eclipse version
    public IPathVariableManager getPathVariableManager() {
        return null;
    }

    //End Only for 3.6

    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void accept(IResourceVisitor visitor) throws CoreException {
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
        return true;
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
        throw new RuntimeException("Not implemented:" + this.getClass());
    }

    public long getLocalTimeStamp() {
        throw new RuntimeException("Not implemented");
    }

    public IPath getLocation() {
        return null;
    }

    public URI getLocationURI() {
        throw new RuntimeException("Not implemented");
    }

    public IMarker getMarker(long id) {
        throw new RuntimeException("Not implemented");
    }

    public long getModificationStamp() {
        throw new RuntimeException("Not implemented in: " + this.getClass().getName());
    }

    public String getName() {
        throw new RuntimeException("Not implemented in: " + this.getClass().getName());
    }

    public IContainer getParent() {
        throw new RuntimeException("Not implemented");
    }

    public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public String getPersistentProperty(QualifiedName key) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IProject getProject() {
        throw new RuntimeException("Not implemented");
    }

    public IPath getProjectRelativePath() {
        throw new RuntimeException("Not implemented");
    }

    public IPath getRawLocation() {
        throw new RuntimeException("Not implemented at:" + this.getClass());
    }

    public URI getRawLocationURI() {
        throw new RuntimeException("Not implemented");
    }

    public ResourceAttributes getResourceAttributes() {
        throw new RuntimeException("Not implemented");
    }

    public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
        return new HashMap<>();
    }

    public Object getSessionProperty(QualifiedName key) throws CoreException {
        return null;
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
        return false;
    }

    public boolean isDerived(int options) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isHidden() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isHidden(int options) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isLinked() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isGroup() {
        throw new RuntimeException("Not implemented");
    }

    public boolean hasFilters() {
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
        return false;
    }

    public boolean isSynchronized(int depth) {
        return true;
    }

    public boolean isTeamPrivateMember() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isTeamPrivateMember(int options) {
        throw new RuntimeException("Not implemented");
    }

    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
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

    public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void setHidden(boolean isHidden) throws CoreException {
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
        throw new RuntimeException("Not implemented");
    }

    public boolean contains(ISchedulingRule rule) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isConflicting(ISchedulingRule rule) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isVirtual() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isFiltered() {
        throw new RuntimeException("Not implemented");
    }

    public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

}
