/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;

public class StringSubstitutionTest extends TestCase {

    public void testStringSubstitution() throws Exception {
        final Map<String, String> variableSubstitution = new HashMap<String, String>();
        variableSubstitution.put("AA", "XX");
        assertEquals("aaXXbb",
                createStringSubstitution(variableSubstitution).performPythonpathStringSubstitution("aa${AA}bb"));

        variableSubstitution.put("AA", "${XX}");
        variableSubstitution.put("XX", "YY");
        assertEquals("aaYYbb",
                createStringSubstitution(variableSubstitution).performPythonpathStringSubstitution("aa${AA}bb"));

        assertEquals("aa${unknown}bb", createStringSubstitution(variableSubstitution)
                .performPythonpathStringSubstitution("aa${unknown}bb"));
    }

    //Just creating stub...
    private StringSubstitution createStringSubstitution(final Map<String, String> variableSubstitution) {
        StringSubstitution s = new StringSubstitution(new IPythonNature() {

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

            public boolean isOkToUse() {
                throw new RuntimeException("Not implemented");
            }

            public IInterpreterInfo getProjectInterpreter() throws MisconfigurationException,
                    PythonNatureWithoutProjectException {
                throw new RuntimeException("Not implemented");
            }

            public IPythonPathNature getPythonPathNature() {
                return new IPythonPathNature() {

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

                    public Map<String, String> getVariableSubstitution() {
                        return variableSubstitution;
                    }

                    public Map<String, String> getVariableSubstitution(boolean b) {
                        return variableSubstitution;
                    }

                    public Set<String> getProjectSourcePathSet(boolean replace) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public String getProjectSourcePath(boolean replace) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public String getProjectExternalSourcePath(boolean replace) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public List<String> getProjectExternalSourcePathAsList(boolean replaceVariables)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public String getOnlyProjectPythonPathStr(boolean b) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter,
                            IInterpreterManager info) {
                        throw new RuntimeException("Not implemented");
                    }

                    public void clearCaches() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IPythonNature getNature() {
                        throw new RuntimeException("Not implemented");
                    }

                };
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

            public boolean isResourceInPythonpath(IResource resource) throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            public boolean isResourceInPythonpath(String resource) throws MisconfigurationException {
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

            public String resolveModule(File file) throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            public String resolveModule(String fileAbsolutePath) throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            public String resolveModule(IResource resource) throws MisconfigurationException {
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

            public int getGrammarVersion() throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal)
                    throws CoreException, MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            public String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath, boolean addExternal)
                    throws CoreException, MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }
        });
        return s;
    }
}
