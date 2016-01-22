/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.console;

import java.io.IOException;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.internal.console.IOConsolePartitioner;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.newconsole.CurrentPyStackFrameForConsole;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.PydevDebugConsole;
import org.python.pydev.debug.newconsole.PydevDebugConsoleCommunication;
import org.python.pydev.debug.newconsole.prefs.ColorManager;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleCommunication;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_interactive_console.console.ScriptConsolePrompt;
import org.python.pydev.shared_interactive_console.console.ui.IConsoleStyleProvider;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleListener;
import org.python.pydev.shared_interactive_console.console.ui.internal.IScriptConsoleContentHandler;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleViewer;

@SuppressWarnings("restriction")
public class PromptOverlay implements DisposeListener, Listener, IScriptConsoleContentHandler {

    private static final String IS_PROMPT_OVERLAY_STYLED_TEXT = "IS_PROMPT_OVERLAY_STYLED_TEXT";
    private StyledText interactiveConsoleTextWidget;
    private StyledText styledText;
    private Composite styledTextParent;
    private CustomPageBookLayout customLayout;
    private final CurrentPyStackFrameForConsole currentPyStackFrameForConsole;
    private ScriptConsoleViewer viewer;
    private PromptOverlayReplaceGlobalActionHandlers promptOverlayActionHandlers;
    private boolean overlayVisible = true;
    private double percSize = .3;
    private PydevDebugConsole debugConsole;
    private boolean bufferedOutput = false;

    public PromptOverlay(IOConsolePage consolePage, final ProcessConsole processConsole,
            CurrentPyStackFrameForConsole currentPyStackFrameForConsole) {

        this.currentPyStackFrameForConsole = currentPyStackFrameForConsole;
        SourceViewerConfiguration cfg;
        try {
            ILaunch launch = processConsole.getProcess().getLaunch();
            debugConsole = new PydevConsoleFactory().createDebugConsole(launch, "", false, bufferedOutput,
                    currentPyStackFrameForConsole);
            cfg = debugConsole.createSourceViewerConfiguration();
            processConsole.setAttribute(PydevDebugConsole.SCRIPT_DEBUG_CONSOLE_IN_PROCESS_CONSOLE, debugConsole);
        } catch (Exception e) {
            // If we can't create the debug console, bail out and do nothing else.
            Log.log(e);
            return;
        }

        TextConsoleViewer consoleViewer = consolePage.getViewer();
        final StyledText styledText = (StyledText) consoleViewer.getControl();
        this.styledText = styledText;
        styledTextParent = styledText.getParent();

        final IConsoleStyleProvider styleProvider = debugConsole.createStyleProvider();
        viewer = new ScriptConsoleViewer(styledTextParent, debugConsole, this, styleProvider,
                debugConsole.getInitialCommands(), debugConsole.getFocusOnStart(), debugConsole.getBackspaceAction(),
                debugConsole.getAutoEditStrategy(), debugConsole.getTabCompletionEnabled(), false);
        viewer.configure(cfg);

        Layout currentLayout = styledTextParent.getLayout();
        this.customLayout = new CustomPageBookLayout(currentLayout);
        this.interactiveConsoleTextWidget = viewer.getTextWidget();
        this.interactiveConsoleTextWidget.setData(IS_PROMPT_OVERLAY_STYLED_TEXT, Boolean.TRUE);

        final IOConsoleOutputStream streamPrompt = processConsole.newOutputStream();
        final IOConsoleOutputStream stream = processConsole.newOutputStream();
        this.promptOverlayActionHandlers = new PromptOverlayReplaceGlobalActionHandlers(consolePage, viewer);

        IActionBars bars = consolePage.getSite().getActionBars();
        IToolBarManager toolbarManager = bars.getToolBarManager();

        ShowPromptOverlayAction showPromptOverlayAction = new ShowPromptOverlayAction(this);
        toolbarManager.prependToGroup(IConsoleConstants.LAUNCH_GROUP, showPromptOverlayAction);
        bars.updateActionBars();

        debugConsole.addListener(new IScriptConsoleListener() {

            @Override
            public void userRequest(String text, ScriptConsolePrompt prompt) {
                try {
                    if (!bufferedOutput) {
                        streamPrompt.setColor(ColorManager.getDefault().getPreferenceColor(
                                PydevConsoleConstants.CONSOLE_PROMPT_COLOR));

                        stream.setColor(ColorManager.getDefault().getPreferenceColor(
                                PydevConsoleConstants.CONSOLE_INPUT_COLOR));

                        IDocument document = processConsole.getDocument();
                        IDocumentPartitioner partitioner = document.getDocumentPartitioner();
                        IOConsolePartitioner ioConsolePartitioner = (IOConsolePartitioner) partitioner;

                        ioConsolePartitioner.streamAppended(streamPrompt, prompt.toString());
                        ioConsolePartitioner.streamAppended(stream, text + "\n");
                    }
                } catch (IOException e) {
                    Log.log(e);
                }
            }

            @Override
            public void interpreterResponse(InterpreterResponse response, ScriptConsolePrompt prompt) {

            }
        });

        styledText.addDisposeListener(this);
        styledText.addListener(SWT.Hide, this);
        styledText.addListener(SWT.Show, this);
        styledText.addListener(SWT.Paint, this);
        styledText.addListener(SWT.Resize, this);
        styledText.addListener(SWT.Selection, this);
        adjust();
    }

    @Override
    public void contentAssistRequired() {
        if (this.currentPyStackFrameForConsole.getLastSelectedFrame() == null) {
            return;
        }
        viewer.getContentAssist().showPossibleCompletions();
    }

    @Override
    public void quickAssistRequired() {
        viewer.getQuickAssistAssistant().showPossibleQuickAssists();
    }

    @Override
    public void widgetDisposed(DisposeEvent e) {
        dispose();
    }

    @Override
    public void handleEvent(Event event) {
        adjust();
    }

    private void adjust() {
        if (styledTextParent == null || styledTextParent.isDisposed()) {
            return;
        }
        if (overlayVisible && styledText != null && !styledText.isDisposed() && styledText.isVisible()) {
            if (styledTextParent.getLayout() != customLayout) {
                styledTextParent.setLayout(customLayout);
                styledTextParent.layout(true);
            }
            if (!interactiveConsoleTextWidget.isVisible()) {
                interactiveConsoleTextWidget.setVisible(true);
            }
            if (!interactiveConsoleTextWidget.getBackground().equals(styledText.getBackground())) {
                interactiveConsoleTextWidget.setBackground(styledText.getBackground());
            }
            if (!interactiveConsoleTextWidget.getForeground().equals(styledText.getForeground())) {
                interactiveConsoleTextWidget.setForeground(styledText.getForeground());
            }
            if (!interactiveConsoleTextWidget.getFont().equals(styledText.getFont())) {
                interactiveConsoleTextWidget.setFont(styledText.getFont());
            }
        } else {
            if (interactiveConsoleTextWidget.isVisible()) {
                interactiveConsoleTextWidget.setVisible(false);
            }
            if (styledTextParent.getLayout() != this.customLayout.originalParentLayout) {
                styledTextParent.setLayout(this.customLayout.originalParentLayout);
                styledTextParent.layout(true);
            }
        }
    }

    private class CustomPageBookLayout extends Layout {

        public final Layout originalParentLayout;

        public CustomPageBookLayout(Layout originalParentLayout) {
            if (originalParentLayout instanceof CustomPageBookLayout) {
                //It's there by some other view of ours (switched directly between them).
                CustomPageBookLayout customPageBookLayout = (CustomPageBookLayout) originalParentLayout;
                this.originalParentLayout = customPageBookLayout.originalParentLayout;
            } else {
                this.originalParentLayout = originalParentLayout;
            }
        }

        @Override
        protected Point computeSize(Composite composite, int wHint, int hHint,
                boolean flushCache) {
            if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
                return new Point(wHint, hHint);
            }

            Point result = null;
            if (styledText != null) {
                result = styledText.computeSize(wHint, hHint, flushCache);
            } else {
                result = new Point(0, 0);
            }
            if (wHint != SWT.DEFAULT) {
                result.x = wHint;
            }
            if (hHint != SWT.DEFAULT) {
                result.y = hHint;
            }
            return result;
        }

        @Override
        protected void layout(Composite composite, boolean flushCache) {
            if (styledText != null && !styledText.isDisposed()) {
                Rectangle bounds = composite.getClientArea();

                int height = bounds.height;
                int perc = (int) (height * percSize); // 30% to the input

                interactiveConsoleTextWidget.setBounds(bounds.x, bounds.y + height - perc, bounds.width,
                        perc);
                styledText.setBounds(bounds.x, bounds.y, bounds.width, height - perc);
            }
        }
    }

    public void dispose() {
        try {
            styledText = null;
            if (interactiveConsoleTextWidget != null) {
                interactiveConsoleTextWidget.setVisible(false);
                interactiveConsoleTextWidget.dispose();
                interactiveConsoleTextWidget = null;
            }
        } catch (Exception e1) {
            Log.log(e1);
        }
        try {
            if (styledTextParent != null) {
                if (!styledTextParent.isDisposed()) {
                    if (styledTextParent.getLayout() == customLayout) {
                        styledTextParent.setLayout(this.customLayout.originalParentLayout);
                    }
                }
                styledTextParent = null;
            }
        } catch (Exception e1) {
            Log.log(e1);
        }
        try {
            if (promptOverlayActionHandlers != null) {
                promptOverlayActionHandlers.dispose();
            }
            promptOverlayActionHandlers = null;
        } catch (Exception e1) {
            Log.log(e1);
        }
    }

    public void setOverlayVisible(boolean visible) {
        if (this.overlayVisible != visible) {
            this.overlayVisible = visible;
            adjustAndLayout();
        }
    }

    /**
     * Returns a number from 0 - 100.
     */
    public int getRelativeConsoleHeight() {
        return (int) (this.percSize * 100);
    }

    public void setRelativeConsoleHeight(int relSize0To100) {
        double newVal = relSize0To100 / 100.;
        if (newVal != this.percSize) {
            this.percSize = newVal;
            adjustAndLayout();
        }
    }

    private void adjustAndLayout() {
        adjust();
        if (styledTextParent != null && !styledTextParent.isDisposed()) {
            styledTextParent.layout(true);
        }
    }

    public void activated() {
        //I.e.: Console view gets focus
    }

    public void deactivated() {
        //I.e.: Console view looses focus
    }

    public void setBufferedOutput(boolean bufferedOutput) {
        if (this.bufferedOutput != bufferedOutput) {
            this.bufferedOutput = bufferedOutput;
            IScriptConsoleCommunication consoleCommunication = debugConsole.getInterpreter().getConsoleCommunication();
            if (consoleCommunication instanceof PydevDebugConsoleCommunication) {
                PydevDebugConsoleCommunication pydevDebugConsoleCommunication = (PydevDebugConsoleCommunication) consoleCommunication;
                pydevDebugConsoleCommunication.setBufferedOutput(bufferedOutput);
            }
        }
    }

}