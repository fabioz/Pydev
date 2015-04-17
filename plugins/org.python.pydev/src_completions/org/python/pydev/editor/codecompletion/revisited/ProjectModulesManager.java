/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaProjectModulesManagerCreator;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author Fabio Zadrozny
 */
public final class ProjectModulesManager extends ModulesManagerWithBuild implements IProjectModulesManager {

    private static final boolean DEBUG_MODULES = false;

    //these attributes must be set whenever this class is restored.
    private volatile IProject project;
    private volatile IPythonNature nature;

    public ProjectModulesManager() {
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#setProject(org.eclipse.core.resources.IProject, boolean)
     */
    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas) {
        this.project = project;
        this.nature = nature;
        File completionsCacheDir = this.nature.getCompletionsCacheDir();
        if (completionsCacheDir == null) {
            return; //project was deleted.
        }

        DeltaSaver<ModulesKey> d = this.deltaSaver = new DeltaSaver<ModulesKey>(completionsCacheDir, "v1_astdelta",
                readFromFileMethod,
                toFileMethod);

        if (!restoreDeltas) {
            d.clearAll(); //remove any existing deltas
        } else {
            d.processDeltas(this); //process the current deltas (clears current deltas automatically and saves it when the processing is concluded)
        }
    }

    // ------------------------ delta processing

    /**
     * @see org.python.pydev.core.IProjectModulesManager#endProcessing()
     */
    public void endProcessing() {
        //save it with the updated info
        nature.saveAstManager();
    }

    // ------------------------ end delta processing

    /**
     * @see org.python.pydev.core.IProjectModulesManager#setPythonNature(org.python.pydev.core.IPythonNature)
     */
    public void setPythonNature(IPythonNature nature) {
        this.nature = nature;
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#getNature()
     */
    public IPythonNature getNature() {
        return nature;
    }

    /**
     * @param defaultSelectedInterpreter
     * @see org.python.pydev.core.IProjectModulesManager#getSystemModulesManager()
     */
    public ISystemModulesManager getSystemModulesManager() {
        if (nature == null) {
            Log.log("Nature still not set");
            return null; //still not set (initialization)
        }
        try {
            return nature.getProjectInterpreter().getModulesManager();
        } catch (Exception e1) {
            return null;
        }
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#getAllModuleNames(boolean addDependencies, String partStartingWithLowerCase)
     */
    @Override
    public Set<String> getAllModuleNames(boolean addDependencies, String partStartingWithLowerCase) {
        if (addDependencies) {
            Set<String> s = new HashSet<String>();
            IModulesManager[] managersInvolved = this.getManagersInvolved(true);
            for (int i = 0; i < managersInvolved.length; i++) {
                s.addAll(managersInvolved[i].getAllModuleNames(false, partStartingWithLowerCase));
            }
            return s;
        } else {
            return super.getAllModuleNames(addDependencies, partStartingWithLowerCase);
        }
    }

    /**
     * @return all the modules that start with some token (from this manager and others involved)
     */
    @Override
    public SortedMap<ModulesKey, ModulesKey> getAllModulesStartingWith(String strStartingWith) {
        SortedMap<ModulesKey, ModulesKey> ret = new TreeMap<ModulesKey, ModulesKey>();
        IModulesManager[] managersInvolved = this.getManagersInvolved(true);
        for (int i = 0; i < managersInvolved.length; i++) {
            ret.putAll(managersInvolved[i].getAllDirectModulesStartingWith(strStartingWith));
        }
        return ret;
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#getModule(java.lang.String, org.python.pydev.plugin.nature.PythonNature, boolean)
     */
    @Override
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        return getModule(name, nature, true, dontSearchInit);
    }

    /**
     * When looking for relative, we do not check dependencies
     */
    public IModule getRelativeModule(String name, IPythonNature nature) {
        return super.getModule(false, name, nature, true); //cannot be a compiled module
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#getModule(java.lang.String, org.python.pydev.plugin.nature.PythonNature, boolean, boolean)
     */
    public IModule getModule(String name, IPythonNature nature, boolean checkSystemManager, boolean dontSearchInit) {
        Tuple<IModule, IModulesManager> ret = getModuleAndRelatedModulesManager(name, nature, checkSystemManager,
                dontSearchInit);
        if (ret != null) {
            return ret.o1;
        }
        return null;
    }

    /**
     * @return a tuple with the IModule requested and the IModulesManager that contained that module.
     */
    public Tuple<IModule, IModulesManager> getModuleAndRelatedModulesManager(String name, IPythonNature nature,
            boolean checkSystemManager, boolean dontSearchInit) {

        IModule module = null;

        IModulesManager[] managersInvolved = this.getManagersInvolved(true); //only get the system manager here (to avoid recursion)

        for (IModulesManager m : managersInvolved) {
            if (m instanceof ISystemModulesManager) {
                module = ((ISystemModulesManager) m).getBuiltinModule(name, dontSearchInit);
                if (module != null) {
                    if (DEBUG_MODULES) {
                        System.out.println("Trying to get:" + name + " - " + " returned builtin:" + module + " - "
                                + m.getClass());
                    }
                    return new Tuple<IModule, IModulesManager>(module, m);
                }
            }
        }

        for (IModulesManager m : managersInvolved) {
            if (m instanceof IProjectModulesManager) {
                IProjectModulesManager pM = (IProjectModulesManager) m;
                module = pM.getModuleInDirectManager(name, nature, dontSearchInit);

            } else if (m instanceof ISystemModulesManager) {
                ISystemModulesManager systemModulesManager = (ISystemModulesManager) m;
                module = systemModulesManager.getModuleWithoutBuiltins(name, nature, dontSearchInit);

            } else {
                throw new RuntimeException("Unexpected: " + m);
            }

            if (module != null) {
                if (DEBUG_MODULES) {
                    System.out.println("Trying to get:" + name + " - " + " returned:" + module + " - " + m.getClass());
                }
                return new Tuple<IModule, IModulesManager>(module, m);
            }
        }
        if (DEBUG_MODULES) {
            System.out.println("Trying to get:" + name + " - " + " returned:null - " + this.getClass());
        }
        return null;
    }

    /**
     * Only searches the modules contained in the direct modules manager.
     */
    public IModule getModuleInDirectManager(String name, IPythonNature nature, boolean dontSearchInit) {
        return super.getModule(name, nature, dontSearchInit);
    }

    @Override
    protected String getResolveModuleErr(IResource member) {
        return "Unable to find the path " + member + " in the project were it\n"
                + "is added as a source folder for pydev (project: " + project.getName() + ")";
    }

    public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal) throws CoreException {
        String onlyProjectPythonPathStr = this.nature.getPythonPathNature().getOnlyProjectPythonPathStr(addExternal);
        List<String> pathItems = StringUtils.splitAndRemoveEmptyTrimmed(onlyProjectPythonPathStr, '|');
        List<String> filteredPathItems = filterDuplicatesPreservingOrder(pathItems);
        return this.pythonPathHelper.resolveModule(fileAbsolutePath, false, filteredPathItems, project);
    }

    private List<String> filterDuplicatesPreservingOrder(List<String> pathItems) {
        return new ArrayList<>(new LinkedHashSet<>(pathItems));
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#resolveModule(java.lang.String)
     */
    @Override
    public String resolveModule(String full) {
        return resolveModule(full, true);
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#resolveModule(java.lang.String, boolean)
     */
    @Override
    public String resolveModule(String full, boolean checkSystemManager) {
        IModulesManager[] managersInvolved = this.getManagersInvolved(checkSystemManager);
        for (IModulesManager m : managersInvolved) {

            String mod;
            if (m instanceof IProjectModulesManager) {
                IProjectModulesManager pM = (IProjectModulesManager) m;
                mod = pM.resolveModuleInDirectManager(full);

            } else {
                mod = m.resolveModule(full);
            }

            if (mod != null) {
                return mod;
            }
        }
        return null;
    }

    public String resolveModuleInDirectManager(String full) {
        if (nature != null) {
            return pythonPathHelper.resolveModule(full, false, nature.getProject());
        }
        return super.resolveModule(full);
    }

    public String resolveModuleInDirectManager(IFile member) {
        File inOs = member.getRawLocation().toFile();
        return resolveModuleInDirectManager(FileUtils.getFileAbsolutePath(inOs));
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#getSize(boolean)
     */
    @Override
    public int getSize(boolean addDependenciesSize) {
        if (addDependenciesSize) {
            int size = 0;
            IModulesManager[] managersInvolved = this.getManagersInvolved(true);
            for (int i = 0; i < managersInvolved.length; i++) {
                size += managersInvolved[i].getSize(false);
            }
            return size;
        } else {
            return super.getSize(addDependenciesSize);
        }
    }

    /**
     * @see org.python.pydev.core.IProjectModulesManager#getBuiltins()
     */
    public String[] getBuiltins() {
        String[] builtins = null;
        ISystemModulesManager systemModulesManager = getSystemModulesManager();
        if (systemModulesManager != null) {
            builtins = systemModulesManager.getBuiltins();
        }
        return builtins;
    }

    /**
     * @param checkSystemManager whether the system manager should be added
     * @param referenced true if we should get the referenced projects
     *                   false if we should get the referencing projects
     * @return the Managers that this project references or the ones that reference this project (depends on 'referenced')
     *
     * Change in 1.3.3: adds itself to the list of returned managers
     */
    private synchronized IModulesManager[] getManagers(boolean checkSystemManager, boolean referenced) {
        CompletionCache localCompletionCache = this.completionCache;
        if (localCompletionCache != null) {
            IModulesManager[] ret = localCompletionCache.getManagers(referenced);
            if (ret != null) {
                return ret;
            }
        }
        ArrayList<IModulesManager> list = new ArrayList<IModulesManager>();
        ISystemModulesManager systemModulesManager = getSystemModulesManager();

        //add itself 1st
        list.add(this);

        //get the projects 1st
        if (project != null) {
            IModulesManager javaModulesManagerForProject = JavaProjectModulesManagerCreator
                    .createJavaProjectModulesManagerIfPossible(project);

            if (javaModulesManagerForProject != null) {
                list.add(javaModulesManagerForProject);
            }

            Set<IProject> projs;
            if (referenced) {
                projs = getReferencedProjects(project);
            } else {
                projs = getReferencingProjects(project);
            }
            addModuleManagers(list, projs);
        }

        //the system is the last one we add
        //http://sourceforge.net/tracker/index.php?func=detail&aid=1687018&group_id=85796&atid=577329
        if (checkSystemManager && systemModulesManager != null) {
            //may be null in initialization or if the project does not have a related interpreter manager at the present time
            //(i.e.: misconfigured project)

            list.add(systemModulesManager);
        }

        IModulesManager[] ret = list.toArray(new IModulesManager[list.size()]);
        if (localCompletionCache != null) {
            localCompletionCache.setManagers(ret, referenced);
        }
        return ret;
    }

    public static Set<IProject> getReferencingProjects(IProject project) {
        HashSet<IProject> memo = new HashSet<IProject>();
        getProjectsRecursively(project, false, memo);
        memo.remove(project); //shouldn't happen unless we've a cycle...
        return memo;
    }

    public static Set<IProject> getReferencedProjects(IProject project) {
        HashSet<IProject> memo = new HashSet<IProject>();
        getProjectsRecursively(project, true, memo);
        memo.remove(project); //shouldn't happen unless we've a cycle...
        return memo;
    }

    /**
     * @param project the project for which we want references.
     * @param referenced whether we want to get the referenced projects or the ones referencing this one.
     * @param memo (out) this is the place where all the projects will e available.
     *
     * Note: the project itself will not be added.
     */
    private static void getProjectsRecursively(IProject project, boolean referenced, HashSet<IProject> memo) {
        IProject[] projects = null;
        try {
            if (project == null || !project.isOpen() || !project.exists() || memo.contains(projects)) {
                return;
            }
            if (referenced) {
                projects = project.getReferencedProjects();
            } else {
                projects = project.getReferencingProjects();
            }
        } catch (CoreException e) {
            //ignore (it's closed)
        }

        if (projects != null) {
            for (IProject p : projects) {
                if (!memo.contains(p)) {
                    memo.add(p);
                    getProjectsRecursively(p, referenced, memo);
                }
            }
        }
    }

    /**
     * @param list the list that will be filled with the managers
     * @param projects the projects that should have the managers added
     */
    private void addModuleManagers(ArrayList<IModulesManager> list, Collection<IProject> projects) {
        for (IProject project : projects) {
            PythonNature nature = PythonNature.getPythonNature(project);
            if (nature != null) {
                ICodeCompletionASTManager otherProjectAstManager = nature.getAstManager();
                if (otherProjectAstManager != null) {
                    IModulesManager projectModulesManager = otherProjectAstManager.getModulesManager();
                    if (projectModulesManager != null) {
                        list.add(projectModulesManager);
                    }
                } else {
                    //Removed the warning below: this may be common when starting up...
                    //String msg = "No ast manager configured for :" + project.getName();
                    //Log.log(IStatus.WARNING, msg, new RuntimeException(msg));
                }
            }
            IModulesManager javaModulesManagerForProject = JavaProjectModulesManagerCreator
                    .createJavaProjectModulesManagerIfPossible(project);
            if (javaModulesManagerForProject != null) {
                list.add(javaModulesManagerForProject);
            }
        }
    }

    /**
     * @return Returns the managers that this project references, including itself.
     */
    public IModulesManager[] getManagersInvolved(boolean checkSystemManager) {
        return getManagers(checkSystemManager, true);
    }

    /**
     * @return Returns the managers that reference this project, including itself.
     */
    public IModulesManager[] getRefencingManagersInvolved(boolean checkSystemManager) {
        return getManagers(checkSystemManager, false);
    }

    /**
     * Helper to work as a timer to know when to check for pythonpath consistencies.
     */
    private volatile long checkedPythonpathConsistency = 0;

    /**
     * @see org.python.pydev.core.IProjectModulesManager#getCompletePythonPath()
     */
    public List<String> getCompletePythonPath(IInterpreterInfo interpreter, IInterpreterManager manager) {
        List<String> l = new ArrayList<String>();
        IModulesManager[] managersInvolved = getManagersInvolved(true);
        for (IModulesManager m : managersInvolved) {
            if (m instanceof ISystemModulesManager) {
                ISystemModulesManager systemModulesManager = (ISystemModulesManager) m;
                l.addAll(systemModulesManager.getCompletePythonPath(interpreter, manager));

            } else {
                PythonPathHelper h = (PythonPathHelper) m.getPythonPathHelper();
                if (h != null) {
                    List<String> pythonpath = h.getPythonpath();

                    //Note: this was previously only l.addAll(pythonpath), and was changed to the code below as a place
                    //to check for consistencies in the pythonpath stored in the pythonpath helper and the pythonpath
                    //available in the PythonPathNature (in general, when requesting it the PythonPathHelper should be
                    //used, as it's a cache for the resolved values of the PythonPathNature).

                    boolean forceCheck = false;
                    ProjectModulesManager m2 = null;
                    String onlyProjectPythonPathStr = null;
                    if (m instanceof ProjectModulesManager) {
                        long currentTimeMillis = System.currentTimeMillis();
                        m2 = (ProjectModulesManager) m;
                        //check at most once every 20 seconds (or every time if the pythonpath is empty... in which case
                        //it should be fast to get it too if it's consistent).
                        if (pythonpath.size() == 0 || currentTimeMillis - m2.checkedPythonpathConsistency > 20 * 1000) {
                            try {
                                IPythonNature n = m.getNature();
                                if (n != null) {
                                    IPythonPathNature pythonPathNature = n.getPythonPathNature();
                                    if (pythonPathNature != null) {
                                        onlyProjectPythonPathStr = pythonPathNature.getOnlyProjectPythonPathStr(true);
                                        m2.checkedPythonpathConsistency = currentTimeMillis;
                                        forceCheck = true;
                                    }
                                }
                            } catch (Exception e) {
                                Log.log(e);
                            }
                        }
                    }

                    if (forceCheck) {
                        //Check if it's actually correct and auto-fix if it's not.
                        List<String> parsed = PythonPathHelper.parsePythonPathFromStr(onlyProjectPythonPathStr, null);
                        if (m2.nature != null && !new HashSet<String>(parsed).equals(new HashSet<String>(pythonpath))) {
                            // Make it right at this moment (so any other place that calls it before the restore
                            //takes place has the proper version).
                            h.setPythonPath(parsed);

                            // Force a rebuild as the PythonPathHelper paths are not up to date.
                            m2.nature.rebuildPath();
                        }
                        l.addAll(parsed); //add the proper paths
                    } else {
                        l.addAll(pythonpath);
                    }
                }
            }
        }
        return l;
    }
}
