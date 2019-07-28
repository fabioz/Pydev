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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.core.preferences.PyScopedPreferences;
import org.python.pydev.plugin.PyDevUiPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.shared_ui.field_editors.ComboFieldEditor;
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
    private ComboFieldEditor importEngineFieldEditor;
    private BooleanFieldEditor deleteUnusedImportsField;
    private BooleanFieldEditor groupImportsField;
    private BooleanFieldEditor multilineImportsField;
    private BooleanFieldEditor sortIndiviualOnGroupedField;
    private RadioGroupFieldEditor breakImportsInMultilineMode;

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

    public static final String IMPORT_ENGINE = "IMPORT_ENGINE";
    public static final String IMPORT_ENGINE_REGULAR_SORT = "IMPORT_ENGINE_REGULAR_SORT";
    public static final String IMPORT_ENGINE_PEP_8 = "IMPORT_ENGINE_PEP_8";
    public static final String IMPORT_ENGINE_ISORT = "IMPORT_ENGINE_ISORT";
    public final static String DEFAULT_IMPORT_ENGINE = IMPORT_ENGINE_PEP_8;

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

        importEngineFieldEditor = new ComboFieldEditor(IMPORT_ENGINE, "Select import sort engine to be used",
                new String[][] {
                        new String[] { "Pep 8", IMPORT_ENGINE_PEP_8 },
                        new String[] { "Regular sort", IMPORT_ENGINE_REGULAR_SORT },
                        new String[] { "isort", IMPORT_ENGINE_ISORT },
                }, p);
        addFieldWithToolTip(importEngineFieldEditor, p,
                "Select which import engine should be used to sort the imports when such an operation is requested.");

        deleteUnusedImportsField = new BooleanFieldEditor(DELETE_UNUSED_IMPORTS, WrapAndCaseUtils.wrap(
                "Delete unused imports?", 80), p);
        addFieldWithToolTip(
                deleteUnusedImportsField,
                p,
                "Simple unused imports as reported by the code analysis are deleted. This can be configured to ignore certain files, and individual warnings can be surpressed.");

        groupImportsField = new BooleanFieldEditor(GROUP_IMPORTS, "Combine 'from' imports when possible?", p);
        addField(groupImportsField);

        fromImportsFirstBooleanEditor = new BooleanFieldEditor(FROM_IMPORTS_FIRST,
                "Sort 'from' imports before 'import' imports?", p);
        addField(fromImportsFirstBooleanEditor);

        multilineImportsField = new BooleanFieldEditor(MULTILINE_IMPORTS, WrapAndCaseUtils.wrap(
                "Allow multiline imports when the import size would exceed the print margin?", 80), p);
        addField(multilineImportsField);

        sortIndiviualOnGroupedField = new BooleanFieldEditor(SORT_NAMES_GROUPED, WrapAndCaseUtils.wrap(
                "Sort individual names on grouped imports?", 80), p);
        addField(sortIndiviualOnGroupedField);

        breakImportsInMultilineMode = new RadioGroupFieldEditor(BREAK_IMPORTS_MODE,
                "How to break imports in multiline?", 1,
                new String[][] { { "Use escape char", BREAK_IMPORTS_MODE_ESCAPE },
                        { "Use parenthesis", BREAK_IMPORTS_MODE_PARENTHESIS } },
                p);
        addField(breakImportsInMultilineMode);

        updateEnablement(p, PyDevUiPrefs.getPreferenceStore().getString(IMPORT_ENGINE));
        Combo importEngineCombo = importEngineFieldEditor.getCombo();
        importEngineCombo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateEnablement(p, importEngineFieldEditor.getComboValue());
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

        addField(new ScopedPreferencesFieldEditor(p, SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_QUALIFIER, this));
    }

    private void updateEnablement(Composite p, String importEngine) {
        switch (importEngine) {
            case IMPORT_ENGINE_PEP_8:
                fromImportsFirstBooleanEditor.setEnabled(true, p); // Setting only valid for PEP 8 engine.

                deleteUnusedImportsField.setEnabled(true, p);
                groupImportsField.setEnabled(true, p);
                multilineImportsField.setEnabled(true, p);
                sortIndiviualOnGroupedField.setEnabled(true, p);
                breakImportsInMultilineMode.setEnabled(true, p);
                break;

            case IMPORT_ENGINE_REGULAR_SORT:
                fromImportsFirstBooleanEditor.setEnabled(false, p);

                deleteUnusedImportsField.setEnabled(true, p);
                groupImportsField.setEnabled(true, p);
                multilineImportsField.setEnabled(true, p);
                sortIndiviualOnGroupedField.setEnabled(true, p);
                breakImportsInMultilineMode.setEnabled(true, p);
                break;

            case IMPORT_ENGINE_ISORT:
                fromImportsFirstBooleanEditor.setEnabled(false, p);
                deleteUnusedImportsField.setEnabled(false, p);
                groupImportsField.setEnabled(false, p);
                multilineImportsField.setEnabled(false, p);
                sortIndiviualOnGroupedField.setEnabled(false, p);
                breakImportsInMultilineMode.setEnabled(false, p);
                break;
        }
    }

    private void addFieldWithToolTip(BooleanFieldEditor editor, Composite p, String tip) {
        addField(editor);
        editor.getDescriptionControl(p).setToolTipText(tip);
    }

    private void addFieldWithToolTip(ComboFieldEditor editor, Composite p, String tip) {
        addField(editor);
        editor.getLabelControl(p).setToolTipText(tip);
    }

    @Override
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
    public static String getImportEngine(IAdaptable projectAdaptable) {
        if (SharedCorePlugin.inTestMode()) {
            if (pep8ImportsForTests) {
                return IMPORT_ENGINE_PEP_8;
            } else {
                return IMPORT_ENGINE_REGULAR_SORT;
            }
        }
        String importEngine = PyScopedPreferences.getString(IMPORT_ENGINE, projectAdaptable);
        if (importEngine == null) {
            importEngine = IMPORT_ENGINE_PEP_8;
        }
        switch (importEngine) {
            case IMPORT_ENGINE_PEP_8:
            case IMPORT_ENGINE_ISORT:
            case IMPORT_ENGINE_REGULAR_SORT:
                return importEngine;

            default:
                // Wrong value: use PEP 8 engine.
                return IMPORT_ENGINE_PEP_8;
        }
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
