/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.TextConsole;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.actions.LinkWithDebugSelectionAction;
import org.python.pydev.debug.newconsole.prefs.ColorManager;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs;
import org.python.pydev.debug.ui.PythonConsoleLineTracker;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_interactive_console.console.ScriptConsolePrompt;
import org.python.pydev.shared_interactive_console.console.ui.DefaultScriptConsoleTextHover;
import org.python.pydev.shared_interactive_console.console.ui.IConsoleStyleProvider;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsole;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleUIConstants;
import org.python.pydev.shared_interactive_console.console.ui.internal.IHandleScriptAutoEditStrategy;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleMessages;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsolePage;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.AbstractHandleBackspaceAction;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * The pydev console creates the basic stuff to work as a script console.
 *
 * @author Fabio
 */
public class PydevConsole extends ScriptConsole {

    public static final String CONSOLE_NAME = "PyDev Console";

    public static int nextId = -1;

    private String additionalInitialComands;

    /**
     * Eclipse process that this console is viewing. Only non-null if there is a
     * corresponding Launch/Debug Target connected to the same console
     */
    private IProcess process = null;

    private static String getNextId() {
        nextId += 1;
        return String.valueOf(nextId);
    }

    public PydevConsole(PydevConsoleInterpreter interpreter, String additionalInitialComands) {
        super(CONSOLE_NAME + " [" + getNextId() + "]", PydevConsoleConstants.CONSOLE_TYPE, interpreter);
        this.additionalInitialComands = additionalInitialComands;
        boolean runNowIfInUiThread = true;
        RunInUiThread.async(new Runnable() {

            @Override
            public void run() {
                setPydevConsoleBackground(ColorManager.getDefault().getConsoleBackgroundColor());
                //Cannot be called directly because Eclipse 3.2does not support it.
                //setBackground(ColorManager.getPreferenceColor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR));
            }
        }, runNowIfInUiThread);
    }

    @Override
    public IConsoleStyleProvider createStyleProvider() {
        return new ConsoleStyleProvider();
    }

    /**
     * The completion processor for pydev.
     */
    @Override
    protected PydevConsoleCompletionProcessor createConsoleCompletionProcessor(ContentAssistant pyContentAssistant) {
        return new PydevConsoleCompletionProcessor(interpreter, (PyContentAssistant) pyContentAssistant);
    }

    @Override
    protected IQuickAssistProcessor createConsoleQuickAssistProcessor(QuickAssistAssistant quickAssist) {
        return new PydevConsoleQuickAssistProcessor((PyCorrectionAssistant) quickAssist);
    }

    @Override
    public SourceViewerConfiguration createSourceViewerConfiguration() {
        PyContentAssistant contentAssist = new PyContentAssistant();
        IContentAssistProcessor processor = createConsoleCompletionProcessor(contentAssist);
        contentAssist.setContentAssistProcessor(processor, PydevScriptConsoleSourceViewerConfiguration.PARTITION_TYPE);

        contentAssist.enableAutoActivation(true);
        contentAssist.enableAutoInsert(false);
        contentAssist.setAutoActivationDelay(PyCodeCompletionPreferencesPage.getAutocompleteDelay());

        PyCorrectionAssistant quickAssist = new PyCorrectionAssistant();
        // next create a content assistant processor to populate the completions window
        IQuickAssistProcessor quickAssistProcessor = createConsoleQuickAssistProcessor(quickAssist);

        // Correction assist works on all
        quickAssist.setQuickAssistProcessor(quickAssistProcessor);

        SourceViewerConfiguration cfg = new PydevScriptConsoleSourceViewerConfiguration(createHover(), contentAssist,
                quickAssist);
        return cfg;
    }

    /**
     * @return the text hover to be used in the console.
     */
    @Override
    protected ITextHover createHover() {
        return new DefaultScriptConsoleTextHover(this.interpreter);
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
    public List<IConsoleLineTracker> createLineTrackers(final TextConsole console) {
        return staticCreateLineTrackers(console);
    }

    /**
     * Static so that we know it has no connection to this console (only the one passed in the parameter).
     */
    private static List<IConsoleLineTracker> staticCreateLineTrackers(final TextConsole console) {
        List<IConsoleLineTracker> lineTrackers = new ArrayList<IConsoleLineTracker>();
        PythonConsoleLineTracker lineTracker = new PythonConsoleLineTracker();

        //The IConsole we implement in this class is not the same IConsole that's needed in the
        //lineTracker, so, let's create a wrapper for this with the interfaces requested.
        lineTracker.init(new IConsole() {

            //IMPLEMENTATIONS FORWARDED TO OUTER CLASS
            public void addLink(IConsoleHyperlink link, int offset, int length) {
                try {
                    console.addHyperlink(link, offset, length);
                } catch (BadLocationException e) {
                    Log.log(e);
                }
            }

            public void addLink(IHyperlink link, int offset, int length) {
                try {
                    console.addHyperlink(link, offset, length);
                } catch (BadLocationException e) {
                    Log.log(e);
                }
            }

            public void addPatternMatchListener(IPatternMatchListener matchListener) {
                console.addPatternMatchListener(matchListener);
            }

            public IDocument getDocument() {
                return console.getDocument();
            }

            public IRegion getRegion(IConsoleHyperlink link) {
                return console.getRegion(link);
            }

            public IRegion getRegion(IHyperlink link) {
                return console.getRegion(link);
            }

            public void removePatternMatchListener(IPatternMatchListener matchListener) {
                console.removePatternMatchListener(matchListener);
            }

            //IMPLEMENTATIONS THAT AREN'T REALLY AVAILABLE IN THE PYDEV CONSOLE
            public void connect(IStreamsProxy streamsProxy) {
                /**EMPTY**/
            }

            public void connect(IStreamMonitor streamMonitor, String streamIdentifer) {
                /**EMPTY**/
            }

            public IProcess getProcess() {
                return null;
                /**EMPTY**/
            }

            public IOConsoleOutputStream getStream(String streamIdentifier) {
                return null;
                /**EMPTY**/
            }
        });

        lineTrackers.add(lineTracker);
        return lineTrackers;
    }

    /**
     * @return the initial commands set in the preferences
     */
    @Override
    public String getInitialCommands() {
        String str = PydevDebugPlugin.getDefault().getPreferenceStore().
                getString(PydevConsoleConstants.INITIAL_INTERPRETER_CMDS);
        try {
            // Expand any eclipse variables in the GUI
            IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
            str = manager.performStringSubstitution(str, false);
        } catch (CoreException e) {
            // Unreachable as false passed to reportUndefinedVariables above
            Log.log(e);
        }
        if (!str.endsWith("\n")) {
            str += "\n";
        }

        if (additionalInitialComands != null) {
            str += additionalInitialComands;
        }
        return str;
    }

    @Override
    public boolean getFocusOnStart() {
        return InteractiveConsolePrefs.getFocusConsoleOnStartup();
    }

    @Override
    public boolean getTabCompletionEnabled() {
        return InteractiveConsolePrefs.getTabCompletionInInteractiveConsole();
    }

    /**
     * IConsole: Add a link to the console
     */
    public void addLink(IConsoleHyperlink link, int offset, int length) {
        this.addLink((IHyperlink) link, offset, length);
    }

    /**
     * IConsole: Add a link to the console
     */
    public void addLink(IHyperlink link, int offset, int length) {
        try {
            super.addHyperlink(link, offset, length);
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    /**
     * Eclipse process that this console is viewing. Only non-null if there is a
     * corresponding Launch/Debug Target connected to the same console
     *
     * @return IProcess of viewed process
     */
    public IProcess getProcess() {
        return process;
    }

    /**
     * Eclipse process that this console is viewing.
     *
     * @param process
     *            being viewed
     */
    public void setProcess(IProcess process) {
        this.process = process;
    }

    @Override
    public AbstractHandleBackspaceAction getBackspaceAction() {
        return new HandleBackspaceAction();
    }

    private LinkWithDebugSelectionAction linkWithDebugSelectionAction;

    @Override
    public void createActions(IToolBarManager toolbarManager) {
        if (getType().contains(ScriptConsoleUIConstants.DEBUG_CONSOLE_TYPE)) {
            // initialize LinkWithFrameAction only for Debug Console
            linkWithDebugSelectionAction = new LinkWithDebugSelectionAction(this,
                    ScriptConsoleMessages.LinkWithDebugAction, ScriptConsoleMessages.LinkWithDebugToolTip);
            toolbarManager.appendToGroup(ScriptConsolePage.SCRIPT_GROUP, linkWithDebugSelectionAction);
        }
    }

    @Override
    public IHandleScriptAutoEditStrategy getAutoEditStrategy() {
        return new PyAutoIndentStrategy(new IAdaptable() {

            @Override
            public Object getAdapter(Class adapter) {
                return null;
            }
        });
    }
}
