/******************************************************************************
* Copyright (C) 2015 Brainwy Software Ltda
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.debug.console;

import java.io.IOException;
import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.core.PydevDebugPreferencesInitializer;
import org.python.pydev.shared_ui.utils.UIUtils;

public class SetBufferedOutputAction extends Action implements IPropertyChangeListener {

    private WeakReference<PromptOverlay> promptOverlay;
    private IPreferenceStore preferences;

    public SetBufferedOutputAction(WeakReference<PromptOverlay> promptOverlay) {
        this.promptOverlay = promptOverlay;
        this.setText("Output Mode: Async main console");
        preferences = PydevDebugPlugin.getDefault().getPreferenceStore();
        preferences.addPropertyChangeListener(this);
        this.update();
    }

    private void update() {
        PromptOverlay overlay = promptOverlay.get();
        if (overlay == null) {
            return;
        }
        int val = preferences.getInt(PydevDebugPreferencesInitializer.CONSOLE_PROMPT_OUTPUT_MODE);
        if (val == PydevDebugPreferencesInitializer.MODE_ASYNC_SEPARATE_CONSOLE) {
            this.setText("Output Mode: Async main console");
            overlay.setBufferedOutput(false);
        } else {
            this.setText("Output Mode: Sync same console");
            overlay.setBufferedOutput(true);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (PydevDebugPreferencesInitializer.CONSOLE_PROMPT_OUTPUT_MODE.equals(event.getProperty())) {
            this.update();
        }
    }

    @Override
    public void run() {
        PromptOverlay overlay = promptOverlay.get();
        if (overlay == null || preferences == null) {
            return;
        }
        int curr = preferences.getInt(PydevDebugPreferencesInitializer.CONSOLE_PROMPT_OUTPUT_MODE);

        int retVal = new MessageDialog(
                UIUtils.getActiveShell(),
                "Mode for command output",
                null,
                "Please choose the mode for the command output",
                MessageDialog.QUESTION,
                new String[] { "Output Asynchronous in main console view",
                        "Output synchronous in console prompt view" },
                curr == PydevDebugPreferencesInitializer.MODE_ASYNC_SEPARATE_CONSOLE ? 0 : 1).open();

        if (retVal == 0) {
            //button 1
            preferences.setValue(PydevDebugPreferencesInitializer.CONSOLE_PROMPT_OUTPUT_MODE,
                    PydevDebugPreferencesInitializer.MODE_ASYNC_SEPARATE_CONSOLE);
            savePrefs();

        } else if (retVal == 1) {
            //button 2
            preferences.setValue(PydevDebugPreferencesInitializer.CONSOLE_PROMPT_OUTPUT_MODE,
                    PydevDebugPreferencesInitializer.MODE_NOT_ASYNC_SAME_CONSOLE);
            savePrefs();
        }

    }

    private void savePrefs() {
        if (preferences instanceof IPersistentPreferenceStore) {
            try {
                ((IPersistentPreferenceStore) preferences).save();
            } catch (IOException e) {
                Log.log(e);
            }
        }

    }

    public void dispose() {
        if (preferences != null) {
            preferences.removePropertyChangeListener(this);
        }
        preferences = null;
        this.setEnabled(false);
    }

}
