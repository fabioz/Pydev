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
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.internal.console.IOConsolePartitioner;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.newconsole.CurrentPyStackFrameForConsole;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.PydevDebugConsole;
import org.python.pydev.debug.newconsole.prefs.ColorManager;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_interactive_console.console.ScriptConsolePrompt;
import org.python.pydev.shared_interactive_console.console.ui.IConsoleStyleProvider;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleListener;
import org.python.pydev.shared_interactive_console.console.ui.internal.IScriptConsoleContentHandler;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleViewer;

@SuppressWarnings("restriction")
public class PromptOverlay implements DisposeListener, Listener, IScriptConsoleContentHandler {

    private StyledText interactiveConsoleTextWidget;
    private StyledText styledText;
    private Layout originalParentLayout;
    private Composite styledTextParent;
    private CustomPageBookLayout customLayout;
    private final CurrentPyStackFrameForConsole currentPyStackFrameForConsole;
    private ScriptConsoleViewer viewer;
    private PromptOverlayReplaceGlobalActionHandlers promptOverlayActionHandlers;
    public static String PROMPT_OVERLAY_ATTRIBUTE_IN_CONSOLE = "PROMPT_OVERLAY_ATTRIBUTE_IN_CONSOLE";

    public PromptOverlay(IOConsolePage consolePage, final ProcessConsole processConsole,
            CurrentPyStackFrameForConsole currentPyStackFrameForConsole) {

        this.currentPyStackFrameForConsole = currentPyStackFrameForConsole;
        PydevDebugConsole console;
        SourceViewerConfiguration cfg;
        try {
            ILaunch launch = processConsole.getProcess().getLaunch();
            console = new PydevConsoleFactory().createDebugConsole(launch, "", false, false,
                    currentPyStackFrameForConsole);
            cfg = console.createSourceViewerConfiguration();
            processConsole.setAttribute(PydevDebugConsole.SCRIPT_DEBUG_CONSOLE_IN_PROCESS_CONSOLE, console);
        } catch (Exception e) {
            // If we can't create the debug console, bail out and do nothing else.
            Log.log(e);
            return;
        }

        TextConsoleViewer consoleViewer = consolePage.getViewer();
        final StyledText styledText = (StyledText) consoleViewer.getControl();
        this.styledText = styledText;
        styledTextParent = styledText.getParent();
        originalParentLayout = styledTextParent.getLayout();

        final IConsoleStyleProvider styleProvider = console.createStyleProvider();
        viewer = new ScriptConsoleViewer(styledTextParent, console, this, styleProvider,
                console.getInitialCommands(), console.getFocusOnStart(), console.getBackspaceAction(),
                console.getAutoEditStrategy(), console.getTabCompletionEnabled(), false);
        viewer.configure(cfg);

        this.customLayout = new CustomPageBookLayout();
        this.interactiveConsoleTextWidget = viewer.getTextWidget();

        final IOConsoleOutputStream streamPrompt = processConsole.newOutputStream();
        final IOConsoleOutputStream stream = processConsole.newOutputStream();
        this.promptOverlayActionHandlers = new PromptOverlayReplaceGlobalActionHandlers(consolePage, viewer);

        console.addListener(new IScriptConsoleListener() {

            @Override
            public void userRequest(String text, ScriptConsolePrompt prompt) {
                try {
                    streamPrompt.setColor(ColorManager.getDefault().getPreferenceColor(
                            PydevConsoleConstants.CONSOLE_PROMPT_COLOR));

                    stream.setColor(ColorManager.getDefault().getPreferenceColor(
                            PydevConsoleConstants.CONSOLE_INPUT_COLOR));

                    IDocument document = processConsole.getDocument();
                    IDocumentPartitioner partitioner = document.getDocumentPartitioner();
                    IOConsolePartitioner ioConsolePartitioner = (IOConsolePartitioner) partitioner;

                    ioConsolePartitioner.streamAppended(streamPrompt, prompt.toString());
                    ioConsolePartitioner.streamAppended(stream, text + "\n");
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
            if (!styledTextParent.isDisposed()) {
                if (styledTextParent.getLayout() == customLayout) {
                    styledTextParent.setLayout(originalParentLayout);
                }
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

    @Override
    public void handleEvent(Event event) {
        adjust();
    }

    private void adjust() {
        if (styledText != null && !styledText.isDisposed() && styledText.isVisible()) {
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
            if (styledTextParent.getLayout() != originalParentLayout) {
                styledTextParent.setLayout(originalParentLayout);
            }
            if (interactiveConsoleTextWidget.isVisible()) {
                interactiveConsoleTextWidget.setVisible(false);
            }
        }
    }

    private class CustomPageBookLayout extends Layout {

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
                int perc = (int) (height * .3); // 30% to the input

                interactiveConsoleTextWidget.setBounds(bounds.x, bounds.y + height - perc, bounds.width,
                        perc);
                styledText.setBounds(bounds.x, bounds.y, bounds.width, height - perc);
            }
        }
    }

}