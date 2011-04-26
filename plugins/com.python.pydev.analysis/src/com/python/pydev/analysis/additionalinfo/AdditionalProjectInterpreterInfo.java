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
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.AnalysisPlugin;


public class AdditionalProjectInterpreterInfo extends AbstractAdditionalDependencyInfo implements IDeltaProcessor<Object> {

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
     * Used to save things in a delta fashion (projects have deltas).
     */
    protected DeltaSaver<Object> deltaSaver;
    
    
    @Override
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        synchronized (lock) {
            super.removeInfoFromModule(moduleName, generateDelta);
            if(generateDelta){
                this.deltaSaver.addDeleteCommand(moduleName);
                checkDeltaSize();
            }
        }
    }

    @Override
    protected void restoreSavedInfo(Object o) throws MisconfigurationException {
        synchronized (lock) {
            super.restoreSavedInfo(o);
            //when we do a load, we have to process the deltas that may exist
            if(deltaSaver.availableDeltas() > 0){
                deltaSaver.processDeltas(this);
            }
        }
    }

    /**
     * @return the path to the folder we want to keep things on
     */
    protected File getPersistingFolder() {
        try {
            Assert.isNotNull(project);
            return AnalysisPlugin.getStorageDirForProject(project);
        } catch (NullPointerException e) {
            //it may fail in tests... (save it in default folder in this cases)
            PydevPlugin.log(IStatus.ERROR, "Error getting persisting folder", e, false);
            return new File(".");
        }
    }

    protected DeltaSaver<Object> createDeltaSaver() {
        return new DeltaSaver<Object>(
                getPersistingFolder(), 
                "v1_projectinfodelta", 
                new ICallback<Object, String>(){

                    public Object call(String arg) {
                        if(arg.startsWith("STR")){
                            return arg.substring(3);
                        }
                        if(arg.startsWith("LST")){
                            return InfoStrFactory.strToInfo(arg.substring(3));
                        }
                        
                        throw new AssertionError("Expecting string starting with STR or LST");
                    }}, 
                    
                new ICallback<String, Object>() {

                    /**
                     * Here we'll convert the object we added to a string.
                     * 
                     * The objects we can add are:
                     * List<IInfo> -- on addition
                     * String (module name) -- on deletion
                     */
                    public String call(Object arg) {
                        if(arg instanceof String){
                            return "STR"+(String) arg;
                        }
                        if(arg instanceof List){
                            List<IInfo> l = (List<IInfo>) arg;
                            String infoToString = InfoStrFactory.infoToString(l);
                            FastStringBuffer buf = new FastStringBuffer("LST", infoToString.length());
                            buf.append(infoToString);
                            return buf.toString();
                        }
                        throw new AssertionError("Expecting List<IInfo> or String.");
                    }
                }
        );
    }
    

    
    public void processUpdate(Object data) {
        throw new RuntimeException("There is no update generation, only add.");
    }

    public void processDelete(Object data) {
        synchronized (lock) {
            //the moduleName is generated on delete
            this.removeInfoFromModule((String) data, false);
        }
    }
        

    public void processInsert(Object data) {
        synchronized (lock) {
        	if(data instanceof IInfo){
        		//backward compatibility
        		//the IInfo token is generated on insert
	            IInfo info = (IInfo) data;
	            if(info.getPath() == null || info.getPath().length() == 0){
	                this.add(info, TOP_LEVEL);
	                
	            }else{
	                this.add(info, INNER);
	                
	            }
        	}else if(data instanceof List){
        		//current way (saves a list of iinfo)
        		for(IInfo info : (List<IInfo>) data){
	        		if(info.getPath() == null || info.getPath().length() == 0){
	        			this.add(info, TOP_LEVEL);
	        			
	        		}else{
	        			this.add(info, INNER);
	        			
	        		}
        		}
        	}
        }
    }

    public void endProcessing() {
        //save it when the processing is finished
        synchronized (lock) {
            this.save();
        }
    }
    
    
    /**
     * This is the maximum number of deltas that can be generated before saving everything in a big chunk and 
     * clearing the deltas. 50 means that it's something as 25 modules (because usually a module change
     * is composed of a delete and an addition). 
     */
    public static final int MAXIMUN_NUMBER_OF_DELTAS = 50;

    /**
     * If the delta size is big enough, save the current state and discard the deltas.
     */
    private void checkDeltaSize() {
        synchronized (lock) {
            if(deltaSaver.availableDeltas() > MAXIMUN_NUMBER_OF_DELTAS){
                this.save();
            }
        }
    }

    /**
     * Whenever it's properly saved, clear all the deltas.
     */
    public void save() {
    	synchronized (lock) {
	    	super.save();
	    	deltaSaver.clearAll();
    	}
    }
    
    
    @Override
    public List<IInfo> addAstInfo(SimpleNode node, String moduleName, IPythonNature nature,
    		boolean generateDelta) {
    	List<IInfo> addAstInfo = super.addAstInfo(node, moduleName, nature, generateDelta);
    	if(generateDelta && addAstInfo.size() > 0){
    		deltaSaver.addInsertCommand(addAstInfo);
    	}
    	return addAstInfo;
    }

    //----------------------------------------------------------------------------- END DELTA RELATED
    
    
    
    
    
    
    
    public AdditionalProjectInterpreterInfo(IProject project) throws MisconfigurationException {
        super(false);
        this.project = project;
        init();
        deltaSaver = createDeltaSaver();
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
        AbstractAdditionalInterpreterInfo info = getAdditionalInfoForProject(nature);
        info.save();
    }

    public static List<AbstractAdditionalInterpreterInfo> getAdditionalInfo(IPythonNature nature) throws MisconfigurationException {
        return getAdditionalInfo(nature, true, false);
    }
    
    
    /**
     * @param nature the nature we want to get info on
     * @return all the additional info that is bounded with some nature (including related projects)
     * @throws MisconfigurationException 
     */
    public static List<AbstractAdditionalInterpreterInfo> getAdditionalInfo(IPythonNature nature, boolean addSystemInfo,
            boolean addReferencingProjects) throws MisconfigurationException {
        return getAdditionalInfoAndNature(nature, addSystemInfo, addReferencingProjects).o1;
    }
    
    
    public static Tuple<List<AbstractAdditionalInterpreterInfo>, List<IPythonNature>> getAdditionalInfoAndNature(
            IPythonNature nature, boolean addSystemInfo, boolean addReferencingProjects) throws MisconfigurationException {
        return getAdditionalInfoAndNature(nature, addSystemInfo, addReferencingProjects, true);
    }
    
    
    public static Tuple<List<AbstractAdditionalInterpreterInfo>, List<IPythonNature>> getAdditionalInfoAndNature(
            IPythonNature nature, boolean addSystemInfo, boolean addReferencingProjects, boolean addReferencedProjects) throws MisconfigurationException {
        
        List<AbstractAdditionalInterpreterInfo> ret = new ArrayList<AbstractAdditionalInterpreterInfo>();
        List<IPythonNature> natures = new ArrayList<IPythonNature>();
        
        IProject project = nature.getProject();
        
        //get for the system info
        if(addSystemInfo){
            AbstractAdditionalInterpreterInfo systemInfo;
            try {
                systemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
                        PydevPlugin.getInterpreterManager(nature), nature.getProjectInterpreter().getExecutableOrJar());
            } catch (MisconfigurationException e) {
                throw e;
            }catch(PythonNatureWithoutProjectException e){
                throw new RuntimeException(e);
            }
            ret.add(systemInfo);
        }
    
        //get for the current project
        if(project != null){
            AbstractAdditionalInterpreterInfo additionalInfoForProject = getAdditionalInfoForProject(nature);
            if(additionalInfoForProject != null){
                ret.add(additionalInfoForProject);
                natures.add(nature);
            }
            
            try {
                if(addReferencedProjects){
                    //get for the referenced projects
                    Set<IProject> referencedProjects = ProjectModulesManager.getReferencedProjects(project);
                    for (IProject refProject : referencedProjects) {
                        additionalInfoForProject = getAdditionalInfoForProject(PythonNature.getPythonNature(refProject));
                        if(additionalInfoForProject != null){
                            ret.add(additionalInfoForProject);
                            natures.add(PythonNature.getPythonNature(refProject));
                        }
                    }
                }

                if(addReferencingProjects){
                    Set<IProject> referencingProjects = ProjectModulesManager.getReferencingProjects(project);
                    for (IProject refProject : referencingProjects) {
                        additionalInfoForProject = getAdditionalInfoForProject(PythonNature.getPythonNature(refProject));
                        if(additionalInfoForProject != null){
                            ret.add(additionalInfoForProject);
                            natures.add(PythonNature.getPythonNature(refProject));
                        }
                    }
                }
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
            
        }
        return new Tuple<List<AbstractAdditionalInterpreterInfo>, List<IPythonNature>>(ret, natures);
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
        List<AbstractAdditionalInterpreterInfo> additionalInfo = getAdditionalInfo(nature);
        for (AbstractAdditionalInterpreterInfo info : additionalInfo) {
            ret.addAll(info.getTokensEqualTo(qualifier, getWhat));
        }
        return ret;
    }

    public static List<IInfo> getTokensStartingWith(String qualifier, IPythonNature nature, int getWhat) throws MisconfigurationException {
        ArrayList<IInfo> ret = new ArrayList<IInfo>();
        List<AbstractAdditionalInterpreterInfo> additionalInfo = getAdditionalInfo(nature);
        for (AbstractAdditionalInterpreterInfo info : additionalInfo) {
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
        ret.add(getAdditionalInfoForProject(nature));
        
        IProject project = nature.getProject();
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
