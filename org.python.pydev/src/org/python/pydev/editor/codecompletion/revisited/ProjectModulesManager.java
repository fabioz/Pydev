/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
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
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author Fabio Zadrozny
 */
public class ProjectModulesManager extends ModulesManager{

     private static final long serialVersionUID = 1L;
    //these attributes must be set whenever this class is restored.
    private transient IProject project;
    private transient IPythonNature nature;
    
    /**
     * Set the project this modules manager works with.
     * 
     * @param project the project related to this manager
     */
    public void setProject(IProject project){
        this.project = project;
        this.nature = PythonNature.getPythonNature(project);
    }
    
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
     * @return
     */
    public Set<String> getAllModuleNames() {
        Set s = new HashSet();
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
        return resolveModule(full, true);
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

    
    public List getCompletePythonPath(){
        ArrayList l = new ArrayList();
        ModulesManager[] managersInvolved = getManagersInvolved(true);
        for (int i = 0; i < managersInvolved.length; i++) {
            l.addAll(managersInvolved[i].pythonPathHelper.pythonpath);
        }
        l.addAll(this.pythonPathHelper.pythonpath);
        return l;
    }
}
