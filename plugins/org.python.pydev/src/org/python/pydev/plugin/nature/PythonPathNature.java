/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 2, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin.nature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PropertiesHelper;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.StringSubstitution;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.OrderedMap;

/**
 * @author Fabio Zadrozny
 */
public class PythonPathNature implements IPythonPathNature {

    private volatile IProject fProject;
    private volatile PythonNature fNature;

    /**
     * This is the property that has the python path - associated with the project.
     */
    private static QualifiedName projectSourcePathQualifiedName = null;

    static QualifiedName getProjectSourcePathQualifiedName() {
        if (projectSourcePathQualifiedName == null) {
            projectSourcePathQualifiedName = new QualifiedName(PydevPlugin.getPluginID(), "PROJECT_SOURCE_PATH");
        }
        return projectSourcePathQualifiedName;
    }

    /**
     * This is the property that has the external python path - associated with the project.
     */
    private static QualifiedName projectExternalSourcePathQualifiedName = null;

    static QualifiedName getProjectExternalSourcePathQualifiedName() {
        if (projectExternalSourcePathQualifiedName == null) {
            projectExternalSourcePathQualifiedName = new QualifiedName(PydevPlugin.getPluginID(),
                    "PROJECT_EXTERNAL_SOURCE_PATH");
        }
        return projectExternalSourcePathQualifiedName;
    }

    /**
     * This is the property that has the external python path - associated with the project.
     */
    private static QualifiedName projectVariableSubstitutionQualifiedName = null;

    static QualifiedName getProjectVariableSubstitutionQualifiedName() {
        if (projectVariableSubstitutionQualifiedName == null) {
            projectVariableSubstitutionQualifiedName = new QualifiedName(PydevPlugin.getPluginID(),
                    "PROJECT_VARIABLE_SUBSTITUTION");
        }
        return projectVariableSubstitutionQualifiedName;
    }

    public void setProject(IProject project, IPythonNature nature) {
        this.fProject = project;
        this.fNature = (PythonNature) nature;
    }

    public IPythonNature getNature() {
        return this.fNature;
    }

    private boolean waited = false;

    /**
     * Returns a list of paths with the complete pythonpath for this nature.
     * 
     * This includes the pythonpath for the project, all the referenced projects and the
     * system.
     */
    public List<String> getCompleteProjectPythonPath(IInterpreterInfo interpreter, IInterpreterManager manager) {
        IModulesManager projectModulesManager = getProjectModulesManager();
        if (projectModulesManager == null) {
            if (!waited) {
                waited = true;
                for (int i = 0; i < 10 && projectModulesManager == null; i++) {
                    //We may get into a race condition, so, try to see if we can get it.
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        //OK
                    }
                    projectModulesManager = getProjectModulesManager();
                }
            }
        }
        if (projectModulesManager == null) {
            return null;
        }
        return projectModulesManager.getCompletePythonPath(interpreter, manager);
    }

    private IModulesManager getProjectModulesManager() {
        IPythonNature nature = fNature;
        if (nature == null) {
            return null;
        }

        ICodeCompletionASTManager astManager = nature.getAstManager();
        if (astManager == null) {
            // AST manager might not be yet available
            // Code completion job is scheduled to be run
            return null;
        }

        return astManager.getModulesManager();
    }

    /**
     * @return the project pythonpath with complete paths in the filesystem.
     */
    public String getOnlyProjectPythonPathStr(boolean addExternal) throws CoreException {
        String source = null;
        String external = null;
        String contributed = null;
        IProject project = fProject;
        PythonNature nature = fNature;

        if (project == null || nature == null) {
            return "";
        }

        //Substitute with variables!
        StringSubstitution stringSubstitution = new StringSubstitution(nature);

        source = (String) getProjectSourcePath(true, stringSubstitution, RETURN_STRING_WITH_SEPARATOR);
        if (addExternal) {
            external = getProjectExternalSourcePath(true, stringSubstitution);
        }
        contributed = stringSubstitution.performPythonpathStringSubstitution(getContributedSourcePath(project));

        if (source == null) {
            source = "";
        }
        //we have to work on this one to resolve to full files, as what is stored is the position
        //relative to the project location
        List<String> strings = StringUtils.splitAndRemoveEmptyTrimmed(source, '|');
        FastStringBuffer buf = new FastStringBuffer();

        for (String currentPath : strings) {
            if (currentPath.trim().length() > 0) {
                IPath p = new Path(currentPath);

                if (SharedCorePlugin.inTestMode()) {
                    //in tests
                    buf.append(currentPath);
                    buf.append("|");
                    continue;
                }

                boolean found = false;
                p = p.removeFirstSegments(1); //The first segment should always be the project (historically it's this way, but having it relative would be nicer!?!).
                IResource r = project.findMember(p);
                if (r == null) {
                    r = project.getFolder(p);
                }
                if (r != null) {
                    IPath location = r.getLocation();
                    if (location != null) {
                        found = true;
                        buf.append(FileUtils.getFileAbsolutePath(location.toFile()));
                        buf.append("|");
                    }
                }
                if (!found) {
                    Log.log(IStatus.WARNING, "Unable to find the path " + currentPath + " in the project were it's \n"
                            + "added as a source folder for pydev (project: " + project.getName() + ") member:" + r,
                            null);
                }
            }
        }

        if (external == null) {
            external = "";
        }
        return buf.append("|").append(external).append("|").append(contributed).toString();
    }

    /**
     * Similar to the getOnlyProjectPythonPathStr method above but only for source files (not contributed nor external)
     * and return IResources (zip files or folders).
     */
    public Set<IResource> getProjectSourcePathFolderSet() throws CoreException {
        String source = null;
        IProject project = fProject;
        PythonNature nature = fNature;

        Set<IResource> ret = new HashSet<>();
        if (project == null || nature == null) {
            return ret;
        }

        //Substitute with variables!
        StringSubstitution stringSubstitution = new StringSubstitution(nature);

        source = (String) getProjectSourcePath(true, stringSubstitution, RETURN_STRING_WITH_SEPARATOR);

        if (source == null) {
            return ret;
        }
        //we have to work on this one to resolve to full files, as what is stored is the position
        //relative to the project location
        List<String> strings = StringUtils.splitAndRemoveEmptyTrimmed(source, '|');

        for (String currentPath : strings) {
            if (currentPath.trim().length() > 0) {
                IPath p = new Path(currentPath);
                p = p.removeFirstSegments(1); //The first segment should always be the project (historically it's this way, but having it relative would be nicer!?!).
                IResource r = project.findMember(p);
                if (r == null) {
                    r = project.getFolder(p);
                }
                if (r != null && r.exists()) {
                    ret.add(r);
                }
            }
        }
        return ret;
    }

    /**
     * Gets the source path contributed by plugins.
     * 
     * See: http://sourceforge.net/tracker/index.php?func=detail&aid=1988084&group_id=85796&atid=577329
     * 
     * @throws CoreException
     */
    @SuppressWarnings("unchecked")
    private String getContributedSourcePath(IProject project) throws CoreException {
        FastStringBuffer buff = new FastStringBuffer();
        List<IPythonPathContributor> contributors = ExtensionHelper
                .getParticipants("org.python.pydev.pydev_pythonpath_contrib");
        for (IPythonPathContributor contributor : contributors) {
            String additionalPythonPath = contributor.getAdditionalPythonPath(project);
            if (additionalPythonPath != null && additionalPythonPath.trim().length() > 0) {
                if (buff.length() > 0) {
                    buff.append("|");
                }
                buff.append(additionalPythonPath.trim());
            }
        }
        return buff.toString();
    }

    public void setProjectSourcePath(String newSourcePath) throws CoreException {
        PythonNature nature = fNature;

        if (nature != null) {
            nature.getStore().setPathProperty(PythonPathNature.getProjectSourcePathQualifiedName(), newSourcePath);
        }
    }

    public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException {
        PythonNature nature = fNature;
        if (nature != null) {
            nature.getStore().setPathProperty(PythonPathNature.getProjectExternalSourcePathQualifiedName(),
                    newExternalSourcePath);
        }
    }

    public void setVariableSubstitution(Map<String, String> variableSubstitution) throws CoreException {
        PythonNature nature = fNature;
        if (nature != null) {
            nature.getStore().setMapProperty(PythonPathNature.getProjectVariableSubstitutionQualifiedName(),
                    variableSubstitution);
        }
    }

    public void clearCaches() {
    }

    public Set<String> getProjectSourcePathSet(boolean replace) throws CoreException {
        String projectSourcePath;
        PythonNature nature = fNature;
        if (nature == null) {
            return new HashSet<String>();
        }
        projectSourcePath = getProjectSourcePath(replace);
        return new HashSet<String>(StringUtils.splitAndRemoveEmptyTrimmed(projectSourcePath, '|'));
    }

    public String getProjectSourcePath(boolean replace) throws CoreException {
        return (String) getProjectSourcePath(replace, null, RETURN_STRING_WITH_SEPARATOR);
    }

    @SuppressWarnings("unchecked")
    public OrderedMap<String, String> getProjectSourcePathResolvedToUnresolvedMap() throws CoreException {
        return (OrderedMap<String, String>) getProjectSourcePath(true, null, RETURN_MAP_RESOLVED_TO_UNRESOLVED);
    }

    private static final int RETURN_STRING_WITH_SEPARATOR = 1;
    private static final int RETURN_MAP_RESOLVED_TO_UNRESOLVED = 2;

    /**
     * Function which can take care of getting the paths just for the project (i.e.: without external
     * source folders).
     * 
     * @param replace used only if returnType == RETURN_STRING_WITH_SEPARATOR.
     * 
     * @param substitution the object which will do the string substitutions (only internally used as an optimization as
     * creating the instance may be expensive, so, if some other place already creates it, it can be passed along).
     * 
     * @param returnType if RETURN_STRING_WITH_SEPARATOR returns a string using '|' as the separator. 
     * If RETURN_MAP_RESOLVED_TO_UNRESOLVED returns a map which points from the paths resolved to the maps unresolved.
     */
    private Object getProjectSourcePath(boolean replace, StringSubstitution substitution, int returnType)
            throws CoreException {
        String projectSourcePath;
        boolean restore = false;
        IProject project = fProject;
        PythonNature nature = fNature;

        if (project == null || nature == null) {
            if (returnType == RETURN_STRING_WITH_SEPARATOR) {
                return "";
            } else if (returnType == RETURN_MAP_RESOLVED_TO_UNRESOLVED) {
                return new OrderedMap<String, String>();
            } else {
                throw new AssertionError("Unexpected return: " + returnType);
            }
        }
        projectSourcePath = nature.getStore().getPathProperty(PythonPathNature.getProjectSourcePathQualifiedName());
        if (projectSourcePath == null) {
            //has not been set
            if (returnType == RETURN_STRING_WITH_SEPARATOR) {
                return "";
            } else if (returnType == RETURN_MAP_RESOLVED_TO_UNRESOLVED) {
                return new OrderedMap<String, String>();
            } else {
                throw new AssertionError("Unexpected return: " + returnType);
            }
        }

        if (replace && substitution == null) {
            substitution = new StringSubstitution(fNature);
        }

        //we have to validate it, because as we store the values relative to the workspace, and not to the 
        //project, the path may become invalid (in which case we have to make it compatible again).
        StringBuffer buffer = new StringBuffer();
        List<String> paths = StringUtils.splitAndRemoveEmptyTrimmed(projectSourcePath, '|');
        IPath projectPath = project.getFullPath();
        for (String path : paths) {
            if (path.trim().length() > 0) {
                if (path.indexOf("${") != -1) { //Account for the string substitution.
                    buffer.append(path);
                } else {
                    IPath p = new Path(path);
                    if (p.isEmpty()) {
                        continue; //go to the next...
                    }
                    if (projectPath != null && !projectPath.isPrefixOf(p)) {
                        p = p.removeFirstSegments(1);
                        p = projectPath.append(p);
                        restore = true;
                    }
                    buffer.append(p.toString());
                }
                buffer.append("|");
            }
        }

        //it was wrong and has just been fixed
        if (restore) {
            projectSourcePath = buffer.toString();
            setProjectSourcePath(projectSourcePath);
            if (nature != null) {
                //yeap, everything has to be done from scratch, as all the filesystem paths have just
                //been turned to dust!
                nature.rebuildPath();
            }
        }

        if (returnType == RETURN_STRING_WITH_SEPARATOR) {
            return trimAndReplaceVariablesIfNeeded(replace, projectSourcePath, nature, substitution);

        } else if (returnType == RETURN_MAP_RESOLVED_TO_UNRESOLVED) {
            String ret = StringUtils.leftAndRightTrim(projectSourcePath, '|');
            OrderedMap<String, String> map = new OrderedMap<String, String>();

            List<String> unresolvedVars = StringUtils.splitAndRemoveEmptyTrimmed(ret, '|');

            //Always resolves here!
            List<String> resolved = StringUtils.splitAndRemoveEmptyTrimmed(
                    substitution.performPythonpathStringSubstitution(ret), '|');

            int size = unresolvedVars.size();
            if (size != resolved.size()) {
                throw new AssertionError("Error: expected same size from:\n" + unresolvedVars + "\nand\n" + resolved);
            }
            for (int i = 0; i < size; i++) {
                String un = unresolvedVars.get(i);
                String res = resolved.get(i);
                map.put(res, un);
            }

            return map;
        } else {
            throw new AssertionError("Unexpected return: " + returnType);
        }

    }

    /**
     * Replaces the variables if needed.
     */
    private String trimAndReplaceVariablesIfNeeded(boolean replace, String projectSourcePath, PythonNature nature,
            StringSubstitution substitution)
            throws CoreException {
        String ret = StringUtils.leftAndRightTrim(projectSourcePath, '|');
        if (replace) {
            ret = substitution.performPythonpathStringSubstitution(ret);
        }
        return ret;
    }

    public String getProjectExternalSourcePath(boolean replace) throws CoreException {
        return getProjectExternalSourcePath(replace, null);
    }

    private String getProjectExternalSourcePath(boolean replace, StringSubstitution substitution) throws CoreException {
        String extPath;

        PythonNature nature = fNature;
        if (nature == null) {
            return "";
        }

        //no need to validate because those are always 'file-system' related
        extPath = nature.getStore().getPathProperty(PythonPathNature.getProjectExternalSourcePathQualifiedName());

        if (extPath == null) {
            extPath = "";
        }

        if (replace && substitution == null) {
            substitution = new StringSubstitution(fNature);
        }
        return trimAndReplaceVariablesIfNeeded(replace, extPath, nature, substitution);
    }

    public List<String> getProjectExternalSourcePathAsList(boolean replaceVariables) throws CoreException {
        String projectExternalSourcePath = getProjectExternalSourcePath(replaceVariables);
        List<String> externalPaths = StringUtils.splitAndRemoveEmptyTrimmed(projectExternalSourcePath, '|');
        return externalPaths;
    }

    public Map<String, String> getVariableSubstitution() throws CoreException, MisconfigurationException,
            PythonNatureWithoutProjectException {
        return getVariableSubstitution(true);
    }

    /**
     * Returns the variables in the python nature and in the interpreter.
     */
    public Map<String, String> getVariableSubstitution(boolean addInterpreterInfoSubstitutions) throws CoreException,
            MisconfigurationException, PythonNatureWithoutProjectException {
        PythonNature nature = this.fNature;
        if (nature == null) {
            return new HashMap<String, String>();
        }

        Map<String, String> variableSubstitution;
        if (addInterpreterInfoSubstitutions) {

            IInterpreterInfo info = nature.getProjectInterpreter();
            Properties stringSubstitutionVariables = info.getStringSubstitutionVariables();
            if (stringSubstitutionVariables == null) {
                variableSubstitution = new HashMap<String, String>();
            } else {
                variableSubstitution = PropertiesHelper.createMapFromProperties(stringSubstitutionVariables);
            }
        } else {
            variableSubstitution = new HashMap<String, String>();
        }

        //no need to validate because those are always 'file-system' related
        Map<String, String> variableSubstitution2 = nature.getStore().getMapProperty(
                PythonPathNature.getProjectVariableSubstitutionQualifiedName());
        if (variableSubstitution2 != null) {
            if (variableSubstitution != null) {
                variableSubstitution.putAll(variableSubstitution2);
            } else {
                variableSubstitution = variableSubstitution2;
            }
        }

        //never return null!
        if (variableSubstitution == null) {
            variableSubstitution = new HashMap<String, String>();
        }
        return variableSubstitution;
    }

}
