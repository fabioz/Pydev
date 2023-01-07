/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 26, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pylint;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.field_editors.ArgsStringFieldEditor;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.RadioGroupFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

import com.python.pydev.analysis.PyAnalysisScopedPreferences;
import com.python.pydev.analysis.pylint.PyLintPreferences;

/**
 * @author Fabio Zadrozny
 */
public class PyLintPrefPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final int COLS = 4;

    public static final String[][] LABEL_AND_VALUE = new String[][] {
            { "Error", String.valueOf(IMarker.SEVERITY_ERROR) },
            { "Warning", String.valueOf(IMarker.SEVERITY_WARNING) },
            { "Info", String.valueOf(IMarker.SEVERITY_INFO) },
            { "Ignore", String.valueOf(PyLintPreferences.SEVERITY_IGNORE) }, };

    public static final String[][] SEARCH_PYLINT_LOCATION_OPTIONS = new String[][] {
            { "Search in interpreter", PyLintPreferences.LOCATION_SEARCH },
            { "Specify Location", PyLintPreferences.LOCATION_SPECIFY },
    };

    private FileFieldEditor fileField;

    private RadioGroupFieldEditor searchPyLintLocation;

    private Composite parent;

    public PyLintPrefPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("PyLint");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    protected void createFieldEditors() {
        parent = getFieldEditorParent();

        addField(new BooleanFieldEditor(PyLintPreferences.USE_PYLINT, "Use PyLint?", parent));
        addField(new BooleanFieldEditor(PyLintPreferences.USE_CONSOLE, "Redirect PyLint output to console?", parent));
        searchPyLintLocation = new RadioGroupFieldEditor(PyLintPreferences.SEARCH_PYLINT_LOCATION,
                "PyLint to use", 2, SEARCH_PYLINT_LOCATION_OPTIONS, parent);

        for (Button b : searchPyLintLocation.getRadioButtons()) {
            b.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateSelectFileEnablement(parent);
                }
            });
        }

        addField(searchPyLintLocation);
        fileField = new FileFieldEditor(PyLintPreferences.PYLINT_FILE_LOCATION, "Location of the pylint executable:",
                true, parent);
        addField(fileField);

        addField(new RadioGroupFieldEditor(PyLintPreferences.SEVERITY_FATAL, "FATAL Severity", COLS, LABEL_AND_VALUE,
                parent, true));

        addField(new RadioGroupFieldEditor(PyLintPreferences.SEVERITY_ERRORS, "ERRORS Severity", COLS, LABEL_AND_VALUE,
                parent, true));

        addField(new RadioGroupFieldEditor(PyLintPreferences.SEVERITY_WARNINGS, "WARNINGS Severity", COLS,
                LABEL_AND_VALUE, parent, true));

        addField(new RadioGroupFieldEditor(PyLintPreferences.SEVERITY_CODING_STANDARD, "CONVENTIONS Severity", COLS,
                LABEL_AND_VALUE, parent, true));

        addField(new RadioGroupFieldEditor(PyLintPreferences.SEVERITY_REFACTOR, "REFACTOR Severity", COLS,
                LABEL_AND_VALUE, parent, true));

        addField(new RadioGroupFieldEditor(PyLintPreferences.SEVERITY_INFO, "INFORMATIONAL Severity", COLS,
                LABEL_AND_VALUE, parent, true));

        ArgsStringFieldEditor stringFieldEditor = new ArgsStringFieldEditor(PyLintPreferences.PYLINT_ARGS,
                "Arguments to pass to the pylint command (customize its output):\n"
                        + "Add --rcfile=.pylintrc to use an rcfile relative to the project directory.",
                parent);
        addField(stringFieldEditor);
        addField(new LinkFieldEditor("PYLINT_HELP",
                "View <a>http://www.pydev.org/manual_adv_pylint.html</a> for help.",
                parent,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Program.launch("http://www.pydev.org/manual_adv_pylint.html");
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                }));
        addField(new ScopedPreferencesFieldEditor(parent, PyAnalysisScopedPreferences.ANALYSIS_SCOPE, this));
    }

    @Override
    protected void initialize() {
        super.initialize();
        updateSelectFileEnablement(parent);
    }

    protected void updateSelectFileEnablement(Composite p) {
        fileField.setEnabled(PyLintPreferences.LOCATION_SPECIFY.equals(searchPyLintLocation.getRadioValue()), p);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {

    }

}