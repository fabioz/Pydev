/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.resource_stubs;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.content.IContentTypeMatcher;

public class AbstractIProjectStub extends AbstractIContainerStub implements IProject {

    @Override
    public void build(int kind, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void close(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void create(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IProjectDescription getDescription() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IFile getFile(String name) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IFolder getFolder(String name) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IProjectNature getNature(String natureId) throws CoreException {
        return null;
    }

    @Override
    public IPath getWorkingLocation(String id) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IProject[] getReferencedProjects() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IProject[] getReferencingProjects() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasNature(String natureId) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isNatureEnabled(String natureId) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isOpen() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void move(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void open(IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setDescription(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setDescription(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
            throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void loadSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void saveSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void build(int kind, String builderName, Map<String, String> args, IProgressMonitor monitor)
            throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void build(IBuildConfiguration config, int kind, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IBuildConfiguration getActiveBuildConfig() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IBuildConfiguration getBuildConfig(String configName) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IBuildConfiguration[] getBuildConfigs() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IBuildConfiguration[] getReferencedBuildConfigs(String configName, boolean includeMissing)
            throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean hasBuildConfig(String configName) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getType() {
        return IResource.PROJECT;
    }

    public void clearCachedDynamicReferences() {
    }

}
