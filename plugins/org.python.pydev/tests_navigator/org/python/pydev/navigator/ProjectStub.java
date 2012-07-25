/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.resource_stubs.AbstractIProjectStub;

public class ProjectStub extends AbstractIProjectStub implements IWorkbenchAdapter {

    private File projectRoot;

    private Map<File, IResource> cache = new HashMap<File, IResource>();

    private IPythonNature nature;

    private IContainer parent;

    private boolean addNullChild;

    private List<Object> additionalChildren;

    public ProjectStub(File file, IPythonNature nature) {
        this(file, nature, false);
    }

    public ProjectStub(File file, IPythonNature nature, boolean addNullChild) {
        this(file, nature, addNullChild, new ArrayList<Object>());
    }

    public ProjectStub(File file, IPythonNature nature, boolean addNullChild, List<Object> additionalChildren) {
        Assert.isTrue(file.exists() && file.isDirectory());
        this.projectRoot = file;
        this.nature = nature;
        this.addNullChild = addNullChild;
        this.additionalChildren = additionalChildren;
    }

    public IResource getResource(File parentFile) {
        if (parentFile.equals(projectRoot)) {
            return this;
        }

        IResource r = cache.get(parentFile);
        if (r == null) {
            if (parentFile.isFile()) {
                r = new FileStub(this, parentFile);
            } else {
                r = new FolderStub(this, parentFile);
            }
            cache.put(parentFile, r);
        }
        return r;
    }

    public IContainer getFolder(File parentFile) {
        return (IContainer) getResource(parentFile);
    }

    public void setParent(IContainer parent) {
        this.parent = parent;
    }

    public IContainer getParent() {
        return this.parent;
    }

    @Override
    public String toString() {
        return "ProjectStub:" + this.projectRoot;
    }

    public IProjectNature getNature(String natureId) throws CoreException {
        if (nature == null) {
            throw new RuntimeException("not expected");
        }
        return nature;
    }

    public boolean isOpen() {
        return true;

    }

    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {

    }

    public IPath getFullPath() {
        return Path.fromOSString(REF.getFileAbsolutePath(this.projectRoot));
    }

    public IPath getLocation() {
        return Path.fromOSString(REF.getFileAbsolutePath(this.projectRoot));
    }

    public IProject getProject() {
        return this;

    }

    public Object getAdapter(Class adapter) {
        if (adapter == IWorkbenchAdapter.class) {
            return this;
        }
        throw new RuntimeException("Not impl");

    }

    private HashMap<Object, Object[]> stubsCache = new HashMap<Object, Object[]>();

    //workbench adapter
    public Object[] getChildren(Object o) {
        Object[] found = stubsCache.get(o);
        if (found != null) {
            return found;
        }

        File folder = null;
        if (o instanceof ProjectStub) {
            ProjectStub projectStub = (ProjectStub) o;
            folder = projectStub.projectRoot;
        } else {
            throw new RuntimeException("Shouldn't happen");
        }
        ArrayList<Object> ret = new ArrayList<Object>();
        for (File file : folder.listFiles()) {
            String lower = file.getName().toLowerCase();
            if (lower.equals("cvs") || lower.equals(".svn")) {
                continue;
            }
            if (file.isDirectory()) {
                ret.add(new FolderStub(this, file));
            } else {
                ret.add(new FileStub(this, file));
            }
        }
        if (addNullChild) {
            ret.add(null);
        }
        ret.addAll(this.additionalChildren);
        return ret.toArray();
    }

    public ImageDescriptor getImageDescriptor(Object object) {
        throw new RuntimeException("Not implemented");
    }

    public String getLabel(Object o) {
        throw new RuntimeException("Not implemented");
    }

    public Object getParent(Object o) {
        throw new RuntimeException("Not implemented");
    }
}
