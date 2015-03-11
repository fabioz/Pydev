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
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.core.PydevDebugPreferencesInitializer;

public class SetFullLayoutAction extends Action {

    private WeakReference<PromptOverlay> promptOverlay;
    private IPreferenceStore preferences;

    private int previousConsoleHeight = 30;

    public SetFullLayoutAction(WeakReference<PromptOverlay> promptOverlay) {
        this.promptOverlay = promptOverlay;
        preferences = PydevDebugPlugin.getDefault().getPreferenceStore();
        this.updateText();
    }

    private void updateText() {
        PromptOverlay overlay = promptOverlay.get();
        if (overlay == null) {
            return;
        }
        int relativeConsoleHeight = overlay.getRelativeConsoleHeight();
        if (relativeConsoleHeight < 100) {
            this.setText("Hide original console");
        } else {
            this.setText("Show original console");
        }

    }

    @Override
    public void run() {
        PromptOverlay overlay = promptOverlay.get();
        if (overlay == null || preferences == null) {
            return;
        }
        int relativeConsoleHeight = overlay.getRelativeConsoleHeight();
        int newSize;
        if (relativeConsoleHeight < 100) {
            previousConsoleHeight = relativeConsoleHeight;
            newSize = 100;
            preferences.setValue(PydevDebugPreferencesInitializer.CONSOLE_PROMPT_OUTPUT_MODE,
                    PydevDebugPreferencesInitializer.MODE_NOT_ASYNC_SAME_CONSOLE);
        } else {
            newSize = previousConsoleHeight;
            preferences.setValue(PydevDebugPreferencesInitializer.CONSOLE_PROMPT_OUTPUT_MODE,
                    PydevDebugPreferencesInitializer.MODE_ASYNC_SEPARATE_CONSOLE);
        }
        preferences.setValue(PydevDebugPreferencesInitializer.RELATIVE_CONSOLE_HEIGHT, newSize);
        if (preferences instanceof IPersistentPreferenceStore) {
            try {
                ((IPersistentPreferenceStore) preferences).save();
            } catch (IOException e) {
                Log.log(e);
            }
        }
        updateText();
    }

    public void dispose() {
        preferences = null;
        this.setEnabled(false);
    }

}
