/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author Fabio Zadrozny
 */
public class ProjectModulesManager extends ModulesManager implements IDeltaProcessor<ModulesKey>{

    /**
     * This is the maximun number of deltas that can be generated before saving everything in a big chunck and 
     * clearing the deltas
     */
    public static final int MAXIMUN_NUMBER_OF_DELTAS = 100;
    
    private static final long serialVersionUID = 1L;
    //these attributes must be set whenever this class is restored.
    private transient IProject project;
    private transient IPythonNature nature;
    
    /**
     * Used to process deltas (in case we have the process killed for some reason)
     */
    private transient DeltaSaver<ModulesKey> deltaSaver;
    
    /**
     * Set the project this modules manager works with.
     * 
     * @param project the project related to this manager
     * @param restoreDeltas says whether deltas should be restored (if they are not, they should be discarded)
     */
    public void setProject(IProject project, boolean restoreDeltas){
        this.project = project;
        this.nature = PythonNature.getPythonNature(project);
        this.deltaSaver = new DeltaSaver<ModulesKey>(this.nature.getCompletionsCacheDir(), "astdelta", new ICallback<Object, ObjectInputStream>(){

            public ModulesKey call(ObjectInputStream arg) {
                try {
                    return (ModulesKey) arg.readObject();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }});
        
        if(!restoreDeltas){
            deltaSaver.clearAll(); //remove any existing deltas
        }else{
            deltaSaver.processDeltas(this); //process the current deltas (clears current deltas automatically and saves it when the processing is concluded)
        }
    }
    
    
    // ------------------------ delta processing
    

    public void processUpdate(ModulesKey data) {
        //updates are ignored because we always start with 'empty modules' (so, we don't actually generate them -- updates are treated as inserts).
        throw new RuntimeException("Not impl");
    }

    public void processDelete(ModulesKey key) {
        super.doRemoveSingleModule(key);
    }

    public void processInsert(ModulesKey key) {
        super.doAddSingleModule(key, new EmptyModule(key.name, key.file));
    }

    public void endProcessing() {
        //save it with the updated info
        nature.saveAstManager();
    }

    @Override
    protected void doRemoveSingleModule(ModulesKey key) {
        super.doRemoveSingleModule(key);
        //overriden to add delta
        deltaSaver.addDeleteCommand(key);
        checkDeltaSize();
    }
    
    @Override
    protected void doAddSingleModule(ModulesKey key, AbstractModule n) {
        super.doAddSingleModule(key, n);
        //overriden to add delta
        deltaSaver.addInsertCommand(key);
        checkDeltaSize();
    }
    
    /**
     * If the delta size is big enough, save the current state and discard the deltas.
     */
    private void checkDeltaSize() {
        if(deltaSaver.availableDeltas() > MAXIMUN_NUMBER_OF_DELTAS){
            nature.saveAstManager();
            deltaSaver.clearAll();
        }
    }


    // ------------------------ end delta processing
    
    
    
    /**
     * @param nature this is the nature for this project modules manager (can be used if no project is set)
     */
    public void setPythonNature(IPythonNature nature){
        this.nature = nature;
    }
    
    /**
     * @return the nature related to this manager
     */
    public IPythonNature getNature() {
        return nature;
    }
    
    public SystemModulesManager getSystemModulesManager(){
        IInterpreterManager iMan = PydevPlugin.getInterpreterManager(nature);
        InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
        return info.modulesManager;
    }
    
    /**
     * @return a set with the names of all available modules
     */
    public Set<String> getAllModuleNames() {
        Set<String> s = new HashSet<String>();
        Set keySet = getModules().keySet();
        for (Object object : keySet) {
            ModulesKey m = (ModulesKey) object;
            s.add(m.name);
        }

        ModulesManager[] managersInvolved = this.getManagersInvolved(true);
        for (int i = 0; i < managersInvolved.length; i++) {
            keySet = managersInvolved[i].getModules().keySet();
            for (Object object : keySet) {
                ModulesKey m = (ModulesKey) object;
                s.add(m.name);
            }
        }
        return s;
    }
    
    @Override
    public ModulesKey[] getOnlyDirectModules() {
        return super.getAllModules();
    }

    @Override
    public ModulesKey[] getAllModules() {
        List<ModulesKey> ret = new ArrayList<ModulesKey>();
        ret.addAll(Arrays.asList(super.getAllModules()));
                
        ModulesManager[] managersInvolved = this.getManagersInvolved(true);
        for (int i = 0; i < managersInvolved.length; i++) {
            ret.addAll((Arrays.asList(managersInvolved[i].getAllModules())));
        }
        return ret.toArray(new ModulesKey[0]);
    }

    
    public AbstractModule getModule(String name, PythonNature nature, boolean isLookingForRelative) {
        return getModule(name, nature, true, isLookingForRelative);
    }
    
    public AbstractModule getModule(String name, PythonNature nature, boolean checkSystemManager, boolean isLookingForRelative) {
        ModulesManager[] managersInvolved = this.getManagersInvolved(true); //only get the system manager here (to avoid recursion)

        for (ModulesManager m : managersInvolved) {
            AbstractModule module;
            if (m instanceof ProjectModulesManager) {
                ProjectModulesManager pM = (ProjectModulesManager) m;
                module = pM.getModule(name, nature, false, isLookingForRelative);

            }else{
                module = m.getModule(name, nature, isLookingForRelative); //we already have the system manager here...
            }
            if(module != null){
                return module;
            }
        }
        return super.getModule(name, nature, isLookingForRelative);
    }

    /**
     * @param member the member we want to know if it is in the pythonpath
     * @param container the project where the member is
     * @return true if it is in the pythonpath and false otherwise
     */
    public boolean isInPythonPath(IResource member, IProject container) {
        return resolveModule(member, container) != null;
    }
    
    /**
     * @param member this is the member file we are analyzing
     * @param container the project where the file is contained
     * @return the name of the module given the pythonpath
     */
    public String resolveModule(IResource member, IProject container) {
        IPath location = PydevPlugin.getLocation(member.getFullPath(), container);
        if(location == null){
            //not in workspace?... maybe it was removed, so, do nothing, but let the user know about it
            PydevPlugin.log("Unable to find the path "+member+" in the project were it\n" +
                    "is added as a source folder for pydev (project: "+project.getName()+")");
            return null;
        }else{
            File inOs = new File(location.toOSString());
            return resolveModule(REF.getFileAbsolutePath(inOs));
        }
    }

    /**
     * resolve module for all, including the system manager.
     * 
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#resolveModule(java.lang.String)
     */
    public String resolveModule(String full) {
        return resolveModule(full, false);
    }
    
    /**
     * @param full the full file-system path of the file to resolve
     * @return the name of the module given the pythonpath
     */
    public String resolveModule(String full, boolean checkSystemManager) {
        ModulesManager[] managersInvolved = this.getManagersInvolved(checkSystemManager);
        for (ModulesManager m : managersInvolved) {
            
            String mod;
            if (m instanceof ProjectModulesManager) {
                ProjectModulesManager pM = (ProjectModulesManager) m;
                mod = pM.resolveModule(full, false);
            
            }else{
                mod = m.resolveModule(full);
            }

            if(mod != null){
                return mod;
            }
        }
        return super.resolveModule(full);
    }

    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
        super.changePythonPath(pythonpath, project, monitor);
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getSize()
     */
    public int getSize() {
        int size = getModules().size();
        ModulesManager[] managersInvolved = this.getManagersInvolved(true);
        for (int i = 0; i < managersInvolved.length; i++) {
            size += managersInvolved[i].getModules().size();
        }
        return size;
    }

    /**
     * Forced builtins are only specified in the system.
     * 
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getBuiltins()
     */
    public String[] getBuiltins() {
        String[] builtins = null;
        SystemModulesManager systemModulesManager = getSystemModulesManager();
        if(systemModulesManager != null){
            builtins = systemModulesManager.getBuiltins();
        }
        return builtins;
    }


    /**
     * @param checkSystemManager 
     * @return Returns the managersInvolved (does not include itself).
     */
    protected ModulesManager[] getManagersInvolved(boolean checkSystemManager) {
        ArrayList<ModulesManager> list = new ArrayList<ModulesManager>();
        SystemModulesManager systemModulesManager = getSystemModulesManager();
        if(checkSystemManager && systemModulesManager != null){
            list.add(systemModulesManager);
        }

        try {
            if(project != null){
	            IProject[] referencedProjects = project.getReferencedProjects();
	            for (int i = 0; i < referencedProjects.length; i++) {
	                PythonNature nature = PythonNature.getPythonNature(referencedProjects[i]);
	                if(nature!=null){
	                    ICodeCompletionASTManager otherProjectAstManager = nature.getAstManager();
	                    if(otherProjectAstManager != null){
	                        ProjectModulesManager projectModulesManager = otherProjectAstManager.getProjectModulesManager();
		                    if(projectModulesManager != null){
		                        list.add(projectModulesManager);
		                    }
	                    }
	                }
	            }
            }
            return (ModulesManager[]) list.toArray(new ModulesManager[list.size()]);
        } catch (CoreException e) {
            PydevPlugin.log(e);
            if(checkSystemManager && systemModulesManager != null){
                return new ModulesManager[]{systemModulesManager};
            }else{
                return new ModulesManager[]{};
            }
        }
    }

    
    /**
     * @return the paths that constitute the pythonpath as a list of strings
     */
    public List<String> getCompletePythonPath(){
        List<String> l = new ArrayList<String>();
        ModulesManager[] managersInvolved = getManagersInvolved(true);
        for (int i = 0; i < managersInvolved.length; i++) {
            l.addAll(managersInvolved[i].pythonPathHelper.pythonpath);
        }
        l.addAll(this.pythonPathHelper.pythonpath);
        return l;
    }

}

