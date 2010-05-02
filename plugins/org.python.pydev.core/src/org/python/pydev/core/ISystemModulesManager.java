/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;




public interface ISystemModulesManager extends IModulesManager {

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ModulesManager#getBuiltins()
     */
    public abstract String[] getBuiltins();

    /**
     * @return a given module only considering the modules in the builtins.
     */
    public IModule getBuiltinModule(String name, IPythonNature nature, boolean dontSearchInit);

    /**
     * @return a given module only considering modules that are not in the builtins.
     */
    public abstract IModule getModuleWithoutBuiltins(String name, IPythonNature nature, boolean dontSearchInit);

    /**
     * Sets the interpreter info for the given system modules manager.
     */
    public abstract void setInfo(Object /*InterpreterInfo*/ interpreterInfo);
 
    /**
     * Clears any internally kept caches for the modules manager.
     */
    void clearCache();
}