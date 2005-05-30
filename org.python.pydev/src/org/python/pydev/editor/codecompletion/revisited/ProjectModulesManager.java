/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class ProjectModulesManager extends ModulesManager{

    //these attributes must be set whenever this class is restored.
    private transient ModulesManager systemModulesManager;
    private transient IProject project;
    
    /**
     * @return
     */
    public Set keySet() {
        Set s = new HashSet();
        for (int i = 0; i < this.getManagersInvolved().length; i++) {
            s.addAll(this.getManagersInvolved()[i].getModules().keySet());
        }
        s.addAll(getModules().keySet());
        return s;
    }
    
    public AbstractModule getModule(String name, PythonNature nature) {
        for (int i = 0; i < this.getManagersInvolved().length; i++) {
            AbstractModule module = this.getManagersInvolved()[i].getModule(name, nature);
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
        for (int i = 0; i < this.getManagersInvolved().length; i++) {
            String mod = this.getManagersInvolved()[i].resolveModule(full);
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
        int size = super.getSize();
        for (int i = 0; i < this.getManagersInvolved().length; i++) {
            size += this.getManagersInvolved()[i].getSize();
        }
        return size;
    }

    /**
     * Forced builtins are only specified in the system.
     * 
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getBuiltins()
     */
    public String[] getBuiltins() {
        HashSet set = new HashSet();
        for (int i = 0; i < this.getManagersInvolved().length; i++) {
            String[] builtins = this.getManagersInvolved()[i].getBuiltins();
            set.addAll(Arrays.asList(builtins));
        }
        return (String[]) set.toArray(new String[0]);
    }


    /**
     * @return Returns the managersInvolved.
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
	                    IASTManager otherProjectAstManager = nature.getAstManager();
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

}
