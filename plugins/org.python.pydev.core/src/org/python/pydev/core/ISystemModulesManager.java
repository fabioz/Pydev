/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
    public IModule getBuiltinModule(String name, boolean dontSearchInit);

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