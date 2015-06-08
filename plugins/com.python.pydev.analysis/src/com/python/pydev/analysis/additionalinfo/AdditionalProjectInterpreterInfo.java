/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 13, 2005
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.system_info_builder.InterpreterInfoBuilder;

public class AdditionalProjectInterpreterInfo extends AbstractAdditionalInfoWithBuild {

    /**
     * This is the project that contains this info
     */
    private final IProject project;

    private final File persistingFolder;

    private final File persistingLocation;

    /**
     * holds nature info (project name points to info)
     */
    private final static Map<String, AbstractAdditionalDependencyInfo> additionalNatureInfo = new HashMap<String, AbstractAdditionalDependencyInfo>();

    private final static Object additionalNatureInfoLock = new Object();

    public IProject getProject() {
        return project;
    }

    /**
     * @return the path to the folder we want to keep things on
     */
    @Override
    protected File getPersistingFolder() {
        return persistingFolder;
    }

    @Override
    protected File getPersistingLocation() {
        return persistingLocation;
    }

    public AdditionalProjectInterpreterInfo(IProject project) throws MisconfigurationException {
        super(false);
        Assert.isNotNull(project);
        this.project = project;

        File f;
        try {
            f = AnalysisPlugin.getStorageDirForProject(project);
        } catch (NullPointerException e) {
            //it may fail in tests... (save it in default folder in this cases)
            Log.logInfo("Error getting persisting folder", e);
            f = new File(".");
        }
        persistingFolder = f;

        persistingLocation = new File(persistingFolder, "AdditionalProjectInterpreterInfo.pydevinfo");

        init();
    }

    @Override
    protected String getUIRepresentation() {
        return project != null ? project.getName() : "Unknown project";
    }

    @Override
    protected Set<String> getPythonPathFolders() {
        PythonNature pythonNature = PythonNature.getPythonNature(project);
        IPythonPathNature pythonPathNature = pythonNature.getPythonPathNature();
        Set<String> ret = new HashSet<>();
        try {
            ret.addAll(StringUtils.split(pythonPathNature.getOnlyProjectPythonPathStr(true), "|"));
        } catch (CoreException e) {
            Log.log(e);
        }
        return ret;
    }

    public static List<AbstractAdditionalTokensInfo> getAdditionalInfo(IPythonNature nature)
            throws MisconfigurationException {
        return getAdditionalInfo(nature, true, false);
    }

    /**
     * @param nature the nature we want to get info on
     * @return all the additional info that is bounded with some nature (including related projects)
     * @throws MisconfigurationException
     */
    public static List<AbstractAdditionalTokensInfo> getAdditionalInfo(IPythonNature nature, boolean addSystemInfo,
            boolean addReferencingProjects) throws MisconfigurationException {
        List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> infoAndNature = getAdditionalInfoAndNature(nature,
                addSystemInfo, addReferencingProjects);
        ArrayList<AbstractAdditionalTokensInfo> ret = new ArrayList<AbstractAdditionalTokensInfo>();
        for (Tuple<AbstractAdditionalTokensInfo, IPythonNature> tuple : infoAndNature) {
            ret.add(tuple.o1);
        }

        return ret;
    }

    public static List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> getAdditionalInfoAndNature(
            IPythonNature nature, boolean addSystemInfo, boolean addReferencingProjects)
                    throws MisconfigurationException {
        return getAdditionalInfoAndNature(nature, addSystemInfo, addReferencingProjects, true);
    }

    public static List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> getAdditionalInfoAndNature(
            IPythonNature nature, boolean addSystemInfo, boolean addReferencingProjects, boolean addReferencedProjects)
                    throws MisconfigurationException {

        List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> ret = new ArrayList<Tuple<AbstractAdditionalTokensInfo, IPythonNature>>();

        IProject project = nature.getProject();

        //get for the system info
        if (addSystemInfo) {
            AbstractAdditionalTokensInfo systemInfo;
            try {
                systemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
                        PydevPlugin.getInterpreterManager(nature), nature.getProjectInterpreter().getExecutableOrJar());
            } catch (MisconfigurationException e) {
                throw e;
            } catch (PythonNatureWithoutProjectException e) {
                throw new RuntimeException(e);
            }
            ret.add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(systemInfo, new SystemPythonNature(nature
                    .getRelatedInterpreterManager())));
        }

        //get for the current project
        if (project != null) {
            AbstractAdditionalTokensInfo additionalInfoForProject = getAdditionalInfoForProject(nature);
            if (additionalInfoForProject != null) {
                ret.add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(additionalInfoForProject, nature));
            }

            try {
                if (addReferencedProjects) {
                    //get for the referenced projects
                    Set<IProject> referencedProjects = ProjectModulesManager.getReferencedProjects(project);
                    for (IProject refProject : referencedProjects) {
                        additionalInfoForProject = getAdditionalInfoForProject(
                                PythonNature.getPythonNature(refProject));
                        if (additionalInfoForProject != null) {
                            ret.add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(additionalInfoForProject,
                                    PythonNature.getPythonNature(refProject)));
                        }
                    }
                }

                if (addReferencingProjects) {
                    Set<IProject> referencingProjects = ProjectModulesManager.getReferencingProjects(project);
                    for (IProject refProject : referencingProjects) {
                        additionalInfoForProject = getAdditionalInfoForProject(
                                PythonNature.getPythonNature(refProject));
                        if (additionalInfoForProject != null) {
                            ret.add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(additionalInfoForProject,
                                    PythonNature.getPythonNature(refProject)));
                        }
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }

        }
        return ret;
    }

    /**
     * @param project the project we want to get info on
     * @return the additional info for a given project (gotten from the cache with its name)
     * @throws MisconfigurationException
     */
    public static AbstractAdditionalDependencyInfo getAdditionalInfoForProject(final IPythonNature nature)
            throws MisconfigurationException {
        if (nature == null) {
            return null;
        }
        IProject project = nature.getProject();
        if (project == null) {
            return null;
        }
        String name = FileUtilsFileBuffer.getValidProjectName(project);

        synchronized (additionalNatureInfoLock) {
            AbstractAdditionalDependencyInfo info = additionalNatureInfo.get(name);
            if (info == null) {
                info = new AdditionalProjectInterpreterInfo(project);
                additionalNatureInfo.put(name, info);

                if (!info.load()) {
                    recreateAllInfo(nature, new NullProgressMonitor());
                } else {
                    final AbstractAdditionalDependencyInfo temp = info;
                    temp.setWaitForIntegrityCheck(true);
                    //Ok, after it's loaded the first time, check the index integrity!
                    Job j = new Job("Check index integrity for: " + project.getName()) {

                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                new InterpreterInfoBuilder().syncInfoToPythonPath(monitor, nature);
                            } finally {
                                temp.setWaitForIntegrityCheck(false);
                            }
                            return Status.OK_STATUS;
                        }
                    };
                    j.setPriority(Job.INTERACTIVE);
                    j.setSystem(true);
                    j.schedule();
                }

            }
            return info;
        }
    }

    //interfaces that iterate through all of them
    public static List<IInfo> getTokensEqualTo(String qualifier, IPythonNature nature, int getWhat)
            throws MisconfigurationException {
        ArrayList<IInfo> ret = new ArrayList<IInfo>(50);
        List<AbstractAdditionalTokensInfo> additionalInfo = getAdditionalInfo(nature);
        for (AbstractAdditionalTokensInfo info : additionalInfo) {
            info.getTokensEqualTo(qualifier, getWhat, ret);
        }
        return ret;
    }

    public static List<IInfo> getTokensStartingWith(String qualifier, IPythonNature nature, int getWhat)
            throws MisconfigurationException {
        ArrayList<IInfo> ret = new ArrayList<IInfo>();
        List<AbstractAdditionalTokensInfo> additionalInfo = getAdditionalInfo(nature);
        for (AbstractAdditionalTokensInfo info : additionalInfo) {
            info.getTokensStartingWith(qualifier, getWhat, ret);
        }
        return ret;
    }

    /**
     * @param project the project we want to get info on
     * @return a list of the additional info for the project + referencing projects
     * @throws MisconfigurationException
     */
    public static List<AbstractAdditionalDependencyInfo> getAdditionalInfoForProjectAndReferencing(IPythonNature nature)
            throws MisconfigurationException {
        List<AbstractAdditionalDependencyInfo> ret = new ArrayList<AbstractAdditionalDependencyInfo>();
        IProject project = nature.getProject();
        if (project == null) {
            return ret;
        }
        ret.add(getAdditionalInfoForProject(nature));

        Set<IProject> referencingProjects = ProjectModulesManager.getReferencingProjects(project);
        for (IProject p : referencingProjects) {
            AbstractAdditionalDependencyInfo info2 = getAdditionalInfoForProject(PythonNature.getPythonNature(p));
            if (info2 != null) {
                ret.add(info2);
            }
        }
        return ret;
    }

    public static void recreateAllInfo(IPythonNature nature, IProgressMonitor monitor) {
        try {
            synchronized (additionalNatureInfoLock) {
                //Note: at this point we're 100% certain that the ast manager is there.
                IModulesManager m = nature.getAstManager().getModulesManager();
                IProject project = nature.getProject();

                AbstractAdditionalDependencyInfo currInfo = AdditionalProjectInterpreterInfo
                        .getAdditionalInfoForProject(nature);
                if (currInfo != null) {
                    currInfo.clearAllInfo();
                    currInfo.dispose();
                }

                String feedback = "(project:" + project.getName() + ")";
                synchronized (m) {
                    AbstractAdditionalDependencyInfo info = (AbstractAdditionalDependencyInfo) restoreInfoForModuleManager(
                            monitor, m, feedback, new AdditionalProjectInterpreterInfo(project), nature,
                            nature.getGrammarVersion());

                    if (info != null) {
                        //ok, set it and save it
                        additionalNatureInfo.put(FileUtilsFileBuffer.getValidProjectName(project), info);
                        info.save();
                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }
    }

    //Make it available for being in a HashSet.

    @Override
    public int hashCode() {
        return getProject().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AdditionalProjectInterpreterInfo)) {
            return false;
        }
        AdditionalProjectInterpreterInfo additionalProjectInterpreterInfo = (AdditionalProjectInterpreterInfo) obj;
        return this.getProject().equals(additionalProjectInterpreterInfo.getProject());
    }
}
