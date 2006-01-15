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
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author Fabio Zadrozny
 */
public class ProjectModulesManager extends ModulesManager implements IDeltaProcessor<ModulesKey>, IProjectModulesManager{

    private static final long serialVersionUID = 1L;

    /**
     * Determines whether we are testing it.
     */
    public static boolean IN_TESTS = false;
    
    //these attributes must be set whenever this class is restored.
    private transient IProject project;
    private transient IPythonNature nature;
    
    /**
     * Used to process deltas (in case we have the process killed for some reason)
     */
    private transient DeltaSaver<ModulesKey> deltaSaver;
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#setProject(org.eclipse.core.resources.IProject, boolean)
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
    

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#processUpdate(org.python.pydev.core.ModulesKey)
     */
    public void processUpdate(ModulesKey data) {
        //updates are ignored because we always start with 'empty modules' (so, we don't actually generate them -- updates are treated as inserts).
        throw new RuntimeException("Not impl");
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#processDelete(org.python.pydev.core.ModulesKey)
     */
    public void processDelete(ModulesKey key) {
        super.doRemoveSingleModule(key);
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#processInsert(org.python.pydev.core.ModulesKey)
     */
    public void processInsert(ModulesKey key) {
        super.doAddSingleModule(key, new EmptyModule(key.name, key.file));
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#endProcessing()
     */
    public void endProcessing() {
        //save it with the updated info
        nature.saveAstManager();
    }

    @Override
    protected void doRemoveSingleModule(ModulesKey key) {
        super.doRemoveSingleModule(key);
        if(deltaSaver != null || !IN_TESTS){ //we want the error if we are not in tests
            //overriden to add delta
            deltaSaver.addDeleteCommand(key);
            checkDeltaSize();
        }
    }
        
    
    @Override
    protected void doAddSingleModule(ModulesKey key, AbstractModule n) {
        super.doAddSingleModule(key, n);
        if(deltaSaver != null || !IN_TESTS){ //we want the error if we are not in tests
            //overriden to add delta
            deltaSaver.addInsertCommand(key);
            checkDeltaSize();
        }
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
     * @see org.python.pydev.core.IProjectModulesManager#setPythonNature(org.python.pydev.core.IPythonNature)
     */
    public void setPythonNature(IPythonNature nature){
        this.nature = nature;
    }
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getNature()
     */
    public IPythonNature getNature() {
        return nature;
    }
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getSystemModulesManager()
     */
    public SystemModulesManager getSystemModulesManager(){
        IInterpreterManager iMan = PydevPlugin.getInterpreterManager(nature);
        InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
        return info.modulesManager;
    }
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getAllModuleNames()
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
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getOnlyDirectModules()
     */
    @Override
    public ModulesKey[] getOnlyDirectModules() {
        return super.getAllModules();
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getAllModules()
     */
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

    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getModule(java.lang.String, org.python.pydev.plugin.nature.PythonNature, boolean)
     */
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        return getModule(name, nature, true, dontSearchInit);
    }
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getModule(java.lang.String, org.python.pydev.plugin.nature.PythonNature, boolean, boolean)
     */
    public IModule getModule(String name, IPythonNature nature, boolean checkSystemManager, boolean dontSearchInit) {
        ModulesManager[] managersInvolved = this.getManagersInvolved(true); //only get the system manager here (to avoid recursion)

        for (ModulesManager m : managersInvolved) {
            IModule module;
            if (m instanceof ProjectModulesManager) {
                IProjectModulesManager pM = (IProjectModulesManager) m;
                module = pM.getModule(name, nature, false, dontSearchInit);

            }else{
                module = m.getModule(name, nature, dontSearchInit); //we already have the system manager here...
            }
            if(module != null){
                return module;
            }
        }
        return super.getModule(name, nature, dontSearchInit);
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#isInPythonPath(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
     */
    public boolean isInPythonPath(IResource member, IProject container) {
        return resolveModule(member, container) != null;
    }
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#resolveModule(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
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
     * @see org.python.pydev.core.IProjectModulesManager#resolveModule(java.lang.String)
     */
    public String resolveModule(String full) {
        return resolveModule(full, true);
    }
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#resolveModule(java.lang.String, boolean)
     */
    public String resolveModule(String full, boolean checkSystemManager) {
        ModulesManager[] managersInvolved = this.getManagersInvolved(checkSystemManager);
        for (ModulesManager m : managersInvolved) {
            
            String mod;
            if (m instanceof ProjectModulesManager) {
                IProjectModulesManager pM = (IProjectModulesManager) m;
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

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#changePythonPath(java.lang.String, org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
        super.changePythonPath(pythonpath, project, monitor);
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getSize()
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
     * @see org.python.pydev.core.IProjectModulesManager#getBuiltins()
     */
    public String[] getBuiltins() {
        String[] builtins = null;
        ISystemModulesManager systemModulesManager = getSystemModulesManager();
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
	                        IProjectModulesManager projectModulesManager = otherProjectAstManager.getProjectModulesManager();
		                    if(projectModulesManager != null){
		                        list.add((ModulesManager) projectModulesManager);
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
     * @see org.python.pydev.core.IProjectModulesManager#getCompletePythonPath()
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

