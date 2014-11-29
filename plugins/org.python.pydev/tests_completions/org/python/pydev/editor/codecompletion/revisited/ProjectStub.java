/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.plugin.nature.FileStub2;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.resource_stubs.AbstractIProjectStub;
import org.python.pydev.shared_core.string.StringUtils;

public class ProjectStub extends AbstractIProjectStub implements IProject {

    String name;
    public IProject[] referencedProjects;
    public IProject[] referencingProjects;
    private PythonNature nature;
    private String path;
    private String externalSourcePath;

    public ProjectStub(String name, String path2, IProject[] referencedProjects, IProject[] referencingProjects) {
        List<String> split = StringUtils.split(path2, '|');
        this.path = split.remove(0);
        this.name = name;
        this.externalSourcePath = StringUtils.join("|", split);
        this.referencedProjects = referencedProjects;
        this.referencingProjects = referencingProjects;
    }

    public void setReferencedProjects(IProject... referencedProjects) {
        this.referencedProjects = referencedProjects;
    }

    public void setReferencingProjects(IProject... referencingProjects) {
        this.referencingProjects = referencingProjects;
    }

    @Override
    public IFile getFile(String name) {
        fileStub = new FileStub2(name);
        return fileStub;
    }

    public FileStub2 fileStub;

    @Override
    public IProjectNature getNature(String natureId) throws CoreException {
        if (nature == null) {
            throw new RuntimeException("not expected");
        }
        return nature;
    }

    @Override
    public IPath getWorkingLocation(String id) {
        return new Path(path);
    }

    @Override
    public IPath getFullPath() {
        return new Path(path);
    }

    @Override
    public IProject[] getReferencedProjects() throws CoreException {
        //no referenced projects
        return referencedProjects;
    }

    @Override
    public IProject[] getReferencingProjects() {
        return referencingProjects;
    }

    @Override
    public boolean hasNature(String natureId) throws CoreException {
        if (PythonNature.PYTHON_NATURE_ID.equals(natureId)) {
            return true;
        }
        throw new RuntimeException("not expected");
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPersistentProperty(QualifiedName key) throws CoreException {
        if (key.getLocalName().equals("PYTHON_PROJECT_VERSION")) {
            // TODO the comment below says "always the latests", but it isn't!
            return IPythonNature.PYTHON_VERSION_2_5;//for tests, always the latest version
        }
        if (key.getLocalName().equals("PROJECT_SOURCE_PATH")) {
            return this.path;
        }
        if (key.getLocalName().equals("PROJECT_EXTERNAL_SOURCE_PATH")) {
            return this.externalSourcePath;
        }
        throw new RuntimeException("not impl");
    }

    @Override
    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
        if (value == null) {
            return;
        }
        throw new RuntimeException("not impl");
    }

    public void setNature(PythonNature nature) {
        this.nature = nature;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ProjectStub: " + this.name;
    }
}
