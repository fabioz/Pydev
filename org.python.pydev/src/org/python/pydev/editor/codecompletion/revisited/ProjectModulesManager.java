/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.ICallback;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
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
    public void doRemoveSingleModule(ModulesKey key) {
        super.doRemoveSingleModule(key);
        if(deltaSaver != null || !IN_TESTS){ //we don't want deltas in tests
            //overriden to add delta
            deltaSaver.addDeleteCommand(key);
            checkDeltaSize();
        }
    }
        
    
    @Override
    public void doAddSingleModule(ModulesKey key, AbstractModule n) {
        super.doAddSingleModule(key, n);
        if(deltaSaver != null || !IN_TESTS){ //we don't want deltas in tests
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
    
    public SystemModulesManager getSystemModulesManager(){
    	return getSystemModulesManager(null);
    }
    
    /** 
     * @param defaultSelectedInterpreter 
     * @see org.python.pydev.core.IProjectModulesManager#getSystemModulesManager()
     */
    public SystemModulesManager getSystemModulesManager(String defaultSelectedInterpreter){
    	if(nature == null){
    		return null; //still not set (initialization)
    	}
        IInterpreterManager iMan = PydevPlugin.getInterpreterManager(nature);
        if(defaultSelectedInterpreter == null){
        	defaultSelectedInterpreter = iMan.getDefaultInterpreter();
        }
        InterpreterInfo info = (InterpreterInfo) iMan.getInterpreterInfo(defaultSelectedInterpreter, new NullProgressMonitor());
        if(info == null){
        	return null; //may happen during initialization
        }
        return info.modulesManager;
    }
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getAllModuleNames()
     */
    public Set<String> getAllModuleNames() {
        Set<String> s = new HashSet<String>();
        for (Object object : this.modulesKeys.keySet()) {
            ModulesKey m = (ModulesKey) object;
            s.add(m.name);
        }

        ModulesManager[] managersInvolved = this.getManagersInvolved(true);
        for (int i = 0; i < managersInvolved.length; i++) {
            for (Object object : managersInvolved[i].modulesKeys.keySet()) {
                ModulesKey m = (ModulesKey) object;
                s.add(m.name);
            }
        }
        return s;
    }
    
    /**
     * @return all the modules that start with some token (from this manager and others involved)
     */
    @Override
    public SortedMap<ModulesKey, ModulesKey> getAllModulesStartingWith(String strStartingWith) {
    	SortedMap<ModulesKey, ModulesKey> ret = getAllDirectModulesStartingWith(strStartingWith);
    	ModulesManager[] managersInvolved = this.getManagersInvolved(true);
    	for (int i = 0; i < managersInvolved.length; i++) {
    		ret.putAll(managersInvolved[i].getAllDirectModulesStartingWith(strStartingWith));
    	}
		return ret;
    }

    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getModule(java.lang.String, org.python.pydev.plugin.nature.PythonNature, boolean)
     */
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        return getModule(name, nature, true, dontSearchInit);
    }
    
    /**
     * When looking for relative, we do not check dependencies
     */
    public IModule getRelativeModule(String name, IPythonNature nature) {
    	return super.getModule(name, nature, true);
    }
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getModule(java.lang.String, org.python.pydev.plugin.nature.PythonNature, boolean, boolean)
     */
    public IModule getModule(String name, IPythonNature nature, boolean checkSystemManager, boolean dontSearchInit) {
        ModulesManager[] managersInvolved = this.getManagersInvolved(true); //only get the system manager here (to avoid recursion)

        for (ModulesManager m : managersInvolved) {
            IModule module;
            if (m instanceof ProjectModulesManager) {
                ProjectModulesManager pM = (ProjectModulesManager) m;
                module = pM.getModuleInDirectManager(name, nature, dontSearchInit);

            }else{
                module = m.getModule(name, nature, dontSearchInit); //we already have the system manager here...
            }
            if(module != null){
                return module;
            }
        }
        return super.getModule(name, nature, dontSearchInit);
    }
    
    public IModule getModuleInDirectManager(String name, IPythonNature nature, boolean dontSearchInit) {
        return super.getModule(name, nature, dontSearchInit);
    }

    protected String getResolveModuleErr(IResource member) {
		return "Unable to find the path "+member+" in the project were it\n" +
        "is added as a source folder for pydev (project: "+project.getName()+")";
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
                ProjectModulesManager pM = (ProjectModulesManager) m;
                mod = pM.resolveModuleInDirectManager(full);
            
            }else{
                mod = m.resolveModule(full);
            }

            if(mod != null){
                return mod;
            }
        }
        return super.resolveModule(full);
    }

    public String resolveModuleInDirectManager(String full) {
        return super.resolveModule(full);
    }
    
    public String resolveModuleInDirectManager(IResource member, IProject container) {
        File inOs = member.getRawLocation().toFile();
        return resolveModuleInDirectManager(REF.getFileAbsolutePath(inOs));
    }

    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#changePythonPath(java.lang.String, org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor, String defaultSelectedInterpreter) {
        super.changePythonPath(pythonpath, project, monitor, defaultSelectedInterpreter);
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getSize()
     */
    public int getSize() {
        int size = this.modulesKeys.size();
        ModulesManager[] managersInvolved = this.getManagersInvolved(true);
        for (int i = 0; i < managersInvolved.length; i++) {
            size += managersInvolved[i].modulesKeys.size();
        }
        return size;
    }

    public String[] getBuiltins() {
    	return getBuiltins(null);
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getBuiltins()
     */
    public String[] getBuiltins(String defaultSelectedInterpreter) {
        String[] builtins = null;
        ISystemModulesManager systemModulesManager = getSystemModulesManager(defaultSelectedInterpreter);
        if(systemModulesManager != null){
            builtins = systemModulesManager.getBuiltins();
        }
        return builtins;
    }


    /**
     * @param checkSystemManager whether the system manager should be added
     * @param referenced true if we should get the referenced projects 
     *                   false if we should get the referencing projects
     * @return the Managers that this project references or the ones that reference this project (depends on 'referenced') 
     */
    private synchronized ModulesManager[] getManagers(boolean checkSystemManager, boolean referenced) {
    	if(this.completionCache != null){
    		ModulesManager[] ret = this.completionCache.getManagers(referenced);
    		if(ret != null){
    			return ret;
    		}
    	}
        ArrayList<ModulesManager> list = new ArrayList<ModulesManager>();
        SystemModulesManager systemModulesManager = getSystemModulesManager(null);
        if(systemModulesManager == null){
        	//may happen in initialization
        	return new ModulesManager[]{};
        }
        
        try {
            if(project != null){
            	HashSet<IProject> projs = new HashSet<IProject>();
            	getProjectsRecursively(project, referenced, projs);
                addModuleManagers(list, projs);
            }
            //the system is the last one we add.
            if(checkSystemManager && systemModulesManager != null){
                list.add(systemModulesManager);
            }
            ModulesManager[] ret = (ModulesManager[]) list.toArray(new ModulesManager[list.size()]);
            if(this.completionCache != null){
            	this.completionCache.setManagers(ret, referenced);
            }
            return ret;
        } catch (CoreException e) {
            //PydevPlugin.log(e); not logged anymore (this may happen if the project was closed and a thread was still running this)
            if(checkSystemManager && systemModulesManager != null){
                return new ModulesManager[]{systemModulesManager};
            }else{
                return new ModulesManager[]{};
            }
        }
    }


	private void getProjectsRecursively(IProject project, boolean referenced, HashSet<IProject> memo) throws CoreException {
		IProject[] projects;
		if(referenced){
		    projects = project.getReferencedProjects();
		}else{
		    projects = project.getReferencingProjects();
		}
		HashSet<IProject> newFound = new HashSet<IProject>();
		for (IProject p : projects) {
			if(!memo.contains(p)){
				memo.add(p);
				newFound.add(p);
			}
		}
		
		for (IProject p : newFound) {
			getProjectsRecursively(p, referenced, memo);
		}
	}
    

    /**
     * @param list the list that will be filled with the managers
     * @param projects the projects that should have the managers added
     */
    private void addModuleManagers(ArrayList<ModulesManager> list, Collection<IProject> projects) {
    	for(IProject project:projects){
	        PythonNature nature = PythonNature.getPythonNature(project);
	        if(nature!=null){
	            ICodeCompletionASTManager otherProjectAstManager = nature.getAstManager();
	            if(otherProjectAstManager != null){
	                IModulesManager projectModulesManager = otherProjectAstManager.getModulesManager();
	                if(projectModulesManager != null){
	                    list.add((ModulesManager) projectModulesManager);
	                }
	            }
            }
        }
    }

    
    /**
     * @return Returns the managers that this project references(does not include itself).
     */
    public ModulesManager[] getManagersInvolved(boolean checkSystemManager) {
        return getManagers(checkSystemManager, true);
    }

    /**
     * @return Returns the managers that reference this project (does not include itself).
     */
    public ModulesManager[] getRefencingManagersInvolved(boolean checkSystemManager) {
        return getManagers(checkSystemManager, false);
    }


    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getCompletePythonPath()
     */
    public List<String> getCompletePythonPath(String interpreter){
        List<String> l = this.pythonPathHelper.getPythonpath();
        ModulesManager[] managersInvolved = getManagersInvolved(true);
        for (ModulesManager m:managersInvolved) {
            if(m instanceof SystemModulesManager){
                SystemModulesManager systemModulesManager = (SystemModulesManager) m;
                l.addAll(systemModulesManager.getCompletePythonPath(interpreter, nature));
                
            }else{
                l.addAll(m.pythonPathHelper.getPythonpath());
            }
        }
        return l;
    }




}

