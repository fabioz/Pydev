package org.python.pydev.debug.console;

import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
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
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.PydevDebugConsole;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_interactive_console.console.ScriptConsolePrompt;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleListener;
import org.python.pydev.shared_interactive_console.console.ui.internal.IScriptConsoleContentHandler;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleViewer;
import org.python.pydev.shared_ui.utils.RunInUiThread;

public class PromptOverlay implements DisposeListener, Listener, IScriptConsoleContentHandler {

    private StyledText interactiveConsoleTextWidget;
    private StyledText styledText;
    private Layout originalParentLayout;
    private Composite styledTextParent;
    private CustomPageBookLayout customLayout;
    public static String PROMPT_OVERLAY_ATTRIBUTE_IN_CONSOLE = "PROMPT_OVERLAY_ATTRIBUTE_IN_CONSOLE";

    public PromptOverlay(IOConsolePage consolePage, ProcessConsole processConsole) {
        PydevDebugConsole console;
        SourceViewerConfiguration cfg;
        try {
            console = new PydevConsoleFactory().createDebugConsole(null, "", false, false);
            cfg = console.createSourceViewerConfiguration();
            processConsole.setAttribute(PydevDebugConsole.SCRIPT_DEBUG_CONSOLE_IN_PROCESS_CONSOLE, console);
        } catch (Exception e) {
            // If we can't create the debug console, bail out and do nothing else.
            Log.log(e);
            return;
        }

        console.addListener(new IScriptConsoleListener() {

            @Override
            public void userRequest(String text, ScriptConsolePrompt prompt) {
                final FastStringBuffer session = new FastStringBuffer();
                session.append(prompt.toString());
                session.append(text);
                session.append('\n');
                boolean runNowIfInUiThread = true;
                RunInUiThread.async(new Runnable() {

                    @Override
                    public void run() {
                        if (!styledText.isDisposed()) {
                            styledText.append(session.toString());
                        }
                    }
                }, runNowIfInUiThread);
            }

            @Override
            public void interpreterResponse(InterpreterResponse response, ScriptConsolePrompt prompt) {

            }
        });
        TextConsoleViewer viewer = consolePage.getViewer();
        final StyledText styledText = (StyledText) viewer.getControl();
        this.styledText = styledText;
        styledTextParent = styledText.getParent();
        originalParentLayout = styledTextParent.getLayout();

        viewer = new ScriptConsoleViewer(styledTextParent, console, this, console.createStyleProvider(),
                console.getInitialCommands(), console.getFocusOnStart(), console.getBackspaceAction(),
                console.getAutoEditStrategy(), console.getTabCompletionEnabled());
        viewer.configure(cfg);

        this.customLayout = new CustomPageBookLayout();
        this.interactiveConsoleTextWidget = viewer.getTextWidget();

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
        // System.out.println("Content assist required");
    }

    @Override
    public void quickAssistRequired() {
        // System.out.println("Quick assist required");
        // styledText.append("print(sys)\n");
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

                interactiveConsoleTextWidget.setBounds(bounds.x, bounds.y + bounds.height - 150, bounds.width,
                        150);
                styledText.setBounds(bounds.x, bounds.y, bounds.width, bounds.height - 150);
            }
        }
    }

}