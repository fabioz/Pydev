/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.launching;

import org.eclipse.core.expressions.PropertyTester;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;


public class InterpreterTypeTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        IInterpreterManager interpreterManager = null;
        String str = expectedValue.toString();

        if ("python".equals(str)) {
            interpreterManager = PydevPlugin.getPythonInterpreterManager();
        } else if ("jython".equals(str)) {
            interpreterManager = PydevPlugin.getJythonInterpreterManager();
        } else if ("ironpython".equals(str)) {
            interpreterManager = PydevPlugin.getIronpythonInterpreterManager();
        } else {
            Log.log("Unable to check for: " + expectedValue);
        }

        if (interpreterManager != null) {
            try {
                String defaultInterpreter = interpreterManager.getDefaultInterpreterInfo(false).getExecutableOrJar();
                return defaultInterpreter != null;
            } catch (MisconfigurationException e) {
                return false;
            }
        }
        return false;
    }

}