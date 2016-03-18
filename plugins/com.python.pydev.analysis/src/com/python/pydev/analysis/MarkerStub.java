/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis;

import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class MarkerStub implements IMarker {

    private static final Object TYPE = "TYPE_STUB";

    private Map<String, Object> attrs;

    public MarkerStub(Map<String, Object> attrs) {
        this.attrs = attrs;
    }

    @Override
    public void delete() throws CoreException {
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public Object getAttribute(String attributeName) throws CoreException {
        return attrs.get(attributeName);
    }

    @Override
    public int getAttribute(String attributeName, int defaultValue) {
        Integer i = (Integer) attrs.get(attributeName);
        if (i == null) {
            return defaultValue;
        }
        return i;
    }

    @Override
    public String getAttribute(String attributeName, String defaultValue) {
        String i = (String) attrs.get(attributeName);
        if (i == null) {
            return defaultValue;
        }
        return i;
    }

    @Override
    public boolean getAttribute(String attributeName, boolean defaultValue) {
        Boolean i = (Boolean) attrs.get(attributeName);
        if (i == null) {
            return defaultValue;
        }
        return i;
    }

    @Override
    public Map<String, Object> getAttributes() throws CoreException {
        return attrs;
    }

    @Override
    public Object[] getAttributes(String[] attributeNames) throws CoreException {
        throw new RuntimeException("not impl");
    }

    @Override
    public long getCreationTime() throws CoreException {
        return 0;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public IResource getResource() {
        throw new RuntimeException("not impl");
    }

    @Override
    public String getType() throws CoreException {
        return (String) attrs.get(TYPE);
    }

    @Override
    public boolean isSubtypeOf(String superType) throws CoreException {
        return false;
    }

    @Override
    public void setAttribute(String attributeName, int value) throws CoreException {
        attrs.put(attributeName, value);
    }

    @Override
    public void setAttribute(String attributeName, Object value) throws CoreException {
        attrs.put(attributeName, value);
    }

    @Override
    public void setAttribute(String attributeName, boolean value) throws CoreException {
        attrs.put(attributeName, value);
    }

    @Override
    public void setAttributes(String[] attributeNames, Object[] values) throws CoreException {
        throw new RuntimeException("not impl");
    }

    @Override
    public void setAttributes(Map attributes) throws CoreException {
        this.attrs = attributes;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        throw new RuntimeException("not impl");
    }

}
