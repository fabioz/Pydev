/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.importsconf;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.editor.preferences.PyScopedPreferences;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.shared_ui.field_editors.BooleanFieldEditorCustom;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.RadioGroupFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

/**
 * Preferences regarding the way that imports should be managed:
 * 
 * - Grouped when possible?
 * - Can use multilines?
 * - Multilines with escape char or with '('
 *
 * @author Fabio
 */
public class ImportsPreferencesPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private BooleanFieldEditor fromImportsFirstBooleanEditor;
    private BooleanFieldEditorCustom pep8ImportCompliantFieldEditor;

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

    public static final String PEP8_IMPORTS = "PEP8_IMPORTS";
    public final static boolean DEFAULT_PEP8_IMPORTS = true;

    public static final String DELETE_UNUSED_IMPORTS = "DELETE_UNUSED_IMPORTS";
    //Left default as false because it can be a destructive operation (i.e.: many imports
    //may have a reason even without being used -- and in this case it must be marked as @UnusedImport,
    //so, making it so that the user has to enable this option and know what he is doing).
    public final static boolean DEFAULT_DELETE_UNUSED_IMPORTS = false;

    public static final String FROM_IMPORTS_FIRST = "FROM_IMPORTS_FIRST";
    public final static boolean DEFAULT_FROM_IMPORTS_FIRST = false;

    public static final String SORT_NAMES_GROUPED = "SORT_NAMES_GROUPED";
    public final static boolean DEFAULT_SORT_NAMES_GROUPED = false;

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new LabelFieldEditor("Label_Info_File_Preferences1", WrapAndCaseUtils.wrap(
                "These setting are used whenever imports are managed in the application\n\n", 80), p));

        pep8ImportCompliantFieldEditor = new BooleanFieldEditorCustom(PEP8_IMPORTS, WrapAndCaseUtils.wrap(
                "Use Pep8 compliant import organzier?", 80), p);
        addFieldWithToolTip(pep8ImportCompliantFieldEditor, p,
                "System modules are those found on the interpreter's Python path;"
                        + " third party modules are found in site-packages.");

        addFieldWithToolTip(
                new BooleanFieldEditor(DELETE_UNUSED_IMPORTS, WrapAndCaseUtils.wrap(
                        "Delete unused imports?", 80), p),
                p,
                "Simple unused imports as reported by the code analysis are deleted. This can be configured to ignore certain files, and individual warnings can be surpressed.");

        addField(new BooleanFieldEditor(GROUP_IMPORTS, "Combine 'from' imports when possible?", p));

        fromImportsFirstBooleanEditor = new BooleanFieldEditor(FROM_IMPORTS_FIRST,
                "Sort 'from' imports before 'import' imports?", p);
        addField(fromImportsFirstBooleanEditor);

        addField(new BooleanFieldEditor(MULTILINE_IMPORTS, WrapAndCaseUtils.wrap(
                "Allow multiline imports when the import size would exceed the print margin?", 80), p));

        addField(new BooleanFieldEditor(SORT_NAMES_GROUPED, WrapAndCaseUtils.wrap(
                "Sort individual names on grouped imports?", 80), p));

        addField(new RadioGroupFieldEditor(BREAK_IMPORTS_MODE, "How to break imports in multiline?", 1,
                new String[][] { { "Use escape char", BREAK_IMPORTS_MODE_ESCAPE },
                        { "Use parenthesis", BREAK_IMPORTS_MODE_PARENTHESIS } }, p));

        updateEnablement(p, PydevPrefs.getPreferences().getBoolean(PEP8_IMPORTS));
        Button checkBox = pep8ImportCompliantFieldEditor.getCheckBox(p);
        checkBox.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateEnablement(p, pep8ImportCompliantFieldEditor.getBooleanValue());
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        addField(new LinkFieldEditor("link_saveactions",
                "\nNote: view <a>save actions</a> to automatically sort imports on save.", p,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.python.pydev.editor.saveactions.PydevSaveActionsPrefPage";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                }));

        addField(new ScopedPreferencesFieldEditor(p, PydevPlugin.DEFAULT_PYDEV_SCOPE, this));
    }

    private void updateEnablement(Composite p, boolean enable) {
        fromImportsFirstBooleanEditor.setEnabled(enable, p);
    }

    private void addFieldWithToolTip(BooleanFieldEditor editor, Composite p, String tip) {
        addField(editor);
        editor.getDescriptionControl(p).setToolTipText(tip);
    }

    public void init(IWorkbench workbench) {
        // pass
    }

    /**
     * @return true if imports should be grouped when possible. E.g.: If from aaa import b and from aaa import c
     * exist, they should be grouped as from aaa import b, c
     */
    public static boolean getGroupImports(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return groupImportsForTests;
        }
        return PyScopedPreferences.getBoolean(GROUP_IMPORTS, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean groupImportsForTests = true;

    /**
     * @return true if 'from ... import ...' statements should be sorted before 'import ...' statements.
     * E.g, a set of imports would be organized like the following:
     *   from a_module import b, c, d
     *   from c_module import e, f
     *   import b_module
     *   import d_module   
     */
    public static boolean getSortFromImportsFirst(IAdaptable projectAdaptable) {
        if (PydevPlugin.getDefault() == null) {
            return sortFromImportsFirstForTests;
        }
        return PyScopedPreferences.getBoolean(FROM_IMPORTS_FIRST, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean sortFromImportsFirstForTests = true;

    /**
     * @return true if imports should be wrapped when they exceed the print margin.
     */
    public static boolean getMultilineImports(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return multilineImportsForTests;
        }
        return PyScopedPreferences.getBoolean(MULTILINE_IMPORTS, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean multilineImportsForTests = true;

    public static boolean getSortNamesGrouped(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return sortNamesGroupedForTests;
        }
        return PyScopedPreferences.getBoolean(SORT_NAMES_GROUPED, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean sortNamesGroupedForTests = false;

    /**
     * @return the way to break imports as the constants specified
     * @see #BREAK_IMPORTS_MODE_ESCAPE
     * @see #BREAK_IMPORTS_MODE_PARENTHESIS
     */
    public static String getBreakIportMode(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return breakImportModeForTests;
        }
        return PyScopedPreferences.getString(BREAK_IMPORTS_MODE, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static String breakImportModeForTests = BREAK_IMPORTS_MODE_PARENTHESIS;

    /**
     * @return whether to format imports according to pep8
     */
    public static boolean getPep8Imports(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return pep8ImportsForTests;
        }
        return PyScopedPreferences.getBoolean(PEP8_IMPORTS, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean pep8ImportsForTests = true;

    /**
     * @return whether to delete unused imports
     */
    public static boolean getDeleteUnusedImports(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            return deleteUnusedImportsForTests;
        }
        return PyScopedPreferences.getBoolean(DELETE_UNUSED_IMPORTS, projectAdaptable);
    }

    /**
     * May be changed for testing purposes.
     */
    public static boolean deleteUnusedImportsForTests = true;

}
