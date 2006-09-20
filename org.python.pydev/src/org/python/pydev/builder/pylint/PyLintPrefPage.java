/*
 * Created on Oct 26, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pylint;

import java.io.File;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.utils.CustomizableFieldEditor;
import org.python.pydev.utils.LabelFieldEditor;

/**
 * @author Fabio Zadrozny
 */
public class PyLintPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String PYLINT_FILE_LOCATION = "PYLINT_FILE_LOCATION";

	public static final String USE_PYLINT = "USE_PYLINT";

	public static final boolean DEFAULT_USE_PYLINT = false;

	public static final String USE_ERRORS = "USE_ERRORS";

	public static final boolean DEFAULT_USE_ERRORS = true;

	public static final String USE_WARNINGS = "USE_WARNINGS";

	public static final boolean DEFAULT_USE_WARNINGS = false;

	public static final String USE_FATAL = "USE_FATAL";

	public static final boolean DEFAULT_USE_FATAL = true;

	public static final String USE_CODING_STANDARD = "USE_CODING_STANDARD";

	public static final boolean DEFAULT_USE_CODING_STANDARD = false;

	public static final String USE_REFACTOR = "USE_REFACTOR";

	public static final boolean DEFAULT_USE_REFACTOR = false;

	public static final String USE_CONSOLE = "USE_CONSOLE";

	public static final boolean DEFAULT_USE_CONSOLE = true;

	public static final String PYLINT_ARGS = "PYLINT_ARGS";

	public static final String MAX_PYLINT_DELTA = "MAX_PYLINT_DELTA";

	public static final int DEFAULT_MAX_PYLINT_DELTA = 4;

	public static final String DEFAULT_PYLINT_ARGS = "";

	public PyLintPrefPage() {
		super(FLAT);
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
		setDescription("Pylint");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	protected void createFieldEditors() {
		final Composite p = getFieldEditorParent();

		addField(new BooleanFieldEditor(USE_PYLINT, "Use pylint?", p));
		addField(new BooleanFieldEditor(USE_CONSOLE, "Redirect Pylint output to console?", p));
		addField(new IntegerFieldEditor(MAX_PYLINT_DELTA, "Max delta to run PyLint?", p));
		FileFieldEditor fileField = new FileFieldEditor(PYLINT_FILE_LOCATION, "Location of pylint (lint.py):", true, p);
		addField(fileField);

		addField(new BooleanFieldEditor(USE_FATAL, "Communicate FATAL?", p));
		addField(new BooleanFieldEditor(USE_ERRORS, "Communicate ERRORS?", p));
		addField(new BooleanFieldEditor(USE_WARNINGS, "Communicate WARNINGS?", p));
		addField(new BooleanFieldEditor(USE_CODING_STANDARD, "Communicate CONVENTIONS?", p));
		addField(new BooleanFieldEditor(USE_REFACTOR, "Communicate REFACTOR?", p));

		CustomizableFieldEditor stringFieldEditor = new CustomizableFieldEditor(PYLINT_ARGS, "Arguments to pass to pylint (customize its output).\n"
				+ "The  --include-ids=y is always included and does not appear here..", p);
		addField(stringFieldEditor);

		String w = "";
		Button button = new Button(p, SWT.NONE);
		button.addSelectionListener(new SelectionListener(){

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

		if (!isPylintConfigured(PyLintPrefPage.getPyLintLocation())) {
			if (b && !communicatedOnce) {
				communicatedOnce = true;
				PydevPlugin.log("Unable to use pylint because it is not properly configured.");
			}
			return false;
		}

		return b;
	}

	/**
	 * Checks if location of pylint is properly configured.
	 */
	public static boolean isPylintConfigured(String pylintLocation) {

		File pylint = new File(pylintLocation);

		if (!pylint.exists() && pylint.isFile()) {
			return false;
		}
		return true;
	}

	public static boolean useErrors() {
		return PydevPrefs.getPreferences().getBoolean(USE_ERRORS);
	}

	public static boolean useWarnings() {
		return PydevPrefs.getPreferences().getBoolean(USE_WARNINGS);
	}

	public static boolean useFatal() {
		return PydevPrefs.getPreferences().getBoolean(USE_FATAL);
	}

	public static boolean useCodingStandard() {
		return PydevPrefs.getPreferences().getBoolean(USE_CODING_STANDARD);
	}

	public static boolean useRefactorTips() {
		return PydevPrefs.getPreferences().getBoolean(USE_REFACTOR);
	}

	public static boolean useConsole() {
		return PydevPrefs.getPreferences().getBoolean(USE_CONSOLE);
	}

	public static String getPylintArgs() {
		return PydevPrefs.getPreferences().getString(PYLINT_ARGS);
	}

	public static int getMaxPyLintDelta() {
		return PydevPrefs.getPreferences().getInt(MAX_PYLINT_DELTA);
	}

}