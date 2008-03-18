/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console.ui;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.part.IPageBookViewPage;
import org.python.pydev.dltk.console.IScriptConsoleInterpreter;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.dltk.console.ScriptConsoleHistory;
import org.python.pydev.dltk.console.ScriptConsolePrompt;
import org.python.pydev.dltk.console.ui.internal.ICommandHandler;
import org.python.pydev.dltk.console.ui.internal.ScriptConsoleInput;
import org.python.pydev.dltk.console.ui.internal.ScriptConsolePage;
import org.python.pydev.dltk.console.ui.internal.ScriptConsoleSession;
import org.python.pydev.plugin.PydevPlugin;

public class ScriptConsole extends TextConsole implements ICommandHandler {

    private ScriptConsolePage page;

    private ScriptConsolePartitioner partitioner;

    private IContentAssistProcessor processor;

    private ITextHover hover;

    private IScriptConsoleInterpreter interpreter;

    private ScriptConsoleSession session;

    private ListenerList consoleListeners;

    private ScriptConsolePrompt prompt;

    private ScriptConsoleHistory history;

    protected IConsoleDocumentPartitioner getPartitioner() {
        return partitioner;
    }

    public ScriptConsole(String consoleName, String consoleType) {
        super(consoleName, consoleType, null, true);

        this.consoleListeners = new ListenerList(ListenerList.IDENTITY);
        this.prompt = new ScriptConsolePrompt("=>", "->"); //$NON-NLS-1$ //$NON-NLS-2$
        this.history = new ScriptConsoleHistory();

        this.session = new ScriptConsoleSession();
        addListener(this.session);

        partitioner = new ScriptConsolePartitioner();
        getDocument().setDocumentPartitioner(partitioner);
        partitioner.connect(getDocument());
    }

    public IScriptConsoleSession getSession() {
        return session;
    }

    public void addListener(IScriptConsoleListener listener) {
        consoleListeners.add(listener);
    }

    public void removeListener(IScriptConsoleListener listener) {
        consoleListeners.remove(listener);
    }

    protected void setContentAssistProcessor(IContentAssistProcessor processor) {
        this.processor = processor;
    }

    protected void setInterpreter(IScriptConsoleInterpreter interpreter) {
        this.interpreter = interpreter;
//		interpreter.addInitialListenerOperation(new Runnable() {
//			public void run() {
//				Object[] listeners = consoleListeners.getListeners();
//				String output = ScriptConsole.this.interpreter
//						.getInitialOuput();
//				if (output != null) {
//					for (int i = 0; i < listeners.length; i++) {
//						((IScriptConsoleListener) listeners[i])
//								.interpreterResponse(output);
//					}
//				}
//			}
//		});
    }

    public void setPrompt(ScriptConsolePrompt prompt) {
        this.prompt = prompt;
    }

    public ScriptConsolePrompt getPrompt() {
        return prompt;
    }

    public ScriptConsoleHistory getHistory() {
        return history;
    }

    protected void setTextHover(ITextHover hover) {
        this.hover = hover;
    }

    public IPageBookViewPage createPage(IConsoleView view) {
        SourceViewerConfiguration cfg = new ScriptConsoleSourceViewerConfiguration(processor, hover);
        page = new ScriptConsolePage(this, view, cfg);
        return page;
    }

    public void clearConsole() {
        page.clearConsolePage();
    }

    public IScriptConsoleInput getInput() {
        return new ScriptConsoleInput(page);
    }

    public InterpreterResponse handleCommand(String userInput) throws Exception {
        Object[] listeners = consoleListeners.getListeners();
        for (Object listener:listeners) {
            ((IScriptConsoleListener)listener).userRequest(userInput);
        }

        InterpreterResponse response = interpreter.exec(userInput);

        prompt.setMode(!response.more);

        for (Object listener:listeners) {
            ((IScriptConsoleListener)listener).interpreterResponse(response.out);
            ((IScriptConsoleListener)listener).interpreterResponse(response.err);
        }

        return response;
    }

    public void terminate() {
        try {
            interpreter.close();
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }
}
