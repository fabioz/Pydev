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
