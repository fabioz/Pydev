/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.nature;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.ast.codecompletion.revisited.SystemASTManager;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModuleRequestState;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PropertiesHelper;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.nature.AbstractPythonNature;
import org.python.pydev.shared_core.structure.OrderedMap;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This nature is used only as a 'last resort', if we're unable to link a given resource to
 * a project (and thus, we don't have project-related completions and we don't know with what
 * exactly we're dealing with: it's usually only used for external files)
 */
public class SystemPythonNature extends AbstractPythonNature implements IPythonNature {

    /**
     * @author Fabio
     *
     */
    private final class SystemPythonPathNature implements IPythonPathNature {
        @Override
        public void setVariableSubstitution(Map<String, String> variableSubstitution) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void setProjectSourcePath(String newSourcePath) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void setProject(IProject project, IPythonNature nature) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public Map<String, String> getVariableSubstitution(boolean addInterpreterInfoSubstitutions)
                throws CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
            return getVariableSubstitution();
        }

        @Override
        public Map<String, String> getVariableSubstitution() throws CoreException, MisconfigurationException,
                PythonNatureWithoutProjectException {
            Properties stringSubstitutionVariables = SystemPythonNature.this.info.getStringSubstitutionVariables(false);
            Map<String, String> variableSubstitution;
            if (stringSubstitutionVariables == null) {
                variableSubstitution = new HashMap<String, String>();
            } else {
                variableSubstitution = PropertiesHelper.createMapFromProperties(stringSubstitutionVariables);
            }
            return variableSubstitution;

        }

        @Override
        public Set<String> getProjectSourcePathSet(boolean replaceVariables) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public String getProjectSourcePath(boolean replaceVariables) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public List<String> getProjectExternalSourcePathAsList(boolean replaceVariables) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public String getProjectExternalSourcePath(boolean replaceVariables) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public String getOnlyProjectPythonPathStr(boolean addExternal) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public IPythonNature getNature() {
            return SystemPythonNature.this;
        }

        @Override
        public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter, IInterpreterManager info) {
            return interpreter.getPythonPath();
        }

        @Override
        public void clearCaches() {
            //No caches anyways
        }

        @Override
        public OrderedMap<String, String> getProjectSourcePathResolvedToUnresolvedMap() throws CoreException {
            throw new RuntimeException(
                    "Not implemented: We should use this only for doing path manipulation, "
                            + "which should not happen for the system python nature.");
        }

        @Override
        public Set<IResource> getProjectSourcePathFolderSet() throws CoreException {
            throw new RuntimeException("not implemented");
        }
    }

    private final IInterpreterManager manager;
    public final IInterpreterInfo info;
    private SystemASTManager systemASTManager;

    public SystemPythonNature(IInterpreterManager manager) throws MisconfigurationException {
        this(manager, manager.getDefaultInterpreterInfo(false));
    }

    public SystemPythonNature(IInterpreterManager manager, IInterpreterInfo info) {
        this.info = info;
        this.manager = manager;
    }

    @Override
    public boolean isResourceInPythonpathProjectSources(IResource resource, boolean addExternal)
            throws MisconfigurationException {
        return super.isResourceInPythonpath(resource); //no source folders in the system nature (just treat it as default)
    }

    @Override
    public boolean isResourceInPythonpathProjectSources(String resource, boolean addExternal)
            throws MisconfigurationException {
        return super.isResourceInPythonpath(resource); //no source folders in the system nature (just treat it as default)
    }

    @Override
    public String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath, boolean addExternal)
            throws CoreException, MisconfigurationException {
        return super.resolveModule(fileAbsolutePath);
    }

    @Override
    public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal) throws CoreException,
            MisconfigurationException {
        return super.resolveModule(new File(fileAbsolutePath));
    }

    @Override
    public String getVersion(boolean translateIfInterpreter) throws CoreException {
        if (!translateIfInterpreter) {
            switch (this.manager.getInterpreterType()) {
                case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                    return IPythonNature.PYTHON_VERSION_INTERPRETER;

                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                    return IPythonNature.JYTHON_VERSION_INTERPRETER;

                case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                    return IPythonNature.IRONPYTHON_VERSION_INTERPRETER;

                default:
                    throw new RuntimeException("Not Python nor Jython nor IronPython?");
            }
        }
        if (this.info != null) {
            String version = this.info.getVersion();
            if (version != null) {
                return IPythonNature.Versions.convertToInternalVersion(this.manager.getInterpreterType(), version);
            } else {
                Log.log("Unable to get version from interpreter info: " + this.info.getNameForUI() + " - "
                        + this.info.getExecutableOrJar());
            }
        }

        // Last fallback (we should almost never get here).

        switch (this.manager.getInterpreterType()) {
            case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                return IPythonNature.Versions.PYTHON_VERSION_LATEST;

            case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                return IPythonNature.Versions.JYTHON_VERSION_LATEST;

            case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                return IPythonNature.Versions.IRONPYTHON_VERSION_LATEST;

            default:
                throw new RuntimeException("Not Python nor Jython nor IronPython?");
        }
    }

    @Override
    public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
        return null;
    }

    @Override
    public void setVersion(String version, String interpreter) throws CoreException {
        throw new RuntimeException("Not Implemented: the system nature is read-only.");
    }

    @Override
    public int getInterpreterType() throws CoreException {
        return this.manager.getInterpreterType();
    }

    @Override
    public File getCompletionsCacheDir() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public void saveAstManager() {
        throw new RuntimeException("Not Implemented: system nature is only transient.");
    }

    @Override
    public IPythonPathNature getPythonPathNature() {
        return new SystemPythonPathNature();
    }

    @Override
    public String resolveModule(String file) throws MisconfigurationException {
        if (info == null) {
            return null;
        }
        return info.getModulesManager().resolveModule(file);
    }

    @Override
    public ICodeCompletionASTManager getAstManager() {
        if (systemASTManager == null) {
            systemASTManager = new SystemASTManager(this.manager, this, this.info);
        }
        return systemASTManager;
    }

    @Override
    public void configure() throws CoreException {
    }

    @Override
    public void deconfigure() throws CoreException {
    }

    @Override
    public IProject getProject() {
        return null;
    }

    @Override
    public void setProject(IProject project) {
    }

    @Override
    public void rebuildPath() {
        throw new RuntimeException("Not Implemented");
    }

    public void rebuildPath(String defaultSelectedInterpreter, IProgressMonitor monitor) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public IInterpreterManager getRelatedInterpreterManager() {
        return manager;
    }

    //builtin completions

    @Override
    public TokensList getBuiltinCompletions(IModuleRequestState moduleRequest) {
        if (!this.isOkToUse()) {
            return null;
        }
        return this.manager.getBuiltinCompletions(this.info.getName(), moduleRequest);
    }

    @Override
    public void clearBuiltinCompletions() {
        this.manager.clearBuiltinCompletions(this.info.getName());
    }

    //builtin mod

    @Override
    public IModule getBuiltinMod(IModuleRequestState moduleRequest) {
        if (!this.isOkToUse()) {
            return null;
        }
        return this.manager.getBuiltinMod(this.info.getName(), moduleRequest);
    }

    @Override
    public void clearBuiltinMod() {
        this.manager.clearBuiltinMod(this.info.getName());
    }

    @Override
    public int getGrammarVersion() throws MisconfigurationException {
        IInterpreterInfo info = this.info;
        if (info != null) {
            return info.getGrammarVersion();
        } else {
            return IPythonNature.LATEST_GRAMMAR_PY3_VERSION;
        }
    }

    @Override
    public IInterpreterInfo getProjectInterpreter() throws MisconfigurationException {
        return this.info;
    }

    @Override
    public boolean isOkToUse() {
        return this.manager != null && this.info != null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public Tuple<String, String> getVersionAndError(boolean translateIfInterpreter) throws CoreException {
        throw new RuntimeException("Not implemented (expected only for the PythonNature itself).");
    }
}
