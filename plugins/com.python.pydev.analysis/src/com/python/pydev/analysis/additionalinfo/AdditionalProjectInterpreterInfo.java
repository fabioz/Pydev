/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;

import com.python.pydev.analysis.AnalysisPlugin;


public class AdditionalProjectInterpreterInfo extends AbstractAdditionalInfoWithBuild {

    /**
     * This is the project that contains this info
     */
    private IProject project;
    
    /**
     * holds nature info (project name points to info)
     */
    private static Map<String, AbstractAdditionalDependencyInfo> additionalNatureInfo = new HashMap<String, AbstractAdditionalDependencyInfo>();

    public IProject getProject(){
        return project;
    }
    
    //----------------------------------------------------------------------------- START DELTA RELATED 
    
    /**
     * @return the path to the folder we want to keep things on
     */
    @Override
    protected File getPersistingFolder() {
        try {
            Assert.isNotNull(project);
            return AnalysisPlugin.getStorageDirForProject(project);
        } catch (NullPointerException e) {
            //it may fail in tests... (save it in default folder in this cases)
            Log.log(IStatus.ERROR, "Error getting persisting folder", e, false);
            return new File(".");
        }
    }
    


    //----------------------------------------------------------------------------- END DELTA RELATED
    
    
    public AdditionalProjectInterpreterInfo(IProject project) throws MisconfigurationException {
        super(false);
        this.project = project;
        init();
    }

    private File persistingLocation;
    
    @Override
    protected File getPersistingLocation() {
        if(persistingLocation == null){
            persistingLocation = new File(getPersistingFolder(), "AdditionalProjectInterpreterInfo.pydevinfo");
        }
        return persistingLocation;
    }
    
    @Override
    protected void setAsDefaultInfo() {
        AdditionalProjectInterpreterInfo.setAdditionalInfoForProject(project, this);
    }

    public static void saveAdditionalInfoForProject(IPythonNature nature) throws MisconfigurationException {
        AbstractAdditionalTokensInfo info = getAdditionalInfoForProject(nature);
        info.save();
    }

    public static List<AbstractAdditionalTokensInfo> getAdditionalInfo(IPythonNature nature) throws MisconfigurationException {
        return getAdditionalInfo(nature, true, false);
    }
    
    
    /**
     * @param nature the nature we want to get info on
     * @return all the additional info that is bounded with some nature (including related projects)
     * @throws MisconfigurationException 
     */
    public static List<AbstractAdditionalTokensInfo> getAdditionalInfo(IPythonNature nature, boolean addSystemInfo,
            boolean addReferencingProjects) throws MisconfigurationException {
        List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> infoAndNature = getAdditionalInfoAndNature(nature, addSystemInfo, addReferencingProjects);
        ArrayList<AbstractAdditionalTokensInfo> ret = new ArrayList<AbstractAdditionalTokensInfo>();
        for (Tuple<AbstractAdditionalTokensInfo, IPythonNature> tuple : infoAndNature) {
            ret.add(tuple.o1);
        }
        
        return ret;
    }
    
    
    public static List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> getAdditionalInfoAndNature(
            IPythonNature nature, boolean addSystemInfo, boolean addReferencingProjects) throws MisconfigurationException {
        return getAdditionalInfoAndNature(nature, addSystemInfo, addReferencingProjects, true);
    }
    
    
    public static List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> getAdditionalInfoAndNature(
            IPythonNature nature, boolean addSystemInfo, boolean addReferencingProjects, boolean addReferencedProjects) throws MisconfigurationException {
        
        List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> ret = new ArrayList<Tuple<AbstractAdditionalTokensInfo, IPythonNature>>();
        
        IProject project = nature.getProject();
        
        //get for the system info
        if(addSystemInfo){
            AbstractAdditionalTokensInfo systemInfo;
            try {
                systemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
                        PydevPlugin.getInterpreterManager(nature), nature.getProjectInterpreter().getExecutableOrJar());
            } catch (MisconfigurationException e) {
                throw e;
            }catch(PythonNatureWithoutProjectException e){
                throw new RuntimeException(e);
            }
            ret.add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(
                    systemInfo, new SystemPythonNature(nature.getRelatedInterpreterManager())));
        }
    
        //get for the current project
        if(project != null){
            AbstractAdditionalTokensInfo additionalInfoForProject = getAdditionalInfoForProject(nature);
            if(additionalInfoForProject != null){
                ret.add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(additionalInfoForProject, nature));
            }
            
            try {
                if(addReferencedProjects){
                    //get for the referenced projects
                    Set<IProject> referencedProjects = ProjectModulesManager.getReferencedProjects(project);
                    for (IProject refProject : referencedProjects) {
                        additionalInfoForProject = getAdditionalInfoForProject(PythonNature.getPythonNature(refProject));
                        if(additionalInfoForProject != null){
                            ret.add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(
                                    additionalInfoForProject, PythonNature.getPythonNature(refProject)));
                        }
                    }
                }

                if(addReferencingProjects){
                    Set<IProject> referencingProjects = ProjectModulesManager.getReferencingProjects(project);
                    for (IProject refProject : referencingProjects) {
                        additionalInfoForProject = getAdditionalInfoForProject(PythonNature.getPythonNature(refProject));
                        if(additionalInfoForProject != null){
                            ret.add(new Tuple<AbstractAdditionalTokensInfo, IPythonNature>(
                                    additionalInfoForProject, PythonNature.getPythonNature(refProject)));
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
    public static AbstractAdditionalDependencyInfo getAdditionalInfoForProject(IPythonNature nature) throws MisconfigurationException {
        if(nature == null){
            return null;
        }
        IProject project = nature.getProject();
        if(project == null){
            return null;
        }
        String name = REF.getValidProjectName(project);
        AbstractAdditionalDependencyInfo info = additionalNatureInfo.get(name);
        if(info == null){
            info = new AdditionalProjectInterpreterInfo(project);
            additionalNatureInfo.put(name, info);
        }
        return info;
    }

    /**
     * sets the additional info (overrides if already set)
     * @param project the project we want to set info on
     * @param info the info to set
     */
    public static void setAdditionalInfoForProject(IProject project, AbstractAdditionalDependencyInfo info) {
        additionalNatureInfo.put(REF.getValidProjectName(project), info);
    }

    public static boolean loadAdditionalInfoForProject(IPythonNature nature) throws MisconfigurationException {
        AbstractAdditionalDependencyInfo info = getAdditionalInfoForProject(nature);
        return info.load();
    }


    //interfaces that iterate through all of them
    public static List<IInfo> getTokensEqualTo(String qualifier, IPythonNature nature, int getWhat) throws MisconfigurationException {
        ArrayList<IInfo> ret = new ArrayList<IInfo>();
        List<AbstractAdditionalTokensInfo> additionalInfo = getAdditionalInfo(nature);
        for (AbstractAdditionalTokensInfo info : additionalInfo) {
            ret.addAll(info.getTokensEqualTo(qualifier, getWhat));
        }
        return ret;
    }

    public static List<IInfo> getTokensStartingWith(String qualifier, IPythonNature nature, int getWhat) throws MisconfigurationException {
        ArrayList<IInfo> ret = new ArrayList<IInfo>();
        List<AbstractAdditionalTokensInfo> additionalInfo = getAdditionalInfo(nature);
        for (AbstractAdditionalTokensInfo info : additionalInfo) {
            ret.addAll(info.getTokensStartingWith(qualifier, getWhat));
        }
        return ret;
    }


    /**
     * @param project the project we want to get info on
     * @return a list of the additional info for the project + referencing projects
     * @throws MisconfigurationException 
     */
    public static List<AbstractAdditionalDependencyInfo> getAdditionalInfoForProjectAndReferencing(IPythonNature nature) throws MisconfigurationException {
        List<AbstractAdditionalDependencyInfo> ret = new ArrayList<AbstractAdditionalDependencyInfo>();
        IProject project = nature.getProject();
        if(project == null){
            return ret;
        }
        ret.add(getAdditionalInfoForProject(nature));
        
        Set<IProject> referencingProjects = ProjectModulesManager.getReferencingProjects(project);
        for (IProject p : referencingProjects) {
            AbstractAdditionalDependencyInfo info2 = getAdditionalInfoForProject(PythonNature.getPythonNature(p));
            if(info2 != null){
                ret.add(info2);
            }
        }
        return ret;
    }


    

}
