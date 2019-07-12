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
    @Override
    public IPathVariableManager getPathVariableManager() {
        return null;
    }

    //End Only for 3.6

    @Override
    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void accept(IResourceVisitor visitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void clearHistory(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IMarker createMarker(String type) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IResourceProxy createProxy() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public IMarker findMarker(long id) throws CoreException {
        return null;
    }

    @Override
    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
        return new IMarker[0];
    }

    @Override
    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
        return 0;
    }

    @Override
    public String getFileExtension() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IPath getFullPath() {
        throw new RuntimeException("Not implemented:" + this.getClass());
    }

    @Override
    public long getLocalTimeStamp() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IPath getLocation() {
        return null;
    }

    @Override
    public URI getLocationURI() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IMarker getMarker(long id) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public long getModificationStamp() {
        throw new RuntimeException("Not implemented in: " + this.getClass().getName());
    }

    @Override
    public String getName() {
        throw new RuntimeException("Not implemented in: " + this.getClass().getName());
    }

    @Override
    public IContainer getParent() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getPersistentProperty(QualifiedName key) throws CoreException {
        return null;
    }

    @Override
    public IProject getProject() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IPath getProjectRelativePath() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IPath getRawLocation() {
        throw new RuntimeException("Not implemented at:" + this.getClass());
    }

    @Override
    public URI getRawLocationURI() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ResourceAttributes getResourceAttributes() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
        return new HashMap<>();
    }

    @Override
    public Object getSessionProperty(QualifiedName key) throws CoreException {
        return null;
    }

    @Override
    public int getType() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IWorkspace getWorkspace() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isAccessible() {
        return false;
    }

    @Override
    public boolean isDerived() {
        return false;
    }

    @Override
    public boolean isDerived(int options) {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isHidden(int options) {
        return false;
    }

    @Override
    public boolean isLinked() {
        return false;
    }

    public boolean isGroup() {
        return false;
    }

    public boolean hasFilters() {
        return false;
    }

    @Override
    public boolean isLinked(int options) {
        return false;
    }

    @Override
    public boolean isLocal(int depth) {
        return true;
    }

    @Override
    public boolean isPhantom() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isSynchronized(int depth) {
        return true;
    }

    @Override
    public boolean isTeamPrivateMember() {
        return false;
    }

    @Override
    public boolean isTeamPrivateMember(int options) {
        return false;
    }

    @Override
    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor)
            throws CoreException {
    }

    @Override
    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public void revertModificationStamp(long value) throws CoreException {
    }

    @Override
    public void setDerived(boolean isDerived) throws CoreException {
    }

    @Override
    public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public void setHidden(boolean isHidden) throws CoreException {
    }

    @Override
    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public long setLocalTimeStamp(long value) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setReadOnly(boolean readOnly) {
    }

    @Override
    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {

    }

    @Override
    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {

    }

    @Override
    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
    }

    @Override
    public void touch(IProgressMonitor monitor) throws CoreException {
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean contains(ISchedulingRule rule) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isConflicting(ISchedulingRule rule) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    public boolean isFiltered() {
        return false;
    }

    @Override
    public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
    }

}
