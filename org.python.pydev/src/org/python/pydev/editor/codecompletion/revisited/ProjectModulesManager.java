/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class ProjectModulesManager extends ModulesManager{

     private static final long serialVersionUID = 1L;
    //these attributes must be set whenever this class is restored.
    private transient ModulesManager systemModulesManager;
    private transient IProject project;
    
    public IProject getProject(){
        return project;
    }
    
    /**
     * @return
     */
    public Set keySet() {
        Set s = new HashSet();
        s.addAll(getModules().keySet());

        ModulesManager[] managersInvolved = this.getManagersInvolved();
        for (int i = 0; i < managersInvolved.length; i++) {
            s.addAll(managersInvolved[i].getModules().keySet());
        }
        return s;
    }
    
    public AbstractModule getModule(String name, PythonNature nature) {
        ModulesManager[] managersInvolved = this.getManagersInvolved();
        for (int i = 0; i < managersInvolved.length; i++) {
            AbstractModule module = managersInvolved[i].getModule(name, nature);
            if(module != null){
                return module;
            }
        }
        return super.getModule(name, nature);
    }

    /**
     * @param full
     * @return
     */
    public String resolveModule(String full) {
        ModulesManager[] managersInvolved = this.getManagersInvolved();
        for (int i = 0; i < managersInvolved.length; i++) {
            String mod = managersInvolved[i].resolveModule(full);
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
     * @param managersInvolved2
     */
    public void setSystemModuleManager(SystemModulesManager systemManager, IProject project) {
        this.systemModulesManager = systemManager;
        this.project = project;
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getSize()
     */
    public int getSize() {
        int size = getModules().size();
        ModulesManager[] managersInvolved = this.getManagersInvolved();
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
        if(systemModulesManager != null){
            builtins = systemModulesManager.getBuiltins();
        }
        return builtins;
    }


    /**
     * @return Returns the managersInvolved (does not include itself).
     */
    protected ModulesManager[] getManagersInvolved() {
        try {
            ArrayList list = new ArrayList();
            if(systemModulesManager != null){
                list.add(systemModulesManager);
            }
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
            if(systemModulesManager != null){
                return new ModulesManager[]{systemModulesManager};
            }else{
                return new ModulesManager[]{};
            }
        }
    }

    public List getCompletePythonPath(){
        ArrayList l = new ArrayList();
        ModulesManager[] managersInvolved = getManagersInvolved();
        for (int i = 0; i < managersInvolved.length; i++) {
            l.addAll(managersInvolved[i].pythonPathHelper.pythonpath);
        }
        l.addAll(this.pythonPathHelper.pythonpath);
        return l;
    }
}
