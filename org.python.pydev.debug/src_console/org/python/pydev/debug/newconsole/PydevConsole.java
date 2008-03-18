package org.python.pydev.debug.newconsole;

import org.python.pydev.dltk.console.ui.ScriptConsole;

public class PydevConsole extends ScriptConsole {

    public static final String CONSOLE_TYPE = "org.python.pydev.debug.newconsole.PydevConsole";

    public static final String CONSOLE_NAME = "Pydev Console";

    public static int nextId = -1;
    
    
    private static String getNextId() {
        nextId += 1;
        return String.valueOf(nextId);
    }
    
    public PydevConsole(PydevConsoleInterpreter interpreter) {
        super(CONSOLE_NAME + " [" + getNextId() + "]", CONSOLE_TYPE);
        

        setInterpreter(interpreter);
        setTextHover(new PydevConsoleTextHover(interpreter));
        setContentAssistProcessor(new PydevConsoleCompletionProcessor(interpreter));
    }
    
}
