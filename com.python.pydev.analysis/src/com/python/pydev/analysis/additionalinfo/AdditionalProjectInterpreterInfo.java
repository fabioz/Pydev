/*
 * License: Common Public License v1.0
 * Created on Sep 13, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;


public class AdditionalProjectInterpreterInfo extends AbstractAdditionalDependencyInfo implements IDeltaProcessor<Object> {

    private IProject project;
    /**
     * holds nature info (project name points to info)
     */
    private static Map<String, AbstractAdditionalDependencyInfo> additionalNatureInfo = new HashMap<String, AbstractAdditionalDependencyInfo>();


    
    //----------------------------------------------------------------------------- START DELTA RELATED 
    /**
     * Used to save things in a delta fashion (projects have deltas).
     */
    protected DeltaSaver<Object> deltaSaver;
    
    @Override
    protected void add(IInfo info, boolean generateDelta) {
        super.add(info, generateDelta);
        //after adding any info, we have to save the delta.
        if(generateDelta){
            deltaSaver.addInsertCommand(info);
            checkDeltaSize();
        }
    }

    
    @Override
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        super.removeInfoFromModule(moduleName, generateDelta);
        if(generateDelta){
            this.deltaSaver.addDeleteCommand(moduleName);
            checkDeltaSize();
        }
    }

    @Override
    protected void restoreSavedInfo(Object o) {
        super.restoreSavedInfo(o);
        //when we do a load, we have to process the deltas that may exist
        if(deltaSaver.availableDeltas() > 0){
            deltaSaver.processDeltas(this);
        }
    }

    protected DeltaSaver<Object> createDeltaSaver() {
        return new DeltaSaver<Object>(
                AbstractAdditionalInterpreterInfo.getPersistingFolder(), 
                REF.getValidProjectName(project)+"_projectinfodelta", 
                new ICallback<Object, ObjectInputStream>(){

            public Object call(ObjectInputStream arg) {
                try {
                    return arg.readObject();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }}
        );
    }
    

    
    public void processUpdate(Object data) {
        throw new RuntimeException("There is no update generation, only add.");
    }

    public void processDelete(Object data) {
        //the moduleName is generated on delete
        this.removeInfoFromModule((String) data, false);
    }

    public void processInsert(Object data) {
        //the IInfo token is generated on insert
        this.add((IInfo) data, false);
    }

    public void endProcessing() {
        //save it when the processing is finished
        this.save();
    }
    
    
    /**
     * This is the maximun number of deltas that can be generated before saving everything in a big chunck and 
     * clearing the deltas
     */
    public static final int MAXIMUN_NUMBER_OF_DELTAS = 100;

    /**
     * If the delta size is big enough, save the current state and discard the deltas.
     */
    private void checkDeltaSize() {
        if(deltaSaver.availableDeltas() > MAXIMUN_NUMBER_OF_DELTAS){
            this.save();
            deltaSaver.clearAll();
        }
    }


    //----------------------------------------------------------------------------- END DELTA RELATED
    
    
    
    
    
    
    
    public AdditionalProjectInterpreterInfo(IProject project) {
        this.project = project;
        deltaSaver = createDeltaSaver();
    }

    @Override
    protected File getPersistingLocation() {
        String name = REF.getValidProjectName(project);
        if(name == null || name.trim().length() == 0){
            throw new RuntimeException("The name of the project is not valid: "+project);
        }
        return new File(getPersistingFolder(), name+".pydevinfo");
    }

    @Override
    protected void setAsDefaultInfo() {
        AdditionalProjectInterpreterInfo.setAdditionalInfoForProject(project, this);
    }

    public static void saveAdditionalInfoForProject(IProject project) {
        AbstractAdditionalInterpreterInfo info = getAdditionalInfoForProject(project);
        info.save();
    }

    /**
     * @param nature the nature we want to get info on
     * @return all the additional info that is bounded with some nature (including related projects)
     */
    public static List<AbstractAdditionalInterpreterInfo> getAdditionalInfo(PythonNature nature) {
        List<AbstractAdditionalInterpreterInfo> ret = new ArrayList<AbstractAdditionalInterpreterInfo>();
        IProject project = nature.getProject();
        
        //get for the system info
        AbstractAdditionalInterpreterInfo systemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(PydevPlugin.getInterpreterManager(nature));
        ret.add(systemInfo);
    
        //get for the current project
        AbstractAdditionalInterpreterInfo additionalInfoForProject = getAdditionalInfoForProject(project);
        if(additionalInfoForProject != null){
            ret.add(additionalInfoForProject);
        }
        
        try {
            //get for the referenced projects
            IProject[] referencedProjects = project.getReferencedProjects();
            for (IProject refProject : referencedProjects) {
                
                additionalInfoForProject = getAdditionalInfoForProject(refProject);
                if(additionalInfoForProject != null){
                    ret.add(additionalInfoForProject);
                }
            }
            
            
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    /**
     * @param project the project we want to get info on
     * @return the additional info for a given project (gotten from the cache with its name)
     */
    public static AbstractAdditionalDependencyInfo getAdditionalInfoForProject(IProject project) {
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

    public static boolean loadAdditionalInfoForProject(IProject project) {
        AbstractAdditionalDependencyInfo info = getAdditionalInfoForProject(project);
        return info.load();
    }



}
