package org.python.pydev.core;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IModulesManager {
    /**
     * This is the maximun number of deltas that can be generated before saving everything in a big chunck and 
     * clearing the deltas
     */
    public static final int MAXIMUN_NUMBER_OF_DELTAS = 100;
    /**
     * @param nature this is the nature for this project modules manager (can be used if no project is set)
     */
    public abstract void setPythonNature(IPythonNature nature);

    /**
     * @return the nature related to this manager
     */
    public abstract IPythonNature getNature();

    public abstract ISystemModulesManager getSystemModulesManager();

    /**
     * @return a set with the names of all available modules
     */
    public abstract Set<String> getAllModuleNames();

    public abstract ModulesKey[] getOnlyDirectModules();

    public abstract IModule getRelativeModule(String name, IPythonNature nature);

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param name the name of the module we're looking for (e.g.: mod1.mod2)
     * @param dontSearchInit is used in a negative form because initially it was isLookingForRelative, but
     * it actually defines if we should look in __init__ modules too, so, the name matches the old signature.
     * 
     * NOTE: isLookingForRelative description was: when looking for relative imports, we don't check for __init__
     * @return the module represented by this name
     */
    public abstract IModule getModule(String name, IPythonNature nature, boolean dontSearchInit);

    public abstract IModule getModule(String name, IPythonNature nature, boolean checkSystemManager, boolean dontSearchInit);

    /**
     * @param member the member we want to know if it is in the pythonpath
     * @param container the project where the member is
     * @return true if it is in the pythonpath and false otherwise
     */
    public abstract boolean isInPythonPath(IResource member, IProject container);

    /**
     * @param member this is the member file we are analyzing
     * @param container the project where the file is contained
     * @return the name of the module given the pythonpath
     */
    public abstract String resolveModule(IResource member, IProject container);

    /**
     * resolve module for all, including the system manager.
     * 
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#resolveModule(java.lang.String)
     */
    public abstract String resolveModule(String full);

    /**
     * @param full the full file-system path of the file to resolve
     * @return the name of the module given the pythonpath
     */
    public abstract String resolveModule(String full, boolean checkSystemManager);

    public abstract void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor, String defaultSelectedInterpreter);

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getSize()
     */
    public abstract int getSize();

    /**
     * Forced builtins are only specified in the system.
     * 
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getBuiltins()
     */
    public abstract String[] getBuiltins();

    /**
     * @param interpreter: this is the interpreter that should be used for getting the pythonpathString interpreter
     *                     (if it is null, the default interpreter is used)
     * @return the paths that constitute the pythonpath as a list of strings
     */
    public abstract List<String> getCompletePythonPath(String interpreter);

    public abstract SortedMap<ModulesKey,ModulesKey> getAllModulesStartingWith(String moduleToGetTokensFrom);
    public abstract SortedMap<ModulesKey,ModulesKey> getAllDirectModulesStartingWith(String moduleToGetTokensFrom);
    
    /**
     * @return true if it was started without problems
     */
    public boolean startCompletionCache();
    public void endCompletionCache();

    public abstract Object /*PythonPathHelper*/ getPythonPathHelper();

    /**
     * This method removes some module from this modules manager.
     * 
     * @param key the key that represents the module to be removed from this modules manager.
     */
    public abstract void removeModules(Collection<ModulesKey> toRem);

    /**
     * 
     * @param key
     */
    public abstract void addModule(ModulesKey key);
}
