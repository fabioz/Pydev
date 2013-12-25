/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.io.File;
import java.io.IOException;

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
     * Loads the system information from the disk.
     */
    public abstract void load() throws IOException;

    /**
     * Saves the system information to the disk.
     */
    public abstract void save();

    public File getIoDirectory();

    public abstract IInterpreterManager getInterpreterManager();

    public abstract File getCompiledModuleCacheFile(String name);
}