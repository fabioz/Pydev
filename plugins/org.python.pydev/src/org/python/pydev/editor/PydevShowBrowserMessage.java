/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
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
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.RunInUiThread;

final class DialogNotifier extends Dialog {

    private static final int BOLD_COLS = 120;

    public DialogNotifier(Shell shell) {
        super(shell);
        setShellStyle(
                SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE | SWT.MAX | getDefaultOrientation());
        setBlockOnOpen(true);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Point getInitialSize() {
        return new Point(800, 600);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        GridData gridData = null;
        GridLayout layout = (GridLayout) composite.getLayout();
        layout.numColumns = 1;

        String msg = "Help keeping PyDev supported";
        createLabel(composite, WrapAndCaseUtils.wrap(msg, BOLD_COLS), 1);

        try {
            final String html = "<html><head>"
                    +
                    "<base href=\"http://pydev.org\" >"
                    +
                    "<title>Keeping PyDev supported</title></head>"
                    +
                    "<body>"
                    +
                    "I'm reaching out for you today to ask for your help to keep PyDev properly supported."
                    +
                    "<br/>"
                    +
                    "<br/>"
                    +
                    "PyDev is kept as an open source product and relies on contributions to remain being developed, so, if you feel that's a worthy goal, please take a look at <a href=\"http://pydev.org\">http://pydev.org</a> and contribute if you can.<br/><br/>"
                    +
                    ""
                    +
                    "Thank you,"
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
                @Override
                public void handleEvent(Event event) {
                    browser.back();
                }
            });
            forward.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    browser.forward();
                }
            });
            stop.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    browser.stop();
                }
            });
            refresh.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    browser.refresh();
                }
            });
            home.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    browser.setText(html);
                }
            });

        } catch (Throwable e) {
            //some error might happen creating it according to the docs, so, let's put another text into the widget
            String msg2 = "I'm reaching out for you today to ask for your help to keep PyDev\n"
                    + "properly supported.\n"
                    +
                    "\n"
                    +
                    "PyDev is kept as an open source product and relies on contributions\n"
                    + "to remain being developed, so, if you feel that's a worthy goal, please\n"
                    + "take a look at http://pydev.org and contribute if you can.\n"
                    +
                    "\n"
                    +
                    "Thank you,\n"
                    +
                    "\n"
                    +
                    "Fabio\n"
                    +
                    "\n"
                    +
                    "p.s.: Sorry for the dialog. It won't be shown again in this workspace after\n"
                    + "you click the \"Read it\" button.\n"
                    +
                    "";
            createText(composite, msg2, 1);
        }

        return composite;
    }

    public boolean doClose() {
        return super.close();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        Button button = createButton(parent, IDialogConstants.OK_ID, " Show later ", true);
        button.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                doClose();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });

        button = createButton(parent, IDialogConstants.CLIENT_ID, " Read it ", true);
        button.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                doClose();
                IPreferenceStore preferenceStore = PydevPrefs.getPreferenceStore();
                //Show it again only after a full year has elapsed.
                preferenceStore.setValue(PydevShowBrowserMessage.PYDEV_FUNDING_SHOW_AT_TIME,
                        System.currentTimeMillis() + (PydevShowBrowserMessage.ONE_DAY_IN_MILLIS * 365));
            }

            @Override
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

    public static final String PYDEV_FUNDING_SHOW_AT_TIME = "PYDEV_FUNDING_SHOW_AT_TIME_2016";
    private static boolean shownInSession = false;
    public static final long ONE_DAY_IN_MILLIS = 86400000;

    public static void show() {
        if (shownInSession) {
            return;
        }
        shownInSession = true;
        if (SharedCorePlugin.inTestMode()) {
            return;
        }
        String hide = System.getProperty("pydev.funding.hide");
        if (hide != null && (hide.equals("1") || hide.equals("true"))) {
            return;
        }
        IPreferenceStore preferenceStore = PydevPrefs.getPreferenceStore();
        long showAtTime = preferenceStore.getLong(PYDEV_FUNDING_SHOW_AT_TIME);
        boolean show;
        if (showAtTime == 0) {
            // It was never shown, so, show it after 3 days from now (we don't want to show
            // the dialog as the first thing after the user installed it).
            preferenceStore.setValue(PYDEV_FUNDING_SHOW_AT_TIME,
                    System.currentTimeMillis() + (ONE_DAY_IN_MILLIS * 3));
            show = false;
        } else if (System.currentTimeMillis() < showAtTime) {
            // We still didn't reach the time for it to show.
            show = false;
        } else {
            show = true;
        }
        if (show) {
            boolean runNowIfInUiThread = false;
            RunInUiThread.async(new Runnable() {

                @Override
                public void run() {
                    Display disp = Display.getCurrent();
                    Shell shell = disp.getActiveShell();
                    if (shell == null) {
                        shell = new Shell(disp);
                    }
                    DialogNotifier notifier = new DialogNotifier(shell);
                    notifier.open();
                }
            }, runNowIfInUiThread);
        }

    }
}
