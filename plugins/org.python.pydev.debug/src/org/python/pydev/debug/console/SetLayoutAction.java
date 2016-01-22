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
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.core.PydevDebugPreferencesInitializer;
import org.python.pydev.shared_ui.dialogs.DialogHelpers;

public class SetLayoutAction extends Action implements IPropertyChangeListener {

    private WeakReference<PromptOverlay> promptOverlay;
    private IPreferenceStore preferences;

    public SetLayoutAction(WeakReference<PromptOverlay> promptOverlay) {
        this.promptOverlay = promptOverlay;
        this.setText("Set Console Height");
        preferences = PydevDebugPlugin.getDefault().getPreferenceStore();
        preferences.addPropertyChangeListener(this);
        this.update();
    }

    private void update() {
        PromptOverlay overlay = promptOverlay.get();
        if (overlay == null) {
            return;
        }
        overlay.setRelativeConsoleHeight(preferences.getInt(PydevDebugPreferencesInitializer.RELATIVE_CONSOLE_HEIGHT));
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (PydevDebugPreferencesInitializer.RELATIVE_CONSOLE_HEIGHT.equals(event.getProperty())) {
            this.update();
        }
    }

    @Override
    public void run() {
        PromptOverlay overlay = promptOverlay.get();
        if (overlay == null || preferences == null) {
            return;
        }
        Integer newSize = DialogHelpers.openAskInt("Percentual size for console prompt.",
                "Please enter the relative size for the console prompt (0-100)",
                preferences.getInt(PydevDebugPreferencesInitializer.RELATIVE_CONSOLE_HEIGHT));
        if (newSize != null) {
            if (newSize < 0) {
                newSize = 0;
            }
            if (newSize > 100) {
                newSize = 100;
            }
        }
        preferences.setValue(PydevDebugPreferencesInitializer.RELATIVE_CONSOLE_HEIGHT, newSize);
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
