package org.python.pydev.debug.newconsole;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.prefs.ColorManager;
import org.python.pydev.debug.ui.PythonConsoleLineTracker;
import org.python.pydev.dltk.console.ScriptConsolePrompt;
import org.python.pydev.dltk.console.ui.IConsoleStyleProvider;
import org.python.pydev.dltk.console.ui.ScriptConsole;
import org.python.pydev.dltk.console.ui.ScriptStyleRange;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;
import org.python.pydev.plugin.PydevPlugin;

/**
 * The pydev console creates the basic stuff to work as a script console.
 *
 * @author Fabio
 */
public class PydevConsole extends ScriptConsole implements IConsole {

    public static final String CONSOLE_NAME = "Pydev Console";

    public static int nextId = -1;
    
    
    private static String getNextId() {
        nextId += 1;
        return String.valueOf(nextId);
    }
    
    public PydevConsole(PydevConsoleInterpreter interpreter) {
        super(CONSOLE_NAME + " [" + getNextId() + "]", PydevConsoleConstants.CONSOLE_TYPE, interpreter);
        setBackground(ColorManager.getPreferenceColor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR));
    }

    
    /**
     * Can be overridden to create a style provider for the console.
     * @return a style provider.
     */
    public IConsoleStyleProvider createStyleProvider() {
        return new IConsoleStyleProvider(){

            private ScriptStyleRange getIt(String content, int offset, String foregroundPrefName, int scriptStyle){
                Color foreground = ColorManager.getPreferenceColor(foregroundPrefName);
                
                //background is the default (already set)
                return new ScriptStyleRange(offset, content.length(), foreground, null, scriptStyle);
            }
            
            public ScriptStyleRange createInterpreterErrorStyle(String content, int offset) {
                return getIt(content, offset, PydevConsoleConstants.CONSOLE_SYS_ERR_COLOR, ScriptStyleRange.STDERR);
            }

            public ScriptStyleRange createInterpreterOutputStyle(String content, int offset) {
                return getIt(content, offset, PydevConsoleConstants.CONSOLE_SYS_OUT_COLOR, ScriptStyleRange.STDOUT);
            }

            public ScriptStyleRange createPromptStyle(String content, int offset) {
                return getIt(content, offset, PydevConsoleConstants.CONSOLE_PROMPT_COLOR, ScriptStyleRange.PROMPT);
            }

            public ScriptStyleRange createUserInputStyle(String content, int offset) {
                return getIt(content, offset, PydevConsoleConstants.CONSOLE_SYS_IN_COLOR, ScriptStyleRange.STDIN);
            }
            
        };
    }


    
    /**
     * The completion processor for pydev.
     */
    @Override
    protected PydevConsoleCompletionProcessor createConsoleCompletionProcessor(PyContentAssistant pyContentAssistant) {
        return new PydevConsoleCompletionProcessor(interpreter, pyContentAssistant);
    }
    
    @Override
    protected IQuickAssistProcessor createConsoleQuickAssistProcessor(PyCorrectionAssistant quickAssist) {
        return new PydevConsoleQuickAssistProcessor(quickAssist);
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

    
    /**
     * Overridden to get the line trackers that'll add hyperlinks to the console.
     */
    @Override
    public List<IConsoleLineTracker> getLineTrackers() {
        List<IConsoleLineTracker> lineTrackers = new ArrayList<IConsoleLineTracker>();
        PythonConsoleLineTracker lineTracker = new PythonConsoleLineTracker();
        lineTracker.init(this);
        lineTrackers.add(lineTracker);
        return lineTrackers;
    }

    /**
     * @return the initial commands set in the preferences
     */
    @Override
    public String getInitialCommands() {
        return PydevDebugPlugin.getDefault().getPreferenceStore().
            getString(PydevConsoleConstants.INITIAL_INTERPRETER_CMDS);
    }
    
    /**
     * IConsole: Add a link to the console
     */
    public void addLink(IConsoleHyperlink link, int offset, int length) {
        this.addLink((IHyperlink)link, offset, length);
    }

    /**
     * IConsole: Add a link to the console
     */
    public void addLink(IHyperlink link, int offset, int length) {
        try {
            super.addHyperlink(link, offset, length);
        } catch (BadLocationException e) {
            PydevPlugin.log(e);
        }
    }

    
    //required by the IConsole interface -- because of hyperlinks (but not actually used)
    public void connect(IStreamsProxy streamsProxy) {
        throw new RuntimeException("Not implemented");
    }

    //required by the IConsole interface -- because of hyperlinks (but not actually used)
    public void connect(IStreamMonitor streamMonitor, String streamIdentifer) {
        throw new RuntimeException("Not implemented");
    }

    //required by the IConsole interface -- because of hyperlinks (but not actually used)
    public IProcess getProcess() {
        throw new RuntimeException("Not implemented");
    }

    //required by the IConsole interface -- because of hyperlinks (but not actually used)
    public IRegion getRegion(IConsoleHyperlink link) {
        throw new RuntimeException("Not implemented");
    }

    //required by the IConsole interface -- because of hyperlinks (but not actually used)
    public IOConsoleOutputStream getStream(String streamIdentifier) {
        throw new RuntimeException("Not implemented");
    }
}
