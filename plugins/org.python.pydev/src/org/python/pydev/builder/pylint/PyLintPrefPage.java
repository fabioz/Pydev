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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;
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

    public static final boolean DEFAULT_USE_CONSOLE = true;

    //args
    public static final String PYLINT_ARGS = "PYLINT_ARGS";

    public static final String DEFAULT_PYLINT_ARGS = "";

    //delta
    public static final String MAX_PYLINT_DELTA = "MAX_PYLINT_DELTA";

    public static final int DEFAULT_MAX_PYLINT_DELTA = 4;

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
        addField(new IntegerFieldEditor(MAX_PYLINT_DELTA, "Max simultaneous processes for PyLint?", p));
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
                        + "Add --rcfile=.pylintrc to use an rcfile relative to the project directory.", p);
        addField(stringFieldEditor);

        String w = "";
        Button button = new Button(p, SWT.NONE);
        button.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                final String w = "\n\nTo ignore some warning on a line in a file, you can put the comment: \n" +
                        "#IGNORE:ID, so that the id is the warning that you want to ignore. \n" +
                        "E.g.: if you have the code:\n\n" +
                        "from foo import * #IGNORE:W0401\n\n" +
                        "The wildcard import will be ignored.\n\n" +
                        "NOTE:for warnings to appear in the problems view, you have\n" +
                        "to set your filter to accept the org.python.pydev.pylintproblem type!\n\n" +
                        "NOTE2: Make sure that your file is a valid module in the PYTHONPATH, because\n" +
                        "pylint doesn't analyze the file itself, but the module itself (you should\n" +
                        "be able to import it from python without giving the file path).";

                MessageDialog.openInformation(p.getShell(), "Help", w);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });
        button.setText("Click for help (ignoring errors and troubleshooting)");
        GridData d = new GridData();
        d.horizontalAlignment = GridData.FILL;
        d.grabExcessHorizontalSpace = true;
        button.setLayoutData(d);

        FieldEditor fe = new LabelFieldEditor("Help", w, p);
        addField(fe);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
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

    public static boolean useErrors() {
        return eSeverity() != SEVERITY_IGNORE;
    }

    public static boolean useWarnings() {
        return wSeverity() != SEVERITY_IGNORE;
    }

    public static boolean useFatal() {
        return fSeverity() != SEVERITY_IGNORE;
    }

    public static boolean useCodingStandard() {
        return cSeverity() != SEVERITY_IGNORE;
    }

    public static boolean useRefactorTips() {
        return rSeverity() != SEVERITY_IGNORE;
    }

    public static boolean useConsole() {
        return PydevPrefs.getPreferences().getBoolean(USE_CONSOLE);
    }

    public static String getPyLintArgs() {
        return PydevPrefs.getPreferences().getString(PYLINT_ARGS);
    }

    public static int getMaxPyLintDelta() {
        return PydevPrefs.getPreferences().getInt(MAX_PYLINT_DELTA);
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