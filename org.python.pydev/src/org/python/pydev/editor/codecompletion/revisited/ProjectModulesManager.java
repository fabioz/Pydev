/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.plugin.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class ProjectModulesManager extends ModulesManager{

    protected transient ModulesManager[] managersInvolved = new ModulesManager[0];
    
    /**
     * @return
     */
    public Set keySet() {
        Set s = new HashSet();
        for (int i = 0; i < this.managersInvolved.length; i++) {
            s.addAll(this.managersInvolved[i].getModules().keySet());
        }
        s.addAll(getModules().keySet());
        return s;
    }
    
    public AbstractModule getModule(String name, PythonNature nature) {
        for (int i = 0; i < this.managersInvolved.length; i++) {
            AbstractModule module = this.managersInvolved[i].getModule(name, nature);
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
        for (int i = 0; i < this.managersInvolved.length; i++) {
            String mod = this.managersInvolved[i].resolveModule(full);
            if(mod != null){
                return mod;
            }
        }
        return super.resolveModule(full);
    }

    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor, List managersInvolved) {
        super.changePythonPath(pythonpath, project, monitor, managersInvolved);
        
        if(managersInvolved.size() == 0){
            throw new RuntimeException("This class must receive at least a system module manager to work.");
        }
        this.managersInvolved = (ModulesManager[]) managersInvolved.toArray(new ModulesManager[0]);
    }
    
    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getSize()
     */
    public int getSize() {
        int size = super.getSize();
        for (int i = 0; i < this.managersInvolved.length; i++) {
            size += this.managersInvolved[i].getSize();
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
        for (int i = 0; i < this.managersInvolved.length; i++) {
            String[] builtins = this.managersInvolved[i].getBuiltins();
            set.addAll(Arrays.asList(builtins));
        }
        return (String[]) set.toArray(new String[0]);
    }
}
