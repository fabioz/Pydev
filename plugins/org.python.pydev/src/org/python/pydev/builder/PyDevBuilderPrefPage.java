/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 1, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.parser.PyParserManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.field_editors.ComboFieldEditor;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;

/**
 * @author Fabio Zadrozny
 */
public class PyDevBuilderPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private final class BooleanFieldEditorWithPublicGetControl extends BooleanFieldEditor {
        private BooleanFieldEditorWithPublicGetControl(String name, String label, Composite parent) {
            super(name, label, parent);
        }

        @Override
        public Button getChangeControl(Composite parent) {
            Button checkBox = super.getChangeControl(parent);
            return checkBox;
        }
    }

    public static final boolean DEFAULT_USE_PYDEV_BUILDERS = true;
    public static final String USE_PYDEV_BUILDERS = "USE_PYDEV_BUILDERS";

    public static final boolean DEFAULT_USE_PYDEV_ONLY_ON_DOC_SAVE = false;
    public static final String USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE = PyParserManager.USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE;

    public static final int DEFAULT_PYDEV_ELAPSE_BEFORE_ANALYSIS = 3000;
    public static final String PYDEV_ELAPSE_BEFORE_ANALYSIS = PyParserManager.PYDEV_ELAPSE_BEFORE_ANALYSIS;

    public static final String ANALYZE_ONLY_ACTIVE_EDITOR = "ANALYZE_ONLY_ACTIVE_EDITOR_2"; //Changed to _2 because we changed this behavior and the default is now true!
    public static final boolean DEFAULT_ANALYZE_ONLY_ACTIVE_EDITOR = true;

    public static final String REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED = "REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED_2"; //Changed to _2
    public static final boolean DEFAULT_REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED = true;
    private Button onlyAnalyzeOpenCheckBox;
    private Button removeErrorsCheckBox;

    public static final String PYC_DELETE_HANDLING = "PYC_DELETE_HANDLING";

    public static final int PYC_ALWAYS_DELETE = 0;
    public static final int PYC_DELETE_WHEN_PY_IS_DELETED = 1;
    public static final int PYC_NEVER_DELETE = 2;

    public static final int DEFAULT_PYC_DELETE_HANDLING = PYC_ALWAYS_DELETE;

    private static final String[][] ENTRIES_AND_VALUES = new String[][] {
            { "Delete any orphaned .pyc file.", Integer.toString(PYC_ALWAYS_DELETE) },
            { "Only delete .pyc when .py delete is detected.", Integer.toString(PYC_DELETE_WHEN_PY_IS_DELETED) },
            { "Never delete .pyc files.", Integer.toString(PYC_NEVER_DELETE) }, };

    /**
     * @param style
     */
    public PyDevBuilderPrefPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("PyDev builders");
    }

    /**
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        String s = "WARNING: \n\n" +
                "PyDev builders are required for many features \n"
                + "provided by Pydev such as:\n" +
                "\n" +
                "- Code completion\n" +
                "- PyLint\n" +
                "- TODO tasks\n"
                + "\n" +
                "So, if you choose to disable it, note that the features \n"
                + "mentioned above may not work as expected or may even not \n"
                + "work at all (use at your own risk).\n";

        addField(new LabelFieldEditor("LabelFieldEditor", s, p));
        addField(new BooleanFieldEditor(USE_PYDEV_BUILDERS, "Use builders?", p));

        //Analysis only on save means that we'll not have parse notifications (so, things will be analyzed only on save)
        addField(new BooleanFieldEditor(PyParserManager.USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE,
                "Disable parser notifications?", p));
        addField(new IntegerFieldEditor(PyParserManager.PYDEV_ELAPSE_BEFORE_ANALYSIS,
                "Time to elapse before reparsing changed file (millis)", p));

        s = "If only open editors are analyzed, markers will only be added\n" +
                "to the opened PyDev editors.\n";
        addField(new LabelFieldEditor("ActiveBufferLabelFieldEditor", s, p));

        BooleanFieldEditorWithPublicGetControl onlyAnalyzeOpen = new BooleanFieldEditorWithPublicGetControl(
                ANALYZE_ONLY_ACTIVE_EDITOR, "Only analyze open editors?", p);
        addField(onlyAnalyzeOpen);

        BooleanFieldEditorWithPublicGetControl removeErrors = new BooleanFieldEditorWithPublicGetControl(
                REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED, "Remove errors when editor is closed?", p);
        addField(removeErrors);

        removeErrorsCheckBox = removeErrors.getChangeControl(p);

        onlyAnalyzeOpenCheckBox = onlyAnalyzeOpen.getChangeControl(p);
        onlyAnalyzeOpenCheckBox.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                updateCheckEnabledState();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        addField(new ComboFieldEditor(PYC_DELETE_HANDLING, "How to handle .pyc/$py.class deletion?",
                ENTRIES_AND_VALUES, p));

    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected void initialize() {
        super.initialize();
        updateCheckEnabledState();
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        updateCheckEnabledState();
    }

    /**
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    private void updateCheckEnabledState() {
        if (removeErrorsCheckBox != null && !removeErrorsCheckBox.isDisposed() && onlyAnalyzeOpenCheckBox != null
                && !onlyAnalyzeOpenCheckBox.isDisposed()) {
            removeErrorsCheckBox.setEnabled(onlyAnalyzeOpenCheckBox.getSelection());
        }
    }

    public static boolean usePydevBuilders() {
        return PydevPrefs.getPreferences().getBoolean(USE_PYDEV_BUILDERS);
    }

    public static boolean useAnalysisOnlyOnDocSave() {
        return PyParserManager.getPyParserManager(PydevPrefs.getPreferences()).useAnalysisOnlyOnDocSave();
    }

    public static boolean getAnalyzeOnlyActiveEditor() {
        return PydevPrefs.getPreferences().getBoolean(ANALYZE_ONLY_ACTIVE_EDITOR);
    }

    public static boolean getRemoveErrorsWhenEditorIsClosed() {
        return PydevPrefs.getPreferences().getBoolean(REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED);
    }

    public static void setAnalyzeOnlyActiveEditor(boolean b) {
        PydevPrefs.getPreferences().setValue(ANALYZE_ONLY_ACTIVE_EDITOR, b);
    }

    public static int getElapseMillisBeforeAnalysis() {
        return PyParserManager.getPyParserManager(PydevPrefs.getPreferences()).getElapseMillisBeforeAnalysis();
    }

    public static int getPycDeleteHandling() {
        return PydevPrefs.getPreferences().getInt(PYC_DELETE_HANDLING);
    }

}
