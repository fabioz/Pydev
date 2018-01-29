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

import java.io.File;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.RadioGroupFieldEditor;
import org.python.pydev.utils.CustomizableFieldEditor;

/**
 * @author Fabio Zadrozny
 */
public class PyLintPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PYLINT_FILE_LOCATION = "PYLINT_FILE_LOCATION";

    public static final String USE_PYLINT = "USE_PYLINT";

    public static final boolean DEFAULT_USE_PYLINT = false;

    public static final int SEVERITY_IGNORE = -1;

    public static final int COLS = 4;

    public static final String[][] LABEL_AND_VALUE = new String[][] {
            { "Error", String.valueOf(IMarker.SEVERITY_ERROR) },
            { "Warning", String.valueOf(IMarker.SEVERITY_WARNING) }, { "Info", String.valueOf(IMarker.SEVERITY_INFO) },
            { "Ignore", String.valueOf(SEVERITY_IGNORE) }, };

    // errors
    public static final String SEVERITY_ERRORS = "SEVERITY_ERRORS";

    public static final int DEFAULT_SEVERITY_ERRORS = IMarker.SEVERITY_ERROR;

    //warnings
    public static final String SEVERITY_WARNINGS = "SEVERITY_WARNINGS";

    public static final int DEFAULT_SEVERITY_WARNINGS = IMarker.SEVERITY_WARNING;

    //fatal
    public static final String SEVERITY_FATAL = "SEVERITY_FATAL";

    public static final int DEFAULT_SEVERITY_FATAL = IMarker.SEVERITY_ERROR;

    //coding std
    public static final String SEVERITY_CODING_STANDARD = "SEVERITY_CODING_STANDARD";

    public static final int DEFAULT_SEVERITY_CODING_STANDARD = SEVERITY_IGNORE;

    //refactor
    public static final String SEVERITY_REFACTOR = "SEVERITY_REFACTOR";

    public static final int DEFAULT_SEVERITY_REFACTOR = SEVERITY_IGNORE;

    //console
    public static final String USE_CONSOLE = "USE_CONSOLE";
    public static final boolean DEFAULT_USE_CONSOLE = false;

    public static final String SEARCH_PYLINT_LOCATION = "SEARCH_PYLINT_LOCATION";
    private static final String LOCATION_SEARCH = "SEARCH";
    private static final String LOCATION_SPECIFY = "SPECIFY";
    public static final String DEFAULT_SEARCH_PYLINT_LOCATION = LOCATION_SEARCH;

    public static final String[][] SEARCH_PYLINT_LOCATION_OPTIONS = new String[][] {
            { "Search in interpreter", LOCATION_SEARCH },
            { "Specify Location", LOCATION_SPECIFY },
    };

    //args
    public static final String PYLINT_ARGS = "PYLINT_ARGS";

    public static final String DEFAULT_PYLINT_ARGS = "";

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

        addField(new BooleanFieldEditor(USE_PYLINT, "Use PyLint?", parent));
        addField(new BooleanFieldEditor(USE_CONSOLE, "Redirect PyLint output to console?", parent));
        searchPyLintLocation = new RadioGroupFieldEditor(SEARCH_PYLINT_LOCATION,
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
        fileField = new FileFieldEditor(PYLINT_FILE_LOCATION, "Location of the pylint executable:",
                true, parent);
        addField(fileField);

        addField(new RadioGroupFieldEditor(SEVERITY_FATAL, "FATAL Severity", COLS, LABEL_AND_VALUE, parent, true));

        addField(new RadioGroupFieldEditor(SEVERITY_ERRORS, "ERRORS Severity", COLS, LABEL_AND_VALUE, parent, true));

        addField(
                new RadioGroupFieldEditor(SEVERITY_WARNINGS, "WARNINGS Severity", COLS, LABEL_AND_VALUE, parent, true));

        addField(new RadioGroupFieldEditor(SEVERITY_CODING_STANDARD, "CONVENTIONS Severity", COLS, LABEL_AND_VALUE,
                parent,
                true));

        addField(
                new RadioGroupFieldEditor(SEVERITY_REFACTOR, "REFACTOR Severity", COLS, LABEL_AND_VALUE, parent, true));

        CustomizableFieldEditor stringFieldEditor = new CustomizableFieldEditor(PYLINT_ARGS,
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

    }

    @Override
    protected void initialize() {
        super.initialize();
        updateSelectFileEnablement(parent);
    }

    protected void updateSelectFileEnablement(Composite p) {
        fileField.setEnabled(LOCATION_SPECIFY.equals(searchPyLintLocation.getRadioValue()), p);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {

    }

    public static File getPyLintLocation(IPythonNature pythonNature) {
        IEclipsePreferences preferences = PydevPrefs.getEclipsePreferences();
        if (LOCATION_SPECIFY.equals(preferences.get(SEARCH_PYLINT_LOCATION, DEFAULT_SEARCH_PYLINT_LOCATION))) {
            return new File(preferences.get(PYLINT_FILE_LOCATION, ""));
        }
        try {
            return pythonNature.getProjectInterpreter().searchExecutableForInterpreter("pylint", false);
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

    /**
     * should we use py lint?
     *
     * @return
     */
    public static boolean usePyLint() {
        return PydevPrefs.getEclipsePreferences().getBoolean(USE_PYLINT, DEFAULT_USE_PYLINT);
    }

    public static boolean useConsole() {
        return PydevPrefs.getEclipsePreferences().getBoolean(USE_CONSOLE, DEFAULT_USE_CONSOLE);
    }

    public static String getPyLintArgs() {
        return PydevPrefs.getEclipsePreferences().get(PYLINT_ARGS, DEFAULT_PYLINT_ARGS);
    }

    public static int wSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_WARNINGS, DEFAULT_SEVERITY_WARNINGS);
    }

    public static int eSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_ERRORS, DEFAULT_SEVERITY_ERRORS);
    }

    public static int fSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_FATAL, DEFAULT_SEVERITY_FATAL);
    }

    public static int cSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_CODING_STANDARD, DEFAULT_SEVERITY_CODING_STANDARD);
    }

    public static int rSeverity() {
        return PydevPrefs.getEclipsePreferences().getInt(SEVERITY_REFACTOR, DEFAULT_SEVERITY_REFACTOR);
    }

}