package org.python.pydev.debug.newconsole;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextHover;
import org.python.pydev.dltk.console.ScriptConsolePrompt;
import org.python.pydev.dltk.console.ui.ScriptConsole;
import org.python.pydev.plugin.PydevPlugin;

/**
 * The pydev console creates the basic stuff to work as a script console.
 *
 * @author Fabio
 */
public class PydevConsole extends ScriptConsole {

    public static final String CONSOLE_TYPE = "org.python.pydev.debug.newconsole.PydevConsole";

    public static final String CONSOLE_NAME = "Pydev Console";

    public static int nextId = -1;
    
    
    private static String getNextId() {
        nextId += 1;
        return String.valueOf(nextId);
    }
    
    public PydevConsole(PydevConsoleInterpreter interpreter) {
        super(CONSOLE_NAME + " [" + getNextId() + "]", CONSOLE_TYPE, interpreter);
    }

    
    /**
     * The completion processor for pydev.
     */
    @Override
    protected PydevConsoleCompletionProcessor createConsoleCompletionProcessor() {
        return new PydevConsoleCompletionProcessor(interpreter);
    }
    
    /**
     * @return the text hover to be used in the console.
     */
    @Override
    protected ITextHover createHover() {
        return new PydevConsoleTextHover(this.interpreter);
    }
    
    /**
     * @return the prompt to be used in the console.
     */
    @Override
    protected ScriptConsolePrompt createConsolePrompt() {
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();

        String newPrompt = store.getString(PydevConsoleConstants.PREF_NEW_PROMPT);
        String continuePrompt = store.getString(PydevConsoleConstants.PREF_CONTINUE_PROMPT);

        if (newPrompt == null || newPrompt.length() == 0) {
            newPrompt = PydevConsoleConstants.DEFAULT_NEW_PROMPT;
        }
        if (continuePrompt == null || continuePrompt.length() == 0) {
            continuePrompt = PydevConsoleConstants.DEFAULT_CONTINUE_PROMPT;
        }

        return new ScriptConsolePrompt(newPrompt, continuePrompt);
    }

}
