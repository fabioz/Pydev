/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.interpreters;

import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;

/**
 * On a number of cases, we may want to do some action that relies on the python nature, but we are uncertain
 * on which should actually be used (python or jython).
 * 
 * So, this class helps in giving a choice for the user.
 *
 * @author Fabio
 */
public class ChooseInterpreterManager {

    /**
     * 
     * May return null if unable to choose an interpreter.
     * 
     * TODO: Instead of choosing always python as default if both are available, ask the user (and save that info).
     */
    public static IInterpreterManager chooseInterpreterManager() {
        IInterpreterManager manager = PydevPlugin.getPythonInterpreterManager();
        if (manager.isConfigured()) {
            return manager;
        }

        manager = PydevPlugin.getJythonInterpreterManager();
        if (manager.isConfigured()) {
            return manager;
        }

        manager = PydevPlugin.getIronpythonInterpreterManager();
        if (manager.isConfigured()) {
            return manager;
        }

        return null;
    }

}
