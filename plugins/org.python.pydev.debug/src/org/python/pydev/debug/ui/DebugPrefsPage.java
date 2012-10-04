/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jun 23, 2003
 */
package org.python.pydev.debug.ui;

import java.util.List;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Debug preferences.
 * 
 * <p>Simple 1 page debug preferences page.
 * <p>Prefeernce constants are defined in Constants.java
 */
public class DebugPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Initializer sets the preference store
     */
    public DebugPrefsPage() {
        super("Debug", GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
    }

    /**
     * Creates the editors
     */
    @SuppressWarnings("unchecked")
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        addField(new IntegerFieldEditor(PydevEditorPrefs.CONNECT_TIMEOUT, "Connect timeout for debugger (ms)", p, 10));
        List<IDebugPreferencesPageParticipant> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_DEBUG_PREFERENCES_PAGE);
        for (IDebugPreferencesPageParticipant participant : participants) {
            participant.createFieldEditors(this, p);
        }
    }

    /**
     * Make it available for extensions
     */
    @Override
    public void addField(FieldEditor editor) {
        super.addField(editor);
    }

}
