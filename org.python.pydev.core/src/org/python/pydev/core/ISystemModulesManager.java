/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.util.Collection;



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
     * @return the complete pythonpath for this system modules manager.
     */
    public abstract Collection<? extends String> getCompletePythonPath(String interpreter, IPythonNature nature);

    /**
     * Sets the interpreter info for the given system modules manager.
     */
    public abstract void setInfo(Object /*InterpreterInfo*/ interpreterInfo);
}