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
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.IPageBookViewPage;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.Reflection;
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

public abstract class ScriptConsole extends TextConsole implements ICommandHandler {

    protected ScriptConsolePage page;

    protected ScriptConsolePartitioner partitioner;

    protected IScriptConsoleInterpreter interpreter;

    protected ScriptConsoleSession session;

    protected ListenerList consoleListeners;

    protected ScriptConsolePrompt prompt;

    protected ScriptConsoleHistory history;

    private WeakReference<ScriptConsoleViewer> viewer;

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

    protected abstract SourceViewerConfiguration createSourceViewerConfiguration();

    /**
     * Clears the console
     */
    @Override
    public void clearConsole() {
        page.clearConsolePage();
    }

    /**
     * Handles some command that the user entered
     *
     * @param userInput that's the command to be evaluated by the user.
     */
    public void handleCommand(String userInput, final ICallback<Object, InterpreterResponse> onResponseReceived,
            final ICallback<Object, Tuple<String, String>> onContentsReceived) {
        final Object[] listeners = consoleListeners.getListeners();

        //notify about the user request
        for (Object listener : listeners) {
            ((IScriptConsoleListener) listener).userRequest(userInput, prompt);
        }

        //executes the user input in the interpreter
        if (interpreter != null) {
            interpreter.exec(userInput, new ICallback<Object, InterpreterResponse>() {

                public Object call(final InterpreterResponse response) {
                    //sets the new mode
                    prompt.setMode(!response.more);
                    prompt.setNeedInput(response.need_input);

                    //notify about the console answer
                    for (Object listener : listeners) {
                        ((IScriptConsoleListener) listener).interpreterResponse(response, prompt);
                    }
                    onResponseReceived.call(response);
                    return null;
                }
            }, onContentsReceived);
        }

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
     * @return a list of trackers that'll identify links in the console.
     */
    public abstract List<IConsoleLineTracker> getLineTrackers();

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
