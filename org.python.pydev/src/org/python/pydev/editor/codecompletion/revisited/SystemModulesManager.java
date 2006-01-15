/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.Collection;

import org.python.pydev.core.ISystemModulesManager;

/**
 * @author Fabio Zadrozny
 */
public class SystemModulesManager extends ModulesManager implements ISystemModulesManager{

    private static final long serialVersionUID = 1L;
    private String[] builtins;

    /**
     * @param forcedLibs
     */
    public SystemModulesManager(Collection forcedLibs) {
        regenerateForcedBuilltins(forcedLibs);
    }

    /** 
     * @see org.python.pydev.core.ISystemModulesManager#regenerateForcedBuilltins(java.util.Collection)
     */
    public void regenerateForcedBuilltins(Collection forcedLibs){
        this.builtins = (String[]) forcedLibs.toArray(new String[0]);
    }
    
    /** 
     * @see org.python.pydev.core.ISystemModulesManager#getBuiltins()
     */
    public String[] getBuiltins() {
        return this.builtins;
    }

    /** 
     * @see org.python.pydev.core.ISystemModulesManager#setBuiltins(java.util.Collection)
     */
    public void setBuiltins(Collection forcedLibs) {
        regenerateForcedBuilltins(forcedLibs);
    }

}
