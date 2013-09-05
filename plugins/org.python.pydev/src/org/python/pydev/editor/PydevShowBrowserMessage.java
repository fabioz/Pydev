/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Ecliplse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.editor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.python.pydev.core.docutils.WrapAndCaseUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.UIConstants;

final class DialogNotifier extends Dialog {

    private static final int BOLD_COLS = 120;

    public DialogNotifier(Shell shell) {
        super(shell);
        setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX);
        setBlockOnOpen(false);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(800, 600);
    }

    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        GridData gridData = null;
        GridLayout layout = (GridLayout) composite.getLayout();
        layout.numColumns = 1;

        String msg = "Help keeping PyDev alive";
        createLabel(composite, WrapAndCaseUtils.wrap(msg, BOLD_COLS), 1);

        try {
            final String html = "<html><head>"
                    +
                    "<base href=\"http://pydev.org\" >"
                    +
                    "<title>Keeping PyDev alive</title></head>"
                    +
                    "<body>"
                    +
                    "I'm reaching out for you today to ask for your help to keep PyDev properly supported, as well as improving some aspects of Eclipse itself (especially for those that like to work with a Dark theme)."
                    +
                    "<br/>"
                    +
                    "<br/>"
                    +
                    "A campaign was created at Indiegogo (<a href=\"http://igg.me/at/liclipse\">http://igg.me/at/liclipse</a>) for this purpose, and I'd really appreciate if you can take some time to take a look at it and share it (and if possible contribute) if you feel that those are worthy goals.<br/><br/>"
                    +
                    "Without your help, it's possible that PyDev may become unsupported!"
                    +
                    "<br/>"
                    +
                    "<br/>"
                    +
                    ""
                    +
                    "Thanks,"
                    +
                    "<br/>"
                    +
                    "<br/>"
                    +
                    "Fabio"
                    +
                    "<br/>"
                    +
                    "<br/>"
                    +
                    "p.s.: Sorry for the dialog. It won't be shown again in this workspace after you click the \"Read it\" button."
                    +

                    "</body></html>";
            ToolBar navBar = new ToolBar(composite, SWT.NONE);
            //this is the place where it might fail
            final Browser browser = new Browser(composite, SWT.BORDER);
            browser.setText(html);
            gridData = new GridData(GridData.FILL_BOTH);
            browser.setLayoutData(gridData);

            final ToolItem back = new ToolItem(navBar, SWT.PUSH);
            back.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.BACK));

            final ToolItem forward = new ToolItem(navBar, SWT.PUSH);
            forward.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.FORWARD));

            final ToolItem stop = new ToolItem(navBar, SWT.PUSH);
            stop.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.STOP));

            final ToolItem refresh = new ToolItem(navBar, SWT.PUSH);
            refresh.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.REFRESH));

            final ToolItem home = new ToolItem(navBar, SWT.PUSH);
            home.setImage(org.python.pydev.plugin.PydevPlugin.getImageCache().get(UIConstants.HOME));

            back.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    browser.back();
                }
            });
            forward.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    browser.forward();
                }
            });
            stop.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    browser.stop();
                }
            });
            refresh.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    browser.refresh();
                }
            });
            home.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    browser.setText(html);
                }
            });

        } catch (Throwable e) {
            //some error might happen creating it according to the docs, so, let's put another text into the widget
            String msg2 = "I'm reaching out for you today to ask for your help to keep " +
                    "PyDev properly supported, as well as improving some aspects \n" +
                    "of Eclipse itself (especially for those that like to work " +
                    "with a Dark theme).\n" +
                    "\n" +
                    "\n" +
                    "A campaign was created at Indiegogo (http://igg.me/at/liclipse) " +
                    "for this purpose, and I'd really appreciate if you can take \n" +
                    "some time to take a look at it and share it (and if possible " +
                    "contribute) if you feel that those are worthy goals.\n" +
                    "\n" +
                    "\n" +
                    "Without your help, it's possible that PyDev may become unsupported!\n" +
                    "\n" +
                    "\n" +
                    "Thanks,\n" +
                    "\n" +
                    "\n" +
                    "Fabio\n" +
                    "\n" +
                    "\n" +
                    "p.s.: Sorry for the dialog. It won't be shown again in this " +
                    "workspace after you click the \"Read it\" button.\n" +
                    "";
            createText(composite, msg2, 1);
        }

        return composite;
    }

    public boolean doClose() {
        return super.close();
    }

    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        Button button = createButton(parent, IDialogConstants.OK_ID, " Show later ", true);
        button.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                doClose();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });

        button = createButton(parent, IDialogConstants.CLIENT_ID, " Read it ", true);
        button.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                doClose();
                IPreferenceStore preferenceStore = PydevPrefs.getPreferenceStore();
                preferenceStore.setValue(PydevShowBrowserMessage.PYDEV_FUNDING_SHOWN, true);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });
    }

    /**
     * @param composite
     * @param labelMsg 
     * @return 
     */
    private Text createText(Composite composite, String labelMsg, int colSpan) {
        Text text = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = colSpan;
        text.setLayoutData(gridData);
        text.setText(labelMsg);
        return text;
    }

    /**
     * @param composite
     * @param labelMsg 
     * @return 
     */
    private Label createLabel(Composite composite, String labelMsg, int colSpan) {
        Label label = new Label(composite, SWT.NONE);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = colSpan;
        label.setLayoutData(gridData);
        label.setText(labelMsg);
        return label;
    }

}

public class PydevShowBrowserMessage {

    public static final String PYDEV_FUNDING_SHOWN = "PYDEV_FUNDING_SHOWN";
    private static boolean shownInSession = false;

    public static void show() {
        if (shownInSession) {
            return;
        }
        shownInSession = true;
        PydevPlugin plugin = PydevPlugin.getDefault();
        if (plugin == null) {
            return;
        }
        IPreferenceStore preferenceStore = PydevPrefs.getPreferenceStore();
        boolean shownOnce = preferenceStore.getBoolean(PYDEV_FUNDING_SHOWN);
        if (!shownOnce) {
            final Display disp = Display.getDefault();
            disp.asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchWindow window = PydevPlugin.getDefault().getWorkbench()
                            .getActiveWorkbenchWindow();
                    Shell shell = (window == null) ? new Shell(disp) : window.getShell();
                    DialogNotifier notifier = new DialogNotifier(shell);
                    notifier.open();
                }
            });
        }

    }

}
