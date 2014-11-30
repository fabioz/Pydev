/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;

public class PythonNatureStub implements IPythonNature, IAdaptable {

    @Override
    public Object getAdapter(Class adapter) {
        throw new RuntimeException("Not implemented");
    }

    public void endRequests() {
        throw new RuntimeException("Not implemented");
    }

    public ICodeCompletionASTManager getAstManager() {
        throw new RuntimeException("Not implemented");
    }

    public IToken[] getBuiltinCompletions() {
        throw new RuntimeException("Not implemented");
    }

    public IModule getBuiltinMod() {
        throw new RuntimeException("Not implemented");
    }

    public File getCompletionsCacheDir() {
        throw new RuntimeException("Not implemented");
    }

    public String getDefaultVersion() {
        throw new RuntimeException("Not implemented");
    }

    public IPythonPathNature getPythonPathNature() {
        throw new RuntimeException("Not implemented");
    }

    public int getInterpreterType() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IInterpreterManager getRelatedInterpreterManager() {
        throw new RuntimeException("Not implemented");
    }

    public String getVersion() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public boolean isJython() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public boolean isPython() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public boolean isResourceInPythonpath(IResource resource) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isResourceInPythonpath(String resource) {
        throw new RuntimeException("Not implemented");
    }

    public boolean isResourceInPythonpathProjectSources(IResource resource, boolean addExternal)
            throws MisconfigurationException {
        throw new RuntimeException("Not implemented");
    }

    public boolean isResourceInPythonpathProjectSources(String resource, boolean addExternal)
            throws MisconfigurationException {
        throw new RuntimeException("Not implemented");
    }

    public void rebuildPath() {
        throw new RuntimeException("Not implemented");
    }

    public void rebuildPath(String defaultSelectedInterpreter, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    public String resolveModule(File file) {
        throw new RuntimeException("Not implemented");
    }

    public String resolveModule(String fileAbsolutePath) {
        throw new RuntimeException("Not implemented");
    }

    public String resolveModule(IResource resource) {
        throw new RuntimeException("Not implemented");
    }

    public void saveAstManager() {
        throw new RuntimeException("Not implemented");
    }

    public void clearBuiltinCompletions() {
        throw new RuntimeException("Not implemented");
    }

    public void clearBuiltinMod() {
        throw new RuntimeException("Not implemented");
    }

    public void setVersion(String version, String interpreter) throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public boolean startRequests() {
        throw new RuntimeException("Not implemented");
    }

    public void configure() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public void deconfigure() throws CoreException {
        throw new RuntimeException("Not implemented");
    }

    public IProject getProject() {
        throw new RuntimeException("Not implemented");
    }

    public void setProject(IProject project) {
        throw new RuntimeException("Not implemented");
    }

    public int getGrammarVersion() {
        return IPythonNature.GRAMMAR_PYTHON_VERSION_2_5;
    }

    public IInterpreterInfo getProjectInterpreter() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isOkToUse() {
        throw new RuntimeException("Not implemented");
    }

    public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal) throws CoreException,
            MisconfigurationException {
        throw new RuntimeException("Not implemented");
    }

    public String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath, boolean addExternal)
            throws CoreException, MisconfigurationException {
        throw new RuntimeException("Not implemented");
    }
}
