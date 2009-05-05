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
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
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
    protected void add(IInfo info, boolean generateDelta, int doOn) {
        synchronized (lock) {
            super.add(info, generateDelta, doOn);
            //after adding any info, we have to save the delta.
            if(generateDelta){
                deltaSaver.addInsertCommand(info);
                checkDeltaSize();
            }
        }
    }

    
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
    protected void restoreSavedInfo(Object o) {
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
                "projectinfodelta", 
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
        synchronized (lock) {
            //the moduleName is generated on delete
            this.removeInfoFromModule((String) data, false);
        }
    }
        

    public void processInsert(Object data) {
        synchronized (lock) {
            //the IInfo token is generated on insert
            IInfo info = (IInfo) data;
            if(info.getPath() == null || info.getPath().length() == 0){
                this.add(info, false, TOP_LEVEL);
                
            }else{
                this.add(info, false, INNER);
                
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
     * This is the maximun number of deltas that can be generated before saving everything in a big chunck and 
     * clearing the deltas
     */
    public static final int MAXIMUN_NUMBER_OF_DELTAS = 100;

    /**
     * If the delta size is big enough, save the current state and discard the deltas.
     */
    private void checkDeltaSize() {
        synchronized (lock) {
            if(deltaSaver.availableDeltas() > MAXIMUN_NUMBER_OF_DELTAS){
                this.save();
                deltaSaver.clearAll();
            }
        }
    }


    //----------------------------------------------------------------------------- END DELTA RELATED
    
    
    
    
    
    
    
    public AdditionalProjectInterpreterInfo(IProject project) {
        super(false);
        this.project = project;
        init();
        deltaSaver = createDeltaSaver();
    }

    @Override
    protected File getPersistingLocation() {
        return new File(getPersistingFolder(), "AdditionalProjectInterpreterInfo.pydevinfo");
    }
    
    @Override
    protected void setAsDefaultInfo() {
        AdditionalProjectInterpreterInfo.setAdditionalInfoForProject(project, this);
    }

    public static void saveAdditionalInfoForProject(IPythonNature nature) {
        AbstractAdditionalInterpreterInfo info = getAdditionalInfoForProject(nature);
        info.save();
    }

    public static List<AbstractAdditionalInterpreterInfo> getAdditionalInfo(IPythonNature nature) {
        return getAdditionalInfo(nature, true, false);
    }
    
    
    /**
     * @param nature the nature we want to get info on
     * @return all the additional info that is bounded with some nature (including related projects)
     */
    public static List<AbstractAdditionalInterpreterInfo> getAdditionalInfo(IPythonNature nature, boolean addSystemInfo,
            boolean addReferencingProjects) {
        return getAdditionalInfoAndNature(nature, addSystemInfo, addReferencingProjects).o1;
    }
    
    
    public static Tuple<List<AbstractAdditionalInterpreterInfo>, List<IPythonNature>> getAdditionalInfoAndNature(
            IPythonNature nature, boolean addSystemInfo, boolean addReferencingProjects) {
        
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
                //get for the referenced projects
                IProject[] referencedProjects = project.getReferencedProjects();
                for (IProject refProject : referencedProjects) {
                    additionalInfoForProject = getAdditionalInfoForProject(PythonNature.getPythonNature(refProject));
                    if(additionalInfoForProject != null){
                        ret.add(additionalInfoForProject);
                        natures.add(PythonNature.getPythonNature(refProject));
                    }
                }

                if(addReferencingProjects){
                    IProject[] referencingProjects = project.getReferencingProjects();
                    for (IProject refProject : referencingProjects) {
                        additionalInfoForProject = getAdditionalInfoForProject(PythonNature.getPythonNature(refProject));
                        if(additionalInfoForProject != null){
                            ret.add(additionalInfoForProject);
                            natures.add(PythonNature.getPythonNature(refProject));
                        }
                    }
                }
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
            
        }
        return new Tuple<List<AbstractAdditionalInterpreterInfo>, List<IPythonNature>>(ret, natures);
    }

    /**
     * @param project the project we want to get info on
     * @return the additional info for a given project (gotten from the cache with its name)
     */
    public static AbstractAdditionalDependencyInfo getAdditionalInfoForProject(IPythonNature nature) {
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

    public static boolean loadAdditionalInfoForProject(IPythonNature nature) {
        AbstractAdditionalDependencyInfo info = getAdditionalInfoForProject(nature);
        return info.load();
    }


    //interfaces that iterate through all of them
    public static List<IInfo> getTokensEqualTo(String qualifier, IPythonNature nature, int getWhat) {
        ArrayList<IInfo> ret = new ArrayList<IInfo>();
        List<AbstractAdditionalInterpreterInfo> additionalInfo = getAdditionalInfo(nature);
        for (AbstractAdditionalInterpreterInfo info : additionalInfo) {
            ret.addAll(info.getTokensEqualTo(qualifier, getWhat));
        }
        return ret;
    }

    public static List<IInfo> getTokensStartingWith(String qualifier, IPythonNature nature, int getWhat) {
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
     */
    public static List<AbstractAdditionalDependencyInfo> getAdditionalInfoForProjectAndReferencing(IPythonNature nature) {
        List<AbstractAdditionalDependencyInfo> ret = new ArrayList<AbstractAdditionalDependencyInfo>();
        ret.add(getAdditionalInfoForProject(nature));
        
        IProject project = nature.getProject();
        IProject[] referencingProjects = project.getReferencingProjects();
        for (IProject p : referencingProjects) {
            AbstractAdditionalDependencyInfo info2 = getAdditionalInfoForProject(PythonNature.getPythonNature(p));
            if(info2 != null){
                ret.add(info2);
            }
        }
        return ret;
    }


    

}
