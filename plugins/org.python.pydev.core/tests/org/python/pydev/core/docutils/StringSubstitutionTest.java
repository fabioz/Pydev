/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.docutils;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableChangeListener;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.structure.OrderedMap;

public class StringSubstitutionTest extends TestCase {

    public void testStringSubstitution() throws Exception {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        final Map<String, String> variableSubstitution = new HashMap<String, String>();
        final Map<String, String> pathSubstitution = new HashMap<String, String>();
        variableSubstitution.put("AA", "XX");
        assertEquals(
                "aaXXbb${BB}",
                createStringSubstitution(variableSubstitution, pathSubstitution).performPythonpathStringSubstitution(
                        "aa${AA}bb${BB}"));

        pathSubstitution.put("BB", "/ZZ");
        String sep = System.getProperty("file.separator");
        assertEquals(
                "aaXXbb" + sep + "ZZ",
                createStringSubstitution(variableSubstitution, pathSubstitution).performPythonpathStringSubstitution(
                        "aa${AA}bb${BB}"));

        variableSubstitution.put("BB", "WW");
        assertEquals(
                "aaWWbb",
                createStringSubstitution(variableSubstitution, pathSubstitution).performPythonpathStringSubstitution(
                        "aa${BB}bb"));

        variableSubstitution.put("AA", "${XX}");
        variableSubstitution.put("XX", "YY");
        assertEquals(
                "aaYYbb",
                createStringSubstitution(variableSubstitution, pathSubstitution).performPythonpathStringSubstitution(
                        "aa${AA}bb"));

        assertEquals("aa${unknown}bb", createStringSubstitution(variableSubstitution, pathSubstitution)
                .performPythonpathStringSubstitution("aa${unknown}bb"));
    }

    //Just creating stub...
    private StringSubstitution createStringSubstitution(final Map<String, String> variableSubstitution,
            final Map<String, String> pathSubstitution) {
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

            @Override
            public Object getAdapter(Class adapter) {
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

                    public OrderedMap<String, String> getProjectSourcePathResolvedToUnresolvedMap()
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public Set<IResource> getProjectSourcePathFolderSet() throws CoreException {
                        throw new RuntimeException("not implemented");
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
                return new IProject() {

                    public boolean exists(IPath path) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResource findMember(String path) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResource findMember(String path, boolean includePhantoms) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResource findMember(IPath path) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResource findMember(IPath path, boolean includePhantoms) {
                        throw new RuntimeException("Not implemented");
                    }

                    public String getDefaultCharset() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IFile getFile(IPath path) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IFolder getFolder(IPath path) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResource[] members() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResource[] members(boolean includePhantoms) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResource[] members(int memberFlags) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setDefaultCharset(String charset) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResourceFilterDescription createFilter(int type,
                            FileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResourceFilterDescription[] getFilters() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void accept(IResourceVisitor visitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void clearHistory(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IMarker createMarker(String type) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IResourceProxy createProxy() {
                        throw new RuntimeException("Not implemented");
                    }

                    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean exists() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IMarker findMarker(long id) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public String getFileExtension() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IPath getFullPath() {
                        throw new RuntimeException("Not implemented");
                    }

                    public long getLocalTimeStamp() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IPath getLocation() {
                        return null;
                    }

                    public URI getLocationURI() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IMarker getMarker(long id) {
                        throw new RuntimeException("Not implemented");
                    }

                    public long getModificationStamp() {
                        throw new RuntimeException("Not implemented");
                    }

                    public String getName() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IPathVariableManager getPathVariableManager() {
                        return new IPathVariableManager() {

                            public URI convertToRelative(URI path, boolean force, String variableHint)
                                    throws CoreException {
                                throw new RuntimeException("Not implemented");
                            }

                            public void setValue(String name, IPath value) throws CoreException {
                                throw new RuntimeException("Not implemented");
                            }

                            public void setURIValue(String name, URI value) throws CoreException {
                                throw new RuntimeException("Not implemented");
                            }

                            public IPath getValue(String name) {
                                throw new RuntimeException("Not implemented");
                            }

                            public URI getURIValue(String name) {
                                try {
                                    return new URI("file://".concat(pathSubstitution.get(name)));
                                } catch (Exception e) {
                                    ;
                                }
                                return null;
                            }

                            public String[] getPathVariableNames() {
                                return pathSubstitution.keySet().toArray(new String[] {});
                            }

                            public void addChangeListener(IPathVariableChangeListener listener) {
                                throw new RuntimeException("Not implemented");
                            }

                            public void removeChangeListener(IPathVariableChangeListener listener) {
                                throw new RuntimeException("Not implemented");
                            }

                            public URI resolveURI(URI uri) {
                                throw new RuntimeException("Not implemented");
                            }

                            public IPath resolvePath(IPath path) {
                                throw new RuntimeException("Not implemented");
                            }

                            public boolean isDefined(String name) {
                                throw new RuntimeException("Not implemented");
                            }

                            public boolean isUserDefined(String name) {
                                throw new RuntimeException("Not implemented");
                            }

                            public IStatus validateName(String name) {
                                throw new RuntimeException("Not implemented");
                            }

                            public IStatus validateValue(IPath path) {
                                throw new RuntimeException("Not implemented");
                            }

                            public IStatus validateValue(URI path) {
                                throw new RuntimeException("Not implemented");
                            }

                            public URI getVariableRelativePathLocation(URI location) {
                                throw new RuntimeException("Not implemented");
                            }

                            public String convertToUserEditableFormat(String value, boolean locationFormat) {
                                throw new RuntimeException("Not implemented");
                            }

                            public String convertFromUserEditableFormat(String value, boolean locationFormat) {
                                throw new RuntimeException("Not implemented");
                            }
                        };
                    }

                    public IContainer getParent() {
                        throw new RuntimeException("Not implemented");
                    }

                    public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public String getPersistentProperty(QualifiedName key) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IProject getProject() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IPath getProjectRelativePath() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IPath getRawLocation() {
                        throw new RuntimeException("Not implemented");
                    }

                    public URI getRawLocationURI() {
                        throw new RuntimeException("Not implemented");
                    }

                    public ResourceAttributes getResourceAttributes() {
                        throw new RuntimeException("Not implemented");
                    }

                    public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public Object getSessionProperty(QualifiedName key) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public int getType() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IWorkspace getWorkspace() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isAccessible() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isDerived() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isDerived(int options) {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isHidden() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isHidden(int options) {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isLinked() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isVirtual() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isLinked(int options) {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isLocal(int depth) {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isPhantom() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isReadOnly() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isSynchronized(int depth) {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isTeamPrivateMember() {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isTeamPrivateMember(int options) {
                        throw new RuntimeException("Not implemented");
                    }

                    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void move(IProjectDescription description, boolean force, boolean keepHistory,
                            IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void revertModificationStamp(long value) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setDerived(boolean isDerived) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setHidden(boolean isHidden) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public long setLocalTimeStamp(long value) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setReadOnly(boolean readOnly) {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void touch(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public Object getAdapter(Class adapter) {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean contains(ISchedulingRule rule) {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isConflicting(ISchedulingRule rule) {
                        throw new RuntimeException("Not implemented");
                    }

                    public void build(int kind, String builderName, Map<String, String> args, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void build(int kind, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void build(IBuildConfiguration config, int kind, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void close(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void create(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IBuildConfiguration getActiveBuildConfig() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IBuildConfiguration getBuildConfig(String configName) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IBuildConfiguration[] getBuildConfigs() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IProjectDescription getDescription() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IFile getFile(String name) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IFolder getFolder(String name) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IProjectNature getNature(String natureId) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IPath getPluginWorkingLocation(IPluginDescriptor plugin) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IPath getWorkingLocation(String id) {
                        throw new RuntimeException("Not implemented");
                    }

                    public IProject[] getReferencedProjects() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public IProject[] getReferencingProjects() {
                        throw new RuntimeException("Not implemented");
                    }

                    public IBuildConfiguration[] getReferencedBuildConfigs(String configName, boolean includeMissing)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean hasBuildConfig(String configName) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean hasNature(String natureId) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isNatureEnabled(String natureId) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public boolean isOpen() {
                        throw new RuntimeException("Not implemented");
                    }

                    public void loadSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void move(IProjectDescription description, boolean force, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void open(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void saveSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setDescription(IProjectDescription description, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    public void setDescription(IProjectDescription description, int updateFlags,
                            IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }
                };
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
