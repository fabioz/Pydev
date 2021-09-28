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
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TokensList;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.structure.OrderedMap;
import org.python.pydev.shared_core.structure.Tuple;

import junit.framework.TestCase;

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

            @Override
            public void endRequests() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public ICodeCompletionASTManager getAstManager() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public TokensList getBuiltinCompletions() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public IModule getBuiltinMod() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public File getCompletionsCacheDir() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public boolean isOkToUse() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public <T> T getAdapter(Class<T> adapter) {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public IInterpreterInfo getProjectInterpreter() throws MisconfigurationException,
                    PythonNatureWithoutProjectException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public IPythonPathNature getPythonPathNature() {
                return new IPythonPathNature() {

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
                    public Map<String, String> getVariableSubstitution() {
                        return variableSubstitution;
                    }

                    @Override
                    public Map<String, String> getVariableSubstitution(boolean b) {
                        return variableSubstitution;
                    }

                    @Override
                    public Set<String> getProjectSourcePathSet(boolean replace) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public String getProjectSourcePath(boolean replace) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public String getProjectExternalSourcePath(boolean replace) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public List<String> getProjectExternalSourcePathAsList(boolean replaceVariables)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public String getOnlyProjectPythonPathStr(boolean b) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter,
                            IInterpreterManager info) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void clearCaches() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IPythonNature getNature() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
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

            @Override
            public int getInterpreterType() throws CoreException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public IInterpreterManager getRelatedInterpreterManager() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public String getVersion(boolean translateIfInterpreter) throws CoreException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public boolean isResourceInPythonpath(IResource resource) throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public boolean isResourceInPythonpath(String resource) throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public boolean isResourceInPythonpathProjectSources(IResource resource, boolean addExternal)
                    throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public boolean isResourceInPythonpathProjectSources(String resource, boolean addExternal)
                    throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public void rebuildPath() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public String resolveModule(File file) throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public String resolveModule(String fileAbsolutePath) throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public String resolveModule(IResource resource) throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public void saveAstManager() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public void clearBuiltinCompletions() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public void clearBuiltinMod() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public void setVersion(String version, String interpreter) throws CoreException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public boolean startRequests() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public void configure() throws CoreException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public void deconfigure() throws CoreException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public IProject getProject() {
                return new IProject() {

                    @Override
                    public boolean exists(IPath path) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResource findMember(String path) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResource findMember(String path, boolean includePhantoms) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResource findMember(IPath path) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResource findMember(IPath path, boolean includePhantoms) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public String getDefaultCharset() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public String getDefaultCharset(boolean checkImplicit) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IFile getFile(IPath path) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IFolder getFolder(IPath path) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResource[] members() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResource[] members(boolean includePhantoms) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResource[] members(int memberFlags) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IFile[] findDeletedMembersWithHistory(int depth, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setDefaultCharset(String charset) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setDefaultCharset(String charset, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResourceFilterDescription createFilter(int type,
                            FileInfoMatcherDescription matcherDescription, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResourceFilterDescription[] getFilters() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void accept(IResourceVisitor visitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void clearHistory(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void copy(IPath destination, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IMarker createMarker(String type) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IResourceProxy createProxy() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean exists() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IMarker findMarker(long id) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public String getFileExtension() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IPath getFullPath() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public long getLocalTimeStamp() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IPath getLocation() {
                        return null;
                    }

                    @Override
                    public URI getLocationURI() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IMarker getMarker(long id) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public long getModificationStamp() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public String getName() {
                        throw new RuntimeException("Not implemented");
                    }

                    public void clearCachedDynamicReferences() {

                    }

                    @Override
                    public IPathVariableManager getPathVariableManager() {
                        return new IPathVariableManager() {

                            @Override
                            public URI convertToRelative(URI path, boolean force, String variableHint)
                                    throws CoreException {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public void setValue(String name, IPath value) throws CoreException {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public void setURIValue(String name, URI value) throws CoreException {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public IPath getValue(String name) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public URI getURIValue(String name) {
                                try {
                                    return new URI("file://".concat(pathSubstitution.get(name)));
                                } catch (Exception e) {
                                    ;
                                }
                                return null;
                            }

                            @Override
                            public String[] getPathVariableNames() {
                                return pathSubstitution.keySet().toArray(new String[] {});
                            }

                            @Override
                            public void addChangeListener(IPathVariableChangeListener listener) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public void removeChangeListener(IPathVariableChangeListener listener) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public URI resolveURI(URI uri) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public IPath resolvePath(IPath path) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public boolean isDefined(String name) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public boolean isUserDefined(String name) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public IStatus validateName(String name) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public IStatus validateValue(IPath path) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public IStatus validateValue(URI path) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public URI getVariableRelativePathLocation(URI location) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public String convertToUserEditableFormat(String value, boolean locationFormat) {
                                throw new RuntimeException("Not implemented");
                            }

                            @Override
                            public String convertFromUserEditableFormat(String value, boolean locationFormat) {
                                throw new RuntimeException("Not implemented");
                            }
                        };
                    }

                    @Override
                    public IContainer getParent() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public String getPersistentProperty(QualifiedName key) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IProject getProject() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IPath getProjectRelativePath() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IPath getRawLocation() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public URI getRawLocationURI() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public ResourceAttributes getResourceAttributes() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public Object getSessionProperty(QualifiedName key) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public int getType() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IWorkspace getWorkspace() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isAccessible() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isDerived() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isDerived(int options) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isHidden() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isHidden(int options) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isLinked() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isVirtual() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isLinked(int options) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isLocal(int depth) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isPhantom() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isReadOnly() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isSynchronized(int depth) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isTeamPrivateMember() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isTeamPrivateMember(int options) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void move(IPath destination, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void move(IProjectDescription description, boolean force, boolean keepHistory,
                            IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void revertModificationStamp(long value) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setDerived(boolean isDerived) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setHidden(boolean isHidden) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public long setLocalTimeStamp(long value) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setReadOnly(boolean readOnly) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void touch(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public <T> T getAdapter(Class<T> adapter) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean contains(ISchedulingRule rule) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isConflicting(ISchedulingRule rule) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void build(int kind, String builderName, Map<String, String> args, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void build(int kind, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void build(IBuildConfiguration config, int kind, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void close(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void create(IProjectDescription description, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void create(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void create(IProjectDescription description, int updateFlags, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void delete(boolean deleteContent, boolean force, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IBuildConfiguration getActiveBuildConfig() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IBuildConfiguration getBuildConfig(String configName) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IBuildConfiguration[] getBuildConfigs() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IContentTypeMatcher getContentTypeMatcher() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IProjectDescription getDescription() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IFile getFile(String name) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IFolder getFolder(String name) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IProjectNature getNature(String natureId) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IPath getWorkingLocation(String id) {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IProject[] getReferencedProjects() throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IProject[] getReferencingProjects() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public IBuildConfiguration[] getReferencedBuildConfigs(String configName, boolean includeMissing)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean hasBuildConfig(String configName) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean hasNature(String natureId) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isNatureEnabled(String natureId) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public boolean isOpen() {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void loadSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void move(IProjectDescription description, boolean force, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void open(int updateFlags, IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void open(IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void saveSnapshot(int options, URI snapshotLocation, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setDescription(IProjectDescription description, IProgressMonitor monitor)
                            throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }

                    @Override
                    public void setDescription(IProjectDescription description, int updateFlags,
                            IProgressMonitor monitor) throws CoreException {
                        throw new RuntimeException("Not implemented");
                    }
                };
            }

            @Override
            public void setProject(IProject project) {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public int getGrammarVersion() throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal)
                    throws CoreException, MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath, boolean addExternal)
                    throws CoreException, MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public void updateMtime() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public long getMtime() {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public Tuple<String, String> getVersionAndError(boolean translateIfInterpreter) throws CoreException {
                throw new RuntimeException("Not implemented");
            }
        });
        return s;
    }
}
