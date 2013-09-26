/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.hover;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * Preferences page for showing or not hovering info.
 * 
 * @author Fabio
 */
public class PyHoverPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String SHOW_DOCSTRING_ON_HOVER = "SHOW_DOCSTRING_ON_HOVER";
    public static final boolean DEFAULT_SHOW_DOCSTRING_ON_HOVER = true;

    public static final String SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER = "SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER";
    public static final boolean DEFAULT_SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER = true;

    public PyHoverPreferencesPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Hover Preferences");
    }

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();
        addField(new BooleanFieldEditor(SHOW_DOCSTRING_ON_HOVER, "Show docstrings?", p));
        addField(new BooleanFieldEditor(SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER, "Show variables values while debugging?",
                p));
    }

    public void init(IWorkbench workbench) {
        // pass
    }

    /**
     * @return whether the docstring should be shown when hovering.
     */
    public static boolean getShowDocstringOnHover() {
        return PydevPrefs.getPreferences().getBoolean(SHOW_DOCSTRING_ON_HOVER);
    }

    /**
     * @return whether the value of variables should be shown on hover while debugging.
     */
    public static boolean getShowValuesWhileDebuggingOnHover() {
        return PydevPrefs.getPreferences().getBoolean(SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER);
    }
}
