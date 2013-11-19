/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.pythonpathconf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchManager;

@SuppressWarnings("unchecked")
public class MyEnvWorkingCopy implements ILaunchConfigurationWorkingCopy {

    private Map<String, Object> attributes = new HashMap<String, Object>();
    private InterpreterInfo info;

    public void setInfo(InterpreterInfo info) {
        this.attributes.clear();
        this.info = info;
        HashMap<String, String> map = new HashMap<String, String>();
        String[] envVariables = info.getEnvVariables();
        if (envVariables != null) {
            // We don't want to perform string substitution here nor exclude any variables
            InterpreterInfo.fillMapWithEnv(envVariables, map, null, null);
            this.attributes.put(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, map);
        } else {
            this.attributes.remove(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES);
        }
        this.attributes.put(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true);
    }

    public InterpreterInfo getInfo() {
        return this.info;
    }

    private void updateInfo() {
        if (info == null) {
            //no info set, nothing to do
            return;
        }

        Map<String, String> existing = (Map<String, String>) this.attributes.get(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES);
        HashMap<String, String> map = null;
        if (existing != null) {
            map = new HashMap<String, String>(existing);
        }
        //The interpreter info should never contain the PYTHONPATH, as it's managed from other places and not env. attributes.
        InterpreterInfo.removePythonPathFromEnvMapWithWarning(map);
        if (map == null) {
            info.setEnvVariables(null);
        } else {
            info.setEnvVariables(InterpreterInfo.createEnvWithMap(map));
        }
    }

    public void addModes(Set modes) {
        throw new RuntimeException();

    }

    public ILaunchConfiguration doSave() throws CoreException {
        throw new RuntimeException();

    }

    public ILaunchConfiguration getOriginal() {
        return this;

    }

    public ILaunchConfigurationWorkingCopy getParent() {
        return null;

    }

    public boolean isDirty() {
        throw new RuntimeException();

    }

    public Object removeAttribute(String attributeName) {
        Object ret = this.attributes.remove(attributeName);
        if (attributeName.equals(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES)) {
            updateInfo();
        }
        return ret;
    }

    public void removeModes(Set modes) {
        throw new RuntimeException();

    }

    public void rename(String name) {
        throw new RuntimeException();

    }

    public void setAttribute(String attributeName, int value) {
        this.attributes.put(attributeName, value);
        if (attributeName.equals(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES)) {
            updateInfo();
        }
    }

    public void setAttribute(String attributeName, String value) {
        this.attributes.put(attributeName, value);
        if (attributeName.equals(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES)) {
            updateInfo();
        }

    }

    public void setAttribute(String attributeName, @SuppressWarnings("rawtypes") List value) {
        this.attributes.put(attributeName, value);
        if (attributeName.equals(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES)) {
            updateInfo();
        }

    }

    public void setAttribute(String attributeName, @SuppressWarnings("rawtypes") Map value) {
        this.attributes.put(attributeName, value);
        if (attributeName.equals(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES)) {
            updateInfo();
        }

    }

    public void setAttribute(String attributeName, @SuppressWarnings("rawtypes") Set value) {
        this.attributes.put(attributeName, value);
        if (attributeName.equals(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES)) {
            updateInfo();
        }

    }

    public void setAttribute(String attributeName, boolean value) {
        this.attributes.put(attributeName, value);
        if (attributeName.equals(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES)) {
            updateInfo();
        }

    }

    public void setAttributes(Map attributes) {
        this.attributes.clear();
        if (attributes != null) {

        }
        Set<Map.Entry> entrySet = attributes.entrySet();
        for (Map.Entry entry : entrySet) {
            this.attributes.put((String) entry.getKey(), entry.getValue());
        }
        updateInfo();

    }

    public void setContainer(IContainer container) {
        throw new RuntimeException();

    }

    public void setMappedResources(IResource[] resources) {
        throw new RuntimeException();

    }

    public void setModes(Set modes) {
        throw new RuntimeException();

    }

    public void setPreferredLaunchDelegate(Set modes, String delegateId) {
        throw new RuntimeException();

    }

    public boolean contentsEqual(ILaunchConfiguration configuration) {
        throw new RuntimeException();

    }

    public ILaunchConfigurationWorkingCopy copy(String name) throws CoreException {
        throw new RuntimeException();

    }

    public void delete() throws CoreException {
        throw new RuntimeException();

    }

    public boolean exists() {
        throw new RuntimeException();

    }

    public boolean getAttribute(String attributeName, boolean defaultValue) throws CoreException {
        if (this.attributes.containsKey(attributeName)) {
            return (Boolean) this.attributes.get(attributeName);
        }
        return defaultValue;

    }

    public int getAttribute(String attributeName, int defaultValue) throws CoreException {

        if (this.attributes.containsKey(attributeName)) {
            return (Integer) this.attributes.get(attributeName);
        }
        return defaultValue;
    }

    public List getAttribute(String attributeName, List defaultValue) throws CoreException {

        if (this.attributes.containsKey(attributeName)) {
            return (List) this.attributes.get(attributeName);
        }
        return defaultValue;

    }

    public Set getAttribute(String attributeName, Set defaultValue) throws CoreException {

        if (this.attributes.containsKey(attributeName)) {
            return (Set) this.attributes.get(attributeName);
        }
        return defaultValue;

    }

    public Map getAttribute(String attributeName, Map defaultValue) throws CoreException {
        if (this.attributes.containsKey(attributeName)) {
            return (Map) this.attributes.get(attributeName);
        }
        return defaultValue;

    }

    public String getAttribute(String attributeName, String defaultValue) throws CoreException {
        if (this.attributes.containsKey(attributeName)) {
            return (String) this.attributes.get(attributeName);
        }
        return defaultValue;
    }

    public Map<String, Object> getAttributes() throws CoreException {
        return this.attributes;

    }

    public String getCategory() throws CoreException {
        throw new RuntimeException();

    }

    public IFile getFile() {
        throw new RuntimeException();

    }

    public IPath getLocation() {
        throw new RuntimeException();

    }

    public IResource[] getMappedResources() throws CoreException {
        throw new RuntimeException();

    }

    public String getMemento() throws CoreException {
        throw new RuntimeException();

    }

    public Set getModes() throws CoreException {
        throw new RuntimeException();

    }

    public String getName() {
        throw new RuntimeException();

    }

    public ILaunchDelegate getPreferredDelegate(Set modes) throws CoreException {
        throw new RuntimeException();

    }

    public ILaunchConfigurationType getType() throws CoreException {
        throw new RuntimeException();

    }

    public ILaunchConfigurationWorkingCopy getWorkingCopy() throws CoreException {
        throw new RuntimeException();

    }

    public boolean hasAttribute(String attributeName) throws CoreException {
        return this.attributes.containsKey(attributeName);

    }

    public boolean isLocal() {
        throw new RuntimeException();

    }

    public boolean isMigrationCandidate() throws CoreException {
        return false;

    }

    public boolean isReadOnly() {
        return false;

    }

    public boolean isWorkingCopy() {
        return true;

    }

    public ILaunch launch(String mode, IProgressMonitor monitor) throws CoreException {
        throw new RuntimeException();

    }

    public ILaunch launch(String mode, IProgressMonitor monitor, boolean build) throws CoreException {
        throw new RuntimeException();

    }

    public ILaunch launch(String mode, IProgressMonitor monitor, boolean build, boolean register) throws CoreException {
        throw new RuntimeException();

    }

    public void migrate() throws CoreException {
        throw new RuntimeException();

    }

    public boolean supportsMode(String mode) throws CoreException {
        throw new RuntimeException();

    }

    public Object getAdapter(Class adapter) {
        throw new RuntimeException();
    }

}
