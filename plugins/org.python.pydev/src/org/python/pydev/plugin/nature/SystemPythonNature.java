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
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PropertiesHelper;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.editor.codecompletion.revisited.SystemASTManager;
import org.python.pydev.shared_core.structure.OrderedMap;

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
        public void setVariableSubstitution(Map<String, String> variableSubstitution) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        public void setProjectSourcePath(String newSourcePath) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        public void setProject(IProject project, IPythonNature nature) {
            throw new RuntimeException("Not implemented");
        }

        public Map<String, String> getVariableSubstitution(boolean addInterpreterInfoSubstitutions)
                throws CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
            return getVariableSubstitution();
        }

        public Map<String, String> getVariableSubstitution() throws CoreException, MisconfigurationException,
                PythonNatureWithoutProjectException {
            Properties stringSubstitutionVariables = SystemPythonNature.this.info.getStringSubstitutionVariables();
            Map<String, String> variableSubstitution;
            if (stringSubstitutionVariables == null) {
                variableSubstitution = new HashMap<String, String>();
            } else {
                variableSubstitution = PropertiesHelper.createMapFromProperties(stringSubstitutionVariables);
            }
            return variableSubstitution;

        }

        public Set<String> getProjectSourcePathSet(boolean replaceVariables) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        public String getProjectSourcePath(boolean replaceVariables) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        public List<String> getProjectExternalSourcePathAsList(boolean replaceVariables) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        public String getProjectExternalSourcePath(boolean replaceVariables) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        public String getOnlyProjectPythonPathStr(boolean addExternal) throws CoreException {
            throw new RuntimeException("Not implemented");
        }

        public IPythonNature getNature() {
            return SystemPythonNature.this;
        }

        public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter, IInterpreterManager info) {
            return interpreter.getPythonPath();
        }

        public void clearCaches() {
            //No caches anyways
        }

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

    public boolean isResourceInPythonpathProjectSources(IResource resource, boolean addExternal)
            throws MisconfigurationException {
        return super.isResourceInPythonpath(resource); //no source folders in the system nature (just treat it as default)
    }

    public boolean isResourceInPythonpathProjectSources(String resource, boolean addExternal)
            throws MisconfigurationException {
        return super.isResourceInPythonpath(resource); //no source folders in the system nature (just treat it as default)
    }

    public String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath, boolean addExternal)
            throws CoreException, MisconfigurationException {
        return super.resolveModule(fileAbsolutePath);
    }

    public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal) throws CoreException,
            MisconfigurationException {
        return super.resolveModule(new File(fileAbsolutePath));
    }

    public String getVersion() throws CoreException {
        if (this.info != null) {
            String version = this.info.getVersion();
            if (version != null && version.startsWith("3")) {
                switch (this.manager.getInterpreterType()) {

                    case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                        return IPythonNature.PYTHON_VERSION_3_0;

                    case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                        return IPythonNature.JYTHON_VERSION_3_0;

                    case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                        return IPythonNature.PYTHON_VERSION_3_0;

                    default:
                        throw new RuntimeException("Not Python nor Jython nor IronPython?");
                }
            }
        }
        switch (this.manager.getInterpreterType()) {

            case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                return IPythonNature.PYTHON_VERSION_LATEST;

            case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                return IPythonNature.JYTHON_VERSION_LATEST;

            case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                return IPythonNature.PYTHON_VERSION_LATEST;

            default:
                throw new RuntimeException("Not Python nor Jython nor IronPython?");
        }
    }

    public String getDefaultVersion() {
        try {
            return getVersion();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public void setVersion(String version, String interpreter) throws CoreException {
        throw new RuntimeException("Not Implemented: the system nature is read-only.");
    }

    public int getInterpreterType() throws CoreException {
        return this.manager.getInterpreterType();
    }

    public File getCompletionsCacheDir() {
        throw new RuntimeException("Not Implemented");
    }

    public void saveAstManager() {
        throw new RuntimeException("Not Implemented: system nature is only transient.");
    }

    public IPythonPathNature getPythonPathNature() {
        return new SystemPythonPathNature();
    }

    public String resolveModule(String file) throws MisconfigurationException {
        if (info == null) {
            return null;
        }
        return info.getModulesManager().resolveModule(file);
    }

    public ICodeCompletionASTManager getAstManager() {
        if (systemASTManager == null) {
            systemASTManager = new SystemASTManager(this.manager, this, this.info);
        }
        return systemASTManager;
    }

    public void configure() throws CoreException {
    }

    public void deconfigure() throws CoreException {
    }

    public IProject getProject() {
        return null;
    }

    public void setProject(IProject project) {
    }

    public void rebuildPath() {
        throw new RuntimeException("Not Implemented");
    }

    public void rebuildPath(String defaultSelectedInterpreter, IProgressMonitor monitor) {
        throw new RuntimeException("Not Implemented");
    }

    public IInterpreterManager getRelatedInterpreterManager() {
        return manager;
    }

    //builtin completions

    public IToken[] getBuiltinCompletions() {
        if (!this.isOkToUse()) {
            return null;
        }
        return this.manager.getBuiltinCompletions(this.info.getName());
    }

    public void clearBuiltinCompletions() {
        this.manager.clearBuiltinCompletions(this.info.getName());
    }

    //builtin mod

    public IModule getBuiltinMod() {
        if (!this.isOkToUse()) {
            return null;
        }
        return this.manager.getBuiltinMod(this.info.getName());
    }

    public void clearBuiltinMod() {
        this.manager.clearBuiltinMod(this.info.getName());
    }

    public int getGrammarVersion() throws MisconfigurationException {
        IInterpreterInfo info = this.info;
        if (info != null) {
            return info.getGrammarVersion();
        } else {
            return IPythonNature.LATEST_GRAMMAR_VERSION;
        }
    }

    public IInterpreterInfo getProjectInterpreter() throws MisconfigurationException {
        return this.info;
    }

    public boolean isOkToUse() {
        return this.manager != null && this.info != null;
    }

    @Override
    public Object getAdapter(Class adapter) {
        return null;
    }
}
