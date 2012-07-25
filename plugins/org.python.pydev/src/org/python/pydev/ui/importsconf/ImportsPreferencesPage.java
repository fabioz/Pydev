/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.importsconf;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.docutils.WrapAndCaseUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.utils.LabelFieldEditor;

/**
 * Preferences regarding the way that imports should be managed:
 * 
 * - Grouped when possible?
 * - Can use multilines?
 * - Multilines with escape char or with '('
 *
 * @author Fabio
 */
public class ImportsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public ImportsPreferencesPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Imports Preferences");
    }

    public static final String GROUP_IMPORTS = "GROUP_IMPORTS";
    public final static boolean DEFAULT_GROUP_IMPORTS = true;

    public static final String MULTILINE_IMPORTS = "MULTILINE_IMPORTS";
    public final static boolean DEFAULT_MULTILINE_IMPORTS = true;

    public static final String BREAK_IMPORTS_MODE = "BREAK_IMPORTS_MODE";
    public static final String BREAK_IMPORTS_MODE_ESCAPE = "ESCAPE";
    public static final String BREAK_IMPORTS_MODE_PARENTHESIS = "PARENTHESIS";
    public final static String DEFAULT_BREAK_IMPORTS_MODE = BREAK_IMPORTS_MODE_ESCAPE;

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new LabelFieldEditor("Label_Info_File_Preferences1", WrapAndCaseUtils.wrap(
                "These setting are used whenever imports are managed in the application\n\n", 80), p));

        addField(new BooleanFieldEditor(GROUP_IMPORTS, "Group 'from' imports when possible?", p));

        addField(new BooleanFieldEditor(MULTILINE_IMPORTS, WrapAndCaseUtils.wrap(
                "Allow multiline imports when the import size would exceed the print margin?", 80), p));

        addField(new RadioGroupFieldEditor(BREAK_IMPORTS_MODE, "How to break imports in multiline?", 1,
                new String[][] { { "Use escape char", BREAK_IMPORTS_MODE_ESCAPE },
                        { "Use parenthesis", BREAK_IMPORTS_MODE_PARENTHESIS } }, p));
    }

    public void init(IWorkbench workbench) {
        // pass
    }

    /**
     * @return true if imports should be grouped when possible. E.g.: If from aaa import b and from aaa import c
     * exist, they should be grouped as from aaa import b, c
     */
    public static boolean getGroupImports() {
        if (PydevPlugin.getDefault() == null) {
            return groupImportsForTests;
        }
        return PydevPrefs.getPreferences().getBoolean(GROUP_IMPORTS);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean groupImportsForTests = true;

    /**
     * @return true if imports should be wrapped when they exceed the print margin.
     */
    public static boolean getMultilineImports() {
        if (PydevPlugin.getDefault() == null) {
            return multilineImportsForTests;
        }
        return PydevPrefs.getPreferences().getBoolean(MULTILINE_IMPORTS);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean multilineImportsForTests = true;

    /**
     * @return the way to break imports as the constants specified
     * @see #BREAK_IMPORTS_MODE_ESCAPE
     * @see #BREAK_IMPORTS_MODE_PARENTHESIS
     */
    public static String getBreakIportMode() {
        if (PydevPlugin.getDefault() == null) {
            return breakImportModeForTests;
        }
        return PydevPrefs.getPreferences().getString(BREAK_IMPORTS_MODE);
    }

    /**
     * May be changed for testing purposes.
     */
    public static String breakImportModeForTests = BREAK_IMPORTS_MODE_PARENTHESIS;

}
