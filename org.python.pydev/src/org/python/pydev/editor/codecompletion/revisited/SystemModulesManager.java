/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;

/**
 * @author Fabio Zadrozny
 */
public class SystemModulesManager extends ModulesManager implements ISystemModulesManager{

    private static final long serialVersionUID = 2L;
    private String[] builtins;
	private transient IPythonNature nature;

    /**
     * @param forcedLibs
     */
    public SystemModulesManager(Collection<String> forcedLibs) {
        regenerateForcedBuilltins(forcedLibs);
    }

    /** 
     * @see org.python.pydev.core.ISystemModulesManager#regenerateForcedBuilltins(java.util.Collection)
     */
    public void regenerateForcedBuilltins(Collection<String> forcedLibs){
        this.builtins = (String[]) forcedLibs.toArray(new String[0]);
    }
    
    public String[] getBuiltins(String defaultSelectedInterpreter) {
    	return getBuiltins();
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
    public void setBuiltins(Collection<String> forcedLibs) {
        regenerateForcedBuilltins(forcedLibs);
    }

	public void setPythonNature(IPythonNature nature) {
		this.nature = nature;
	}

	public IPythonNature getNature() {
		return nature;
	}

	public ISystemModulesManager getSystemModulesManager() {
		return this; //itself
	}

	public IModule getModule(String name, IPythonNature nature, boolean checkSystemManager, boolean dontSearchInit) {
		return super.getModule(name, nature, dontSearchInit);
	}


	public String resolveModule(String full, boolean checkSystemManager) {
		return super.resolveModule(full);
	}

	public List<String> getCompletePythonPath() {
		return new ArrayList<String>(super.getPythonPath());
	}

	public IModule getRelativeModule(String name, IPythonNature nature) {
		return super.getModule(name, nature, true);
	}

}
