/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under1 the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.prefs;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.debug.core.PydevDebugPlugin;

public class InteractiveConsoleCommandsPreferencesPage extends PreferencePage implements
        IWorkbenchPreferencePage {

    private InterativeConsoleCommandsPreferencesEditor editor;

    public InteractiveConsoleCommandsPreferencesPage() {
        editor = new InterativeConsoleCommandsPreferencesEditor();
    }

    public void init(IWorkbench workbench) {
        setDescription("PyDev interactive console custom commands.");
        setPreferenceStore(PydevDebugPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected Control createContents(Composite parent) {
        return editor.createContents(parent);
    }

    @Override
    protected void performApply() {
        editor.performSave();
    }

    @Override
    public boolean performOk() {
        editor.performSave();
        return true;
    }

    @Override
    protected void performDefaults() {
        editor.performDefaults();
    }

    @Override
    public boolean performCancel() {
        return super.performCancel();
    }

    @Override
    public void dispose() {
        super.dispose();
        editor.dispose();
    }
}
