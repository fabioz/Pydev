/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.shared_core.structure.OrderedMap;

public class PythonPathNatureStub implements IPythonPathNature {

    private HashSet<String> projectSourcePathSet;

    public PythonPathNatureStub(HashSet<String> projectSourcePathSet) {
        this.projectSourcePathSet = projectSourcePathSet;
    }

    public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter, IInterpreterManager manager) {
        throw new RuntimeException("Not impl");

    }

    public String getOnlyProjectPythonPathStr(boolean b) throws CoreException {
        throw new RuntimeException("Not impl");

    }

    public String getProjectExternalSourcePath(boolean resolve) throws CoreException {
        throw new RuntimeException("Not impl");

    }

    public List<String> getProjectExternalSourcePathAsList(boolean replaceVariables) throws CoreException {
        throw new RuntimeException("Not impl");
    }

    public String getProjectSourcePath(boolean resolve) throws CoreException {
        throw new RuntimeException("Not impl");

    }

    public Set<String> getProjectSourcePathSet(boolean resolve) throws CoreException {
        return projectSourcePathSet;
    }

    public void setProject(IProject project, IPythonNature nature) {
        throw new RuntimeException("Not impl");

    }

    public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException {
        throw new RuntimeException("Not impl");

    }

    public void setProjectSourcePath(String newSourcePath) throws CoreException {
        throw new RuntimeException("Not impl");

    }

    public void clearCaches() {
    }

    public void setVariableSubstitution(Map<String, String> variableSubstitution) {
        throw new RuntimeException("Not impl");
    }

    public Map<String, String> getVariableSubstitution() {
        throw new RuntimeException("Not implemented");
    }

    public Map<String, String> getVariableSubstitution(boolean b) {
        throw new RuntimeException("Not implemented");
    }

    public IPythonNature getNature() {
        throw new RuntimeException("Not implemented");
    }

    public OrderedMap<String, String> getProjectSourcePathResolvedToUnresolvedMap() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Set<IResource> getProjectSourcePathFolderSet() throws CoreException {
        throw new RuntimeException("not implemented");
    }

}
