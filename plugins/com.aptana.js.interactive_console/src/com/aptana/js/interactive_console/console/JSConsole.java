/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.console.IHyperlink;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;
import org.python.pydev.plugin.PydevPlugin;

import com.aptana.interactive_console.console.ScriptConsolePrompt;
import com.aptana.interactive_console.console.ui.DefaultScriptConsoleTextHover;
import com.aptana.interactive_console.console.ui.IConsoleStyleProvider;
import com.aptana.interactive_console.console.ui.ScriptConsole;
import com.aptana.interactive_console.console.ui.internal.IHandleScriptAutoEditStrategy;
import com.aptana.interactive_console.console.ui.internal.actions.AbstractHandleBackspaceAction;
import com.aptana.js.interactive_console.console.prefs.ColorManager;
import com.aptana.js.interactive_console.console.prefs.InteractiveConsolePrefs;

/**
 * The pydev console creates the basic stuff to work as a script console.
 *
 * @author Fabio
 */
public class JSConsole extends ScriptConsole {

    public static final String CONSOLE_NAME = "JS Console";

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

    public JSConsole(JSConsoleInterpreter interpreter, String additionalInitialComands) {
        super(CONSOLE_NAME + " [" + getNextId() + "]", JSConsoleConstants.CONSOLE_TYPE, interpreter);
        this.additionalInitialComands = additionalInitialComands;
        this.setPydevConsoleBackground(ColorManager.getDefault().getConsoleBackgroundColor());
        //Cannot be called directly because Eclipse 3.2does not support it.
        //setBackground(ColorManager.getPreferenceColor(PydevConsoleConstants.CONSOLE_BACKGROUND_COLOR));
    }

    public IConsoleStyleProvider createStyleProvider() {
        return new ConsoleStyleProvider();
    }

    /**
     * The completion processor for pydev.
     */
    @Override
    protected JSConsoleCompletionProcessor createConsoleCompletionProcessor(ContentAssistant pyContentAssistant) {
        return new JSConsoleCompletionProcessor(interpreter, (PyContentAssistant) pyContentAssistant);
    }

    @Override
    protected IQuickAssistProcessor createConsoleQuickAssistProcessor(QuickAssistAssistant quickAssist) {
        return new JSConsoleQuickAssistProcessor((PyCorrectionAssistant) quickAssist);
    }

    @Override
    protected SourceViewerConfiguration createSourceViewerConfiguration() {
        PyContentAssistant contentAssist = new PyContentAssistant();
        IContentAssistProcessor processor = createConsoleCompletionProcessor(contentAssist);
        contentAssist.setContentAssistProcessor(processor, JSScriptConsoleSourceViewerConfiguration.PARTITION_TYPE);

        contentAssist.enableAutoActivation(true);
        contentAssist.enableAutoInsert(false);
        contentAssist.setAutoActivationDelay(PyCodeCompletionPreferencesPage.getAutocompleteDelay());

        PyCorrectionAssistant quickAssist = new PyCorrectionAssistant();
        // next create a content assistant processor to populate the completions window
        IQuickAssistProcessor quickAssistProcessor = createConsoleQuickAssistProcessor(quickAssist);

        // Correction assist works on all
        quickAssist.setQuickAssistProcessor(quickAssistProcessor);

        SourceViewerConfiguration cfg = new JSScriptConsoleSourceViewerConfiguration(createHover(), contentAssist,
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

        String newPrompt = store.getString(JSConsoleConstants.PREF_NEW_PROMPT);
        String continuePrompt = store.getString(JSConsoleConstants.PREF_CONTINUE_PROMPT);

        if (newPrompt == null || newPrompt.length() == 0) {
            newPrompt = JSConsoleConstants.DEFAULT_NEW_PROMPT;
        }
        if (continuePrompt == null || continuePrompt.length() == 0) {
            continuePrompt = JSConsoleConstants.DEFAULT_CONTINUE_PROMPT;
        }

        return new ScriptConsolePrompt(newPrompt, continuePrompt);
    }

    /**
     * Overridden to get the line trackers that'll add hyperlinks to the console.
     */
    @Override
    public List<IConsoleLineTracker> getLineTrackers() {
        List<IConsoleLineTracker> lineTrackers = new ArrayList<IConsoleLineTracker>();
        //        PythonConsoleLineTracker lineTracker = new PythonConsoleLineTracker();
        //
        //        //The IConsole we implement in this class is not the same IConsole that's needed in the
        //        //lineTracker, so, let's create a wrapper for this with the interfaces requested.
        //        lineTracker.init(new IConsole() {
        //
        //            //IMPLEMENTATIONS FORWARDED TO OUTER CLASS
        //            public void addLink(IConsoleHyperlink link, int offset, int length) {
        //                PydevConsole.this.addLink(link, offset, length);
        //            }
        //
        //            public void addLink(IHyperlink link, int offset, int length) {
        //                PydevConsole.this.addLink(link, offset, length);
        //            }
        //
        //            public void addPatternMatchListener(IPatternMatchListener matchListener) {
        //                PydevConsole.this.addPatternMatchListener(matchListener);
        //            }
        //
        //            public IDocument getDocument() {
        //                return PydevConsole.this.getDocument();
        //            }
        //
        //            public IRegion getRegion(IConsoleHyperlink link) {
        //                return PydevConsole.this.getRegion(link);
        //            }
        //
        //            public IRegion getRegion(IHyperlink link) {
        //                return PydevConsole.this.getRegion(link);
        //            }
        //
        //            public void removePatternMatchListener(IPatternMatchListener matchListener) {
        //                PydevConsole.this.removePatternMatchListener(matchListener);
        //            }
        //
        //            //IMPLEMENTATIONS THAT AREN'T REALLY AVAILABLE IN THE PYDEV CONSOLE
        //            public void connect(IStreamsProxy streamsProxy) {
        //                /**EMPTY**/
        //            }
        //
        //            public void connect(IStreamMonitor streamMonitor, String streamIdentifer) {
        //                /**EMPTY**/
        //            }
        //
        //            public IProcess getProcess() {
        //                return null;
        //                /**EMPTY**/
        //            }
        //
        //            public IOConsoleOutputStream getStream(String streamIdentifier) {
        //                return null;
        //                /**EMPTY**/
        //            }
        //        });
        //
        //        lineTrackers.add(lineTracker);
        return lineTrackers;
    }

    /**
     * @return the initial commands set in the preferences
     */
    @Override
    public String getInitialCommands() {
        return "";
        //        String str = PydevDebugPlugin.getDefault().getPreferenceStore()
        //                .getString(PydevConsoleConstants.INITIAL_INTERPRETER_CMDS);
        //        if (additionalInitialComands != null) {
        //            str += additionalInitialComands;
        //        }
        //        return str;
    }

    @Override
    public boolean getFocusOnStart() {
        return InteractiveConsolePrefs.getFocusConsoleOnStartup();
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

    @Override
    public void createActions(IToolBarManager toolbarManager) {

    }

    @Override
    public IHandleScriptAutoEditStrategy getAutoEditStrategy() {
        return new PyAutoIndentStrategy();
    }
}
