/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *

 *******************************************************************************/
package org.python.pydev.shared_interactive_console.console.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.IPageBookViewPage;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.Reflection;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleCommunication;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleInterpreter;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_interactive_console.console.ScriptConsoleHistory;
import org.python.pydev.shared_interactive_console.console.ScriptConsolePrompt;
import org.python.pydev.shared_interactive_console.console.ui.internal.ICommandHandler;
import org.python.pydev.shared_interactive_console.console.ui.internal.IHandleScriptAutoEditStrategy;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsolePage;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleSession;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleViewer;
import org.python.pydev.shared_interactive_console.console.ui.internal.actions.AbstractHandleBackspaceAction;
import org.python.pydev.shared_ui.content_assist.AbstractCompletionProcessorWithCycling;

public abstract class ScriptConsole extends TextConsole implements ICommandHandler {

    protected ScriptConsolePage page;

    protected ScriptConsolePartitioner partitioner;

    protected IScriptConsoleInterpreter interpreter;

    protected ScriptConsoleSession session;

    protected ListenerList consoleListeners;

    protected ScriptConsolePrompt prompt;

    protected ScriptConsoleHistory history;

    private WeakReference<ScriptConsoleViewer> viewer;

    public static final String DEFAULT_CONSOLE_TYPE = "org.python.pydev.debug.newconsole.PydevConsole";

    public static final String SCRIPT_DEBUG_CONSOLE_IN_PROCESS_CONSOLE = "SCRIPT_DEBUG_CONSOLE_IN_PROCESS_CONSOLE";

    // Backward-compatibility
    public static ScriptConsole getActiveScriptConsole(String ignored) {
        return getActiveScriptConsole();
    }

    /**
     * @return the currently active script console.
     */
    @SuppressWarnings("restriction")
    public static ScriptConsole getActiveScriptConsole() {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {

                List<IViewPart> consoleParts = getConsoleParts(page, false);
                if (consoleParts.size() == 0) {
                    consoleParts = getConsoleParts(page, true);
                }

                if (consoleParts.size() > 0) {
                    IConsoleView view = null;
                    long lastChangeMillis = Long.MIN_VALUE;

                    if (consoleParts.size() == 1) {
                        view = (IConsoleView) consoleParts.get(0);
                    } else {
                        //more than 1 view available
                        for (int i = 0; i < consoleParts.size(); i++) {
                            IConsoleView temp = (IConsoleView) consoleParts.get(i);
                            IConsole console = temp.getConsole();
                            if (console instanceof ScriptConsole) {
                                ScriptConsole tempConsole = (ScriptConsole) console;
                                ScriptConsoleViewer viewer = tempConsole.getViewer();

                                long tempLastChangeMillis = viewer.getLastChangeMillis();
                                if (tempLastChangeMillis > lastChangeMillis) {
                                    lastChangeMillis = tempLastChangeMillis;
                                    view = temp;
                                }
                            }
                        }
                    }

                    if (view != null) {
                        IConsole console = view.getConsole();

                        if (console instanceof ScriptConsole) {
                            return (ScriptConsole) console;
                        } else {
                            if (console instanceof ProcessConsole) {
                                ProcessConsole processConsole = (ProcessConsole) console;
                                Object scriptConsole = processConsole
                                        .getAttribute(ScriptConsole.SCRIPT_DEBUG_CONSOLE_IN_PROCESS_CONSOLE);
                                if (scriptConsole instanceof ScriptConsole) {
                                    ScriptConsole scriptConsole2 = (ScriptConsole) scriptConsole;
                                    IScriptConsoleCommunication consoleCommunication = scriptConsole2.getInterpreter()
                                            .getConsoleCommunication();
                                    if (consoleCommunication.isConnected()) {
                                        return scriptConsole2;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param page the page where the console view is
     * @param restore whether we should try to restore it
     * @return a list with the parts containing the console
     */
    private static List<IViewPart> getConsoleParts(IWorkbenchPage page, boolean restore) {
        List<IViewPart> consoleParts = new ArrayList<IViewPart>();

        IViewReference[] viewReferences = page.getViewReferences();
        for (IViewReference ref : viewReferences) {
            if (ref.getId().equals(IConsoleConstants.ID_CONSOLE_VIEW)) {
                IViewPart part = ref.getView(restore);
                if (part != null) {
                    consoleParts.add(part);
                    if (restore) {
                        return consoleParts;
                    }
                }
            }
        }
        return consoleParts;
    }

    @Override
    protected IConsoleDocumentPartitioner getPartitioner() {
        return partitioner;
    }

    public ScriptConsole(String consoleName, String consoleType, IScriptConsoleInterpreter interpreterArg) {
        super(consoleName, consoleType, null, true);

        this.interpreter = interpreterArg;

        this.consoleListeners = new ListenerList(ListenerList.IDENTITY);
        this.prompt = createConsolePrompt();
        this.history = new ScriptConsoleHistory();

        this.session = new ScriptConsoleSession();
        addListener(this.session);

        partitioner = new ScriptConsolePartitioner();
        getDocument().setDocumentPartitioner(partitioner);
        partitioner.connect(getDocument());
    }

    /**
     * @return the assistant that should handle content assist requests (code completion)
     */
    protected abstract IContentAssistProcessor createConsoleCompletionProcessor(ContentAssistant pyContentAssistant);

    /**
     * @return the assistant that should handle quick assist requests (quick fixes)
     */
    protected abstract IQuickAssistProcessor createConsoleQuickAssistProcessor(QuickAssistAssistant quickAssist);

    /**
     * @return the text hover to be used in the console.
     */
    protected abstract ITextHover createHover();

    /**
     * @return the console prompt that should be used.
     */
    protected abstract ScriptConsolePrompt createConsolePrompt();

    public IScriptConsoleSession getSession() {
        return session;
    }

    public void addListener(IScriptConsoleListener listener) {
        consoleListeners.add(listener);
    }

    public void removeListener(IScriptConsoleListener listener) {
        consoleListeners.remove(listener);
    }

    protected void setInterpreter(IScriptConsoleInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public IScriptConsoleInterpreter getInterpreter() {
        return interpreter;
    }

    public ScriptConsolePrompt getPrompt() {
        return prompt;
    }

    public ScriptConsoleHistory getHistory() {
        return history;
    }

    /**
     * Creates the actual page to be shown to the user.
     */
    @Override
    public IPageBookViewPage createPage(IConsoleView view) {
        page = new ScriptConsolePage(this, view, createSourceViewerConfiguration());
        return page;
    }

    public abstract SourceViewerConfiguration createSourceViewerConfiguration();

    /**
     * Clears the console
     */
    @Override
    public void clearConsole() {
        page.clearConsolePage();
    }

    @Override
    public void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived) {
        interpreter.setOnContentsReceivedCallback(onContentsReceived);
    }

    @Override
    public void beforeHandleCommand(String userInput, ICallback<Object, InterpreterResponse> onResponseReceived) {
        final Object[] listeners = consoleListeners.getListeners();

        //notify about the user request in the UI thread.
        for (Object listener : listeners) {
            ((IScriptConsoleListener) listener).userRequest(userInput, prompt);
        }
    }

    /**
     * Handles some command that the user entered
     *
     * @param userInput that's the command to be evaluated by the user.
     */
    public void handleCommand(String userInput, final ICallback<Object, InterpreterResponse> onResponseReceived) {
        final Object[] listeners = consoleListeners.getListeners();

        //executes the user input in the interpreter
        if (interpreter != null) {
            interpreter.exec(userInput, new ICallback<Object, InterpreterResponse>() {

                public Object call(final InterpreterResponse response) {
                    //sets the new mode
                    prompt.setMode(!response.more);
                    prompt.setNeedInput(response.need_input);

                    //notify about the console answer (not in the UI thread).
                    for (Object listener : listeners) {
                        ((IScriptConsoleListener) listener).interpreterResponse(response, prompt);
                    }
                    onResponseReceived.call(response);
                    return null;
                }
            });
        }

    }

    /**
     * Fetch the current completions for the content presented in the user's ipython console
     */
    public ICompletionProposal[] getTabCompletions(String commandLine, int cursorPosition) {
        try {
            ICompletionProposal[] completions = interpreter.getCompletions(viewer.get(), commandLine, cursorPosition,
                    cursorPosition, AbstractCompletionProcessorWithCycling.SHOW_FOR_TAB_COMPLETIONS);
            return completions;
        } catch (Exception e) {
        }
        return new ICompletionProposal[0];
    }

    /**
     * Finishes the interpreter (and stops the communication)
     */
    public void terminate() {
        try {
            if (history != null) {
                history.close();
            }
            interpreter.close();
        } catch (Exception e) {
        }
        history = null;
        interpreter = null;
    }

    /**
     * Interrupts the interpreter
     */
    public void interrupt() {
        try {
            interpreter.interrupt();
            getViewer().discardCommandLine();
        } catch (Exception e) {
        }
    }

    public void setViewer(ScriptConsoleViewer scriptConsoleViewer) {
        this.viewer = new WeakReference<ScriptConsoleViewer>(scriptConsoleViewer);
    }

    public ScriptConsoleViewer getViewer() {
        if (this.viewer != null) {
            return this.viewer.get();
        }
        return null;
    }

    /**
     * Must be overridden to create a style provider for the console (configures colors)
     * @return a style provider.
     */
    public abstract IConsoleStyleProvider createStyleProvider();

    /**
     * @return a list of trackers that'll identify links in the console passed.
     */
    public abstract List<IConsoleLineTracker> createLineTrackers(final TextConsole console);

    /**
     * @return the commands that should be initially set in the prompt.
     */
    public abstract String getInitialCommands();

    /**
     * Used for backward compatibility because the setBackground/getBackground is not available for eclipse 3.2
     */
    private Color fPydevConsoleBackground;

    /**
     * Used for backward compatibility because the setBackground/getBackground is not available for eclipse 3.2
     */
    public Color getPydevConsoleBackground() {
        try {
            Color ret = (Color) Reflection.invoke(this, "getBackground");
            return ret;
        } catch (Throwable e) {
            //not available in eclipse 3.2
            return fPydevConsoleBackground;
        }
    }

    /**
     * Used for backward compatibility because the setBackground/getBackground is not available for eclipse 3.2
     */
    public void setPydevConsoleBackground(Color color) {
        try {
            Reflection.invoke(this, "setBackground", color);
        } catch (Throwable e) {
            //not available in eclipse 3.2
            fPydevConsoleBackground = color;
        }
    }

    public Object getInterpreterInfo() {
        return this.interpreter.getInterpreterInfo();
    }

    /**
     * @return
     */
    public abstract boolean getFocusOnStart();

    public abstract boolean getTabCompletionEnabled();

    /**
     * Enable/Disable linking of the debug console with the suspended frame.
     */
    public void linkWithDebugSelection(boolean isLinkedWithDebug) {
        this.interpreter.linkWithDebugSelection(isLinkedWithDebug);
    }

    public abstract AbstractHandleBackspaceAction getBackspaceAction();

    public abstract void createActions(IToolBarManager toolbarManager);

    public abstract IHandleScriptAutoEditStrategy getAutoEditStrategy();
}
