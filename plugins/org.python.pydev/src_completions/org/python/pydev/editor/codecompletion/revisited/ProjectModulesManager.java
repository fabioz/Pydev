/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaProjectModulesManagerCreator;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public final class ProjectModulesManager extends ModulesManagerWithBuild implements IProjectModulesManager{

    
    private static final boolean DEBUG_MODULES = false;
    
    //these attributes must be set whenever this class is restored.
    private volatile IProject project;
    private volatile IPythonNature nature;

    
    public ProjectModulesManager() {}
    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#setProject(org.eclipse.core.resources.IProject, boolean)
     */
    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas){
        this.project = project;
        this.nature = nature;
        File completionsCacheDir = this.nature.getCompletionsCacheDir();
        if(completionsCacheDir == null){
        	return; //project was deleted.
        }

        this.deltaSaver = new DeltaSaver<ModulesKey>(completionsCacheDir, "v1_astdelta", readFromFileMethod,toFileMethod);
        
        if(!restoreDeltas){
            deltaSaver.clearAll(); //remove any existing deltas
        }else{
            deltaSaver.processDeltas(this); //process the current deltas (clears current deltas automatically and saves it when the processing is concluded)
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
     * @param defaultSelectedInterpreter 
     * @see org.python.pydev.core.IProjectModulesManager#getSystemModulesManager()
     */
    public ISystemModulesManager getSystemModulesManager(){
        if(nature == null){
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
    public Set<String> getAllModuleNames(boolean addDependencies, String partStartingWithLowerCase) {
        if(addDependencies){
            Set<String> s = new HashSet<String>();
            IModulesManager[] managersInvolved = this.getManagersInvolved(true);
            for (int i = 0; i < managersInvolved.length; i++) {
                s.addAll(managersInvolved[i].getAllModuleNames(false, partStartingWithLowerCase));
            }
            return s;
        }else{
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
        Tuple<IModule, IModulesManager> ret = getModuleAndRelatedModulesManager(name, nature, checkSystemManager, dontSearchInit);
        if(ret != null){
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
            if(m instanceof ISystemModulesManager){
                module = ((ISystemModulesManager)m).getBuiltinModule(name, dontSearchInit);
                if(module != null){
                    if(DEBUG_MODULES){
                        System.out.println("Trying to get:"+name+" - "+" returned builtin:"+module+" - "+m.getClass());
                    }
                    return new Tuple<IModule, IModulesManager>(module, m);
                }
            }
        }
        
        for (IModulesManager m : managersInvolved) {
            if (m instanceof IProjectModulesManager) {
                IProjectModulesManager pM = (IProjectModulesManager) m;
                module = pM.getModuleInDirectManager(name, nature, dontSearchInit);

            }else if (m instanceof ISystemModulesManager) {
                ISystemModulesManager systemModulesManager = (ISystemModulesManager) m;
                module = systemModulesManager.getModuleWithoutBuiltins(name, nature, dontSearchInit); 
                
            }else{
                throw new RuntimeException("Unexpected: "+m);
            }
            
            if(module != null){
                if(DEBUG_MODULES){
                    System.out.println("Trying to get:"+name+" - "+" returned:"+module+" - "+m.getClass());
                }
                return new Tuple<IModule, IModulesManager>(module, m);
            }
        }
        if(DEBUG_MODULES){
            System.out.println("Trying to get:"+name+" - "+" returned:null - "+this.getClass());
        }
        return null;
    }
    
    /**
     * Only searches the modules contained in the direct modules manager.
     */
    public IModule getModuleInDirectManager(String name, IPythonNature nature, boolean dontSearchInit) {
        return super.getModule(name, nature, dontSearchInit);
    }

    protected String getResolveModuleErr(IResource member) {
        return "Unable to find the path "+member+" in the project were it\n" +
        "is added as a source folder for pydev (project: "+project.getName()+")";
    }

	public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal) throws CoreException {
		String onlyProjectPythonPathStr = this.nature.getPythonPathNature().getOnlyProjectPythonPathStr(addExternal);
		HashSet<String> projectSourcePath = new HashSet<String>(StringUtils.splitAndRemoveEmptyTrimmed(onlyProjectPythonPathStr, '|'));
		
		return this.pythonPathHelper.resolveModule(fileAbsolutePath, new ArrayList<String>(projectSourcePath));
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
        IModulesManager[] managersInvolved = this.getManagersInvolved(checkSystemManager);
        for (IModulesManager m : managersInvolved) {
            
            String mod;
            if (m instanceof IProjectModulesManager) {
                IProjectModulesManager pM = (IProjectModulesManager) m;
                mod = pM.resolveModuleInDirectManager(full);
            
            }else{
                mod = m.resolveModule(full);
            }

            if(mod != null){
                return mod;
            }
        }
        return null;
    }

    public String resolveModuleInDirectManager(String full) {
        return super.resolveModule(full);
    }
    
    public String resolveModuleInDirectManager(IFile member) {
        File inOs = member.getRawLocation().toFile();
        return resolveModuleInDirectManager(REF.getFileAbsolutePath(inOs));
    }

    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getSize(boolean)
     */
    public int getSize(boolean addDependenciesSize) {
        if(addDependenciesSize){
            int size = 0;
            IModulesManager[] managersInvolved = this.getManagersInvolved(true);
            for (int i = 0; i < managersInvolved.length; i++) {
                size += managersInvolved[i].getSize(false);
            }
            return size;
        }else{
            return super.getSize(addDependenciesSize);
        }
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
     * @param checkSystemManager whether the system manager should be added
     * @param referenced true if we should get the referenced projects 
     *                   false if we should get the referencing projects
     * @return the Managers that this project references or the ones that reference this project (depends on 'referenced') 
     * 
     * Change in 1.3.3: adds itself to the list of returned managers
     */
    private synchronized IModulesManager[] getManagers(boolean checkSystemManager, boolean referenced) {
        if(this.completionCache != null){
            IModulesManager[] ret = this.completionCache.getManagers(referenced);
            if(ret != null){
                return ret;
            }
        }
        ArrayList<IModulesManager> list = new ArrayList<IModulesManager>();
        ISystemModulesManager systemModulesManager = getSystemModulesManager();
        if(systemModulesManager == null){
            //may happen in initialization
//            PydevPlugin.log("System modules manager still not available (still initializing or not set).");
            return new IModulesManager[]{};
        }
        
        //add itself 1st
        list.add(this);
        
        //get the projects 1st
        if(project != null){
            IModulesManager javaModulesManagerForProject = JavaProjectModulesManagerCreator.createJavaProjectModulesManagerIfPossible(project);
            if(javaModulesManagerForProject!=null){
                list.add(javaModulesManagerForProject);
            }
            
            Set<IProject> projs;
            if(referenced){
                projs = getReferencedProjects(project);
            }else{
                projs = getReferencingProjects(project);
            }
            addModuleManagers(list, projs);
        }
        
        //the system is the last one we add 
        //http://sourceforge.net/tracker/index.php?func=detail&aid=1687018&group_id=85796&atid=577329
        if(checkSystemManager && systemModulesManager != null){
            list.add(systemModulesManager);
        }
        
        IModulesManager[] ret = (IModulesManager[]) list.toArray(new IModulesManager[list.size()]);
        if(this.completionCache != null){
            this.completionCache.setManagers(ret, referenced);
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
            if(project == null || !project.isOpen() || !project.exists() || memo.contains(projects)){
                return;
            }
            if(referenced){
                projects = project.getReferencedProjects();
            }else{
                projects = project.getReferencingProjects();
            }
        } catch (CoreException e) {
            //ignore (it's closed)
        }
        
        
        
        if(projects != null){
            for (IProject p : projects) {
                if(!memo.contains(p)){
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
        for(IProject project:projects){
            PythonNature nature = PythonNature.getPythonNature(project);
            if(nature!=null){
                ICodeCompletionASTManager otherProjectAstManager = nature.getAstManager();
                if(otherProjectAstManager != null){
                    IModulesManager projectModulesManager = otherProjectAstManager.getModulesManager();
                    if(projectModulesManager != null){
                        list.add((IModulesManager) projectModulesManager);
                    }
                }else{
                    String msg = "No ast manager configured for :"+project.getName();
                    Log.log(IStatus.WARNING, msg, new RuntimeException(msg));
                }
            }
            IModulesManager javaModulesManagerForProject = JavaProjectModulesManagerCreator.createJavaProjectModulesManagerIfPossible(project);
            if(javaModulesManagerForProject != null){
                list.add(javaModulesManagerForProject);
            }
        }
    }

    
    /**
     * @return Returns the managers that this project references(does not include itself).
     */
    public IModulesManager[] getManagersInvolved(boolean checkSystemManager) {
        return getManagers(checkSystemManager, true);
    }

    /**
     * @return Returns the managers that reference this project (does not include itself).
     */
    public IModulesManager[] getRefencingManagersInvolved(boolean checkSystemManager) {
        return getManagers(checkSystemManager, false);
    }


    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#getCompletePythonPath()
     */
    public List<String> getCompletePythonPath(IInterpreterInfo interpreter, IInterpreterManager manager){
        List<String> l = new ArrayList<String>();
        IModulesManager[] managersInvolved = getManagersInvolved(true);
        for (IModulesManager m:managersInvolved) {
            if(m instanceof ISystemModulesManager){
                ISystemModulesManager systemModulesManager = (ISystemModulesManager) m;
                l.addAll(systemModulesManager.getCompletePythonPath(interpreter, manager));
                
            }else{
                PythonPathHelper h = (PythonPathHelper)m.getPythonPathHelper();
                if(h != null){
                    l.addAll(h.getPythonpath());
                }
            }
        }
        return l;
    }



}

