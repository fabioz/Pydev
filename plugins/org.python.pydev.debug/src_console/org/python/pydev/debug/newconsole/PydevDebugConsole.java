/******************************************************************************
* Copyright (C) 2012  Hussain Bohra and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Hussain Bohra <hussain.bohra@tavant.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>       - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.newconsole;

import org.python.pydev.debug.newconsole.prefs.ColorManager;

public class PydevDebugConsole extends PydevConsole {

    public static final String CONSOLE_NAME = "PyDev Debug Console";

    public static int debugConsoleId = -1;

    private static String getNextId() {
        debugConsoleId += 1;
        return String.valueOf(debugConsoleId);
    }

    public PydevDebugConsole(PydevConsoleInterpreter interpreter, String additionalInitialComands) {
        super(interpreter, additionalInitialComands);
        setType(PydevConsoleConstants.DEBUG_CONSOLE_TYPE);
        setName(CONSOLE_NAME + " [" + getNextId() + "]");
        this.setPydevConsoleBackground(ColorManager.getDefault().getDebugConsoleBackgroundColor());
    }

}
