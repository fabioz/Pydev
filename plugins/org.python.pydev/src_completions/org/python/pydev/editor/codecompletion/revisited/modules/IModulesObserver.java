/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 24, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import org.python.pydev.core.IModulesManager;

/**
 * This interface is used to identify what happens to modules. 
 * 
 * @author Fabio
 */
public interface IModulesObserver {

    /**
     * This method is called whenever a compiled module is created (and after its
     * tokens are set).
     * 
     * @param module this is the compiled module that has just been created.
     * @param manager the manager that where this module is stored
     */
    void notifyCompiledModuleCreated(CompiledModule module, IModulesManager manager);

}
