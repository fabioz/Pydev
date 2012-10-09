/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console.env;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.aptana.js.interactive_console.console.prefs.InteractiveConsolePrefs;

/**
 * Helper to choose which kind of Rhino run will it be.
 */
final class ChooseProcessTypeDialog extends Dialog {

    private Button checkboxRhinoEclipse;

    private Link link;

    ChooseProcessTypeDialog(Shell shell) {
        super(shell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        checkboxRhinoEclipse = new Button(area, SWT.RADIO);
        checkboxRhinoEclipse
                .setToolTipText("Creates a Rhino console using the running Eclipse environment (can potentially halt Eclipse depending on what's done).");
        configureButton(checkboxRhinoEclipse, "Rhino using VM running Eclipse");

        link = new Link(area, SWT.LEFT | SWT.WRAP);
        link.setText("<a>Configure interactive console preferences.</a>\n"
                + "I.e.: send contents to console on creation,\n"
                + "connect to variables view, initial commands, etc.");

        link.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil
                        .createPreferenceDialogOn(null,
                                InteractiveConsolePrefs.PREFERENCES_ID, null,
                                null);
                dialog.open();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        return area;
    }

    /**
     * Configures a button related to a given interpreter manager.
     */
    private void configureButton(Button checkBox, String python) {
        boolean enabled = true;
        String text = python + " console";
        checkBox.setText(text);
        checkBox.setEnabled(enabled);
    }

    /**
     * Sets the internal pythonpath chosen.
     */
    @Override
    protected void okPressed() {
        if (checkboxRhinoEclipse.isEnabled()
                && checkboxRhinoEclipse.getSelection()) {
        }
        super.okPressed();
    }

}