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
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
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

    //args
    public static final String PYLINT_ARGS = "PYLINT_ARGS";

    public static final String DEFAULT_PYLINT_ARGS = "";

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
        final Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(USE_PYLINT, "Use PyLint?", p));
        addField(new BooleanFieldEditor(USE_CONSOLE, "Redirect PyLint output to console?", p));
        FileFieldEditor fileField = new FileFieldEditor(PYLINT_FILE_LOCATION, "Location of the pylint executable:",
                true, p);
        addField(fileField);

        addField(new RadioGroupFieldEditor(SEVERITY_FATAL, "FATAL Severity", COLS, LABEL_AND_VALUE, p, true));

        addField(new RadioGroupFieldEditor(SEVERITY_ERRORS, "ERRORS Severity", COLS, LABEL_AND_VALUE, p, true));

        addField(new RadioGroupFieldEditor(SEVERITY_WARNINGS, "WARNINGS Severity", COLS, LABEL_AND_VALUE, p, true));

        addField(new RadioGroupFieldEditor(SEVERITY_CODING_STANDARD, "CONVENTIONS Severity", COLS, LABEL_AND_VALUE, p,
                true));

        addField(new RadioGroupFieldEditor(SEVERITY_REFACTOR, "REFACTOR Severity", COLS, LABEL_AND_VALUE, p, true));

        CustomizableFieldEditor stringFieldEditor = new CustomizableFieldEditor(PYLINT_ARGS,
                "Arguments to pass to the pylint command (customize its output):\n"
                        + "Add --rcfile=.pylintrc to use an rcfile relative to the project directory.",
                p);
        addField(stringFieldEditor);
        addField(new LinkFieldEditor("PYLINT_HELP",
                "View <a>http://www.pydev.org/manual_adv_pylint.html</a> for help.",
                p,
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

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {

    }

    /**
     * @return
     */
    public static String getPyLintLocation() {
        return PydevPrefs.getPreferences().getString(PYLINT_FILE_LOCATION);
    }

    private static boolean communicatedOnce = false;

    /**
     * should we use py lint?
     *
     * @return
     */
    public static boolean usePyLint() {
        boolean b = PydevPrefs.getPreferences().getBoolean(USE_PYLINT);

        if (!isPyLintConfigured(PyLintPrefPage.getPyLintLocation())) {
            if (b && !communicatedOnce) {
                communicatedOnce = true;
                Log.log("Unable to use pylint because it is not properly configured.");
            }
            return false;
        }

        return b;
    }

    /**
     * Checks if location of pylint is properly configured.
     */
    public static boolean isPyLintConfigured(String pylintLocation) {

        File pylint = new File(pylintLocation);

        if (!pylint.exists() && pylint.isFile()) {
            return false;
        }
        return true;
    }

    public static boolean useConsole() {
        return PydevPrefs.getPreferences().getBoolean(USE_CONSOLE);
    }

    public static String getPyLintArgs() {
        return PydevPrefs.getPreferences().getString(PYLINT_ARGS);
    }

    public static int wSeverity() {
        return PydevPrefs.getPreferences().getInt(SEVERITY_WARNINGS);
    }

    public static int eSeverity() {
        return PydevPrefs.getPreferences().getInt(SEVERITY_ERRORS);
    }

    public static int fSeverity() {
        return PydevPrefs.getPreferences().getInt(SEVERITY_FATAL);
    }

    public static int cSeverity() {
        return PydevPrefs.getPreferences().getInt(SEVERITY_CODING_STANDARD);
    }

    public static int rSeverity() {
        return PydevPrefs.getPreferences().getInt(SEVERITY_REFACTOR);
    }

}