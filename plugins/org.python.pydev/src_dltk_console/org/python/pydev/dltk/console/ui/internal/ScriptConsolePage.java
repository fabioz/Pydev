/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.python.pydev.dltk.console.ui.internal;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.console.actions.TextViewerAction;
import org.python.pydev.dltk.console.ui.ScriptConsole;
import org.python.pydev.dltk.console.ui.ScriptConsoleUIConstants;
import org.python.pydev.dltk.console.ui.internal.actions.CloseScriptConsoleAction;
import org.python.pydev.dltk.console.ui.internal.actions.LinkWithDebugSelectionAction;
import org.python.pydev.dltk.console.ui.internal.actions.SaveConsoleSessionAction;

public class ScriptConsolePage extends TextConsolePage implements IScriptConsoleContentHandler {

    public static final String SCRIPT_GROUP = "scriptGroup"; //$NON-NLS-1$

    /**
     * Action to request content assist proposals.
     *
     * @author Fabio
     */
    protected class ContentAssistProposalsAction extends TextViewerAction {

        public ContentAssistProposalsAction(ITextViewer viewer) {
            super(viewer, ISourceViewer.CONTENTASSIST_PROPOSALS);
        }
    }

    /**
     * Action to request quick assist proposals.
     *
     * @author Fabio
     */
    protected class QuickAssistProposalsAction extends TextViewerAction {

        public QuickAssistProposalsAction(ITextViewer viewer) {
            super(viewer, ISourceViewer.QUICK_ASSIST);
        }
    }

    private SourceViewerConfiguration cfg;

    private ScriptConsoleViewer viewer;

    private TextViewerAction proposalsAction;

    private TextViewerAction quickAssistAction;

    private SaveConsoleSessionAction saveSessionAction;

    private CloseScriptConsoleAction closeConsoleAction;

    private LinkWithDebugSelectionAction linkWithDebugSelectionAction;

    protected void createActions() {
        super.createActions();

        proposalsAction = new ContentAssistProposalsAction(getViewer());
        quickAssistAction = new QuickAssistProposalsAction(getViewer());

        saveSessionAction = new SaveConsoleSessionAction((ScriptConsole) getConsole(),
                ScriptConsoleMessages.SaveSessionAction, ScriptConsoleMessages.SaveSessionTooltip);

        closeConsoleAction = new CloseScriptConsoleAction((ScriptConsole) getConsole(),
                ScriptConsoleMessages.TerminateConsoleAction, ScriptConsoleMessages.TerminateConsoleTooltip);

        IActionBars bars = getSite().getActionBars();

        IToolBarManager toolbarManager = bars.getToolBarManager();

        toolbarManager.prependToGroup(IConsoleConstants.LAUNCH_GROUP, new GroupMarker(SCRIPT_GROUP));
        toolbarManager.appendToGroup(SCRIPT_GROUP, new Separator());

        toolbarManager.appendToGroup(SCRIPT_GROUP, closeConsoleAction);

        toolbarManager.appendToGroup(SCRIPT_GROUP, saveSessionAction);

        if (getConsole().getType().contains(ScriptConsoleUIConstants.DEBUG_CONSOLE_TYPE)) {
            // initialize LinkWithFrameAction only for Debug Console
            linkWithDebugSelectionAction = new LinkWithDebugSelectionAction((ScriptConsole) getConsole(),
                    ScriptConsoleMessages.LinkWithDebugAction, ScriptConsoleMessages.LinkWithDebugToolTip);
            toolbarManager.appendToGroup(SCRIPT_GROUP, linkWithDebugSelectionAction);
        }

        bars.updateActionBars();
    }

    @Override
    protected void contextMenuAboutToShow(IMenuManager menuManager) {
        super.contextMenuAboutToShow(menuManager);
        menuManager.add(new Separator(SCRIPT_GROUP));
        menuManager.appendToGroup(SCRIPT_GROUP, saveSessionAction);
        menuManager.appendToGroup(SCRIPT_GROUP, closeConsoleAction);
    }

    protected TextConsoleViewer createViewer(Composite parent) {
        ScriptConsole console = (ScriptConsole) getConsole();
        viewer = new ScriptConsoleViewer(parent, console, this, console.createStyleProvider(),
                console.getInitialCommands(), console.getFocusOnStart());
        viewer.configure(cfg);
        return viewer;
    }

    public ScriptConsolePage(ScriptConsole console, IConsoleView view, SourceViewerConfiguration cfg) {
        super(console, view);

        this.cfg = cfg;
    }

    public void clearConsolePage() {
        viewer.clear(false);
    }

    public void contentAssistRequired() {
        proposalsAction.run();
    }

    public void quickAssistRequired() {
        quickAssistAction.run();
    }

}
