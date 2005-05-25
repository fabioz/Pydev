/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.Collection;

/**
 * @author Fabio Zadrozny
 */
public class SystemModulesManager extends ModulesManager{

    private String[] builtins;

    /**
     * @param forcedLibs
     */
    public SystemModulesManager(Collection forcedLibs) {
        regenerateForcedBuilltins(forcedLibs);
    }

    public void regenerateForcedBuilltins(Collection forcedLibs){
        this.builtins = (String[]) forcedLibs.toArray(new String[0]);
    }
    
    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getBuiltins()
     */
    public String[] getBuiltins() {
        return this.builtins;
    }

    /**
     * @param forcedLibs
     */
    public void setBuiltins(Collection forcedLibs) {
        regenerateForcedBuilltins(forcedLibs);
    }

}
