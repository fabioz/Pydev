/*
 * Created on Oct 26, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pylint;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class PyLintPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final String PYLINT_FILE_LOCATION = "PYLINT_FILE_LOCATION";
    private static final String USE_PYLINT = "USE_PYLINT";
    private static final boolean DEFAULT_USE_PYLINT = true;

    private static final String USE_ERRORS = "USE_ERRORS";
    private static final boolean DEFAULT_USE_ERRORS = true;

    private static final String USE_WARNINGS = "USE_WARNINGS";
    private static final boolean DEFAULT_USE_WARNINGS = false;

    private static final String USE_FATAL = "USE_FATAL";
    private static final boolean DEFAULT_USE_FATAL = true;

    private static final String USE_CODING_STANDARD = "USE_CODING_STANDARD";
    private static final boolean DEFAULT_USE_CODING_STANDARD = false;

    private static final String USE_REFACTOR = "USE_REFACTOR";
    private static final boolean DEFAULT_USE_REFACTOR = false;

    private static final String PYLINT_ARGS = "PYLINT_ARGS";
    private static final String DEFAULT_PYLINT_ARGS = "--persistent=n --comment=n --disable-msg=0312";
    
    public PyLintPrefPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Pylint");
    }

    public static void initializeDefaultPreferences(Preferences prefs) {
        try {
            File s = PydevPlugin.getScriptWithinPySrc("ThirdParty/logilab/pylint/lint.py");
            prefs.setDefault(PYLINT_FILE_LOCATION, s.toString());
            prefs.setDefault(USE_PYLINT, DEFAULT_USE_PYLINT);
            prefs.setDefault(USE_ERRORS, DEFAULT_USE_ERRORS);
            prefs.setDefault(USE_WARNINGS, DEFAULT_USE_WARNINGS);
            prefs.setDefault(USE_FATAL, DEFAULT_USE_FATAL);
            prefs.setDefault(USE_CODING_STANDARD, DEFAULT_USE_CODING_STANDARD);
            prefs.setDefault(USE_REFACTOR, DEFAULT_USE_REFACTOR);
            prefs.setDefault(PYLINT_ARGS, DEFAULT_PYLINT_ARGS);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(USE_PYLINT, "Use pylint?", p));
        FileFieldEditor fileField = new FileFieldEditor(PYLINT_FILE_LOCATION, "Location of pylint:", true, p);
        addField(fileField);
        
        addField(new BooleanFieldEditor(USE_FATAL, "Communicate found fatal errors?", p));
        addField(new BooleanFieldEditor(USE_ERRORS, "Communicate found errors?", p));
        addField(new BooleanFieldEditor(USE_WARNINGS, "Communicate found warnings?", p));
        addField(new BooleanFieldEditor(USE_CODING_STANDARD, "Communicate coding standard warnings?", p));
        addField(new BooleanFieldEditor(USE_REFACTOR, "Communicate refactor warnings?", p));
        
        addField(new StringFieldEditor(PYLINT_ARGS, "Arguments to pass to pylint (customize its output).\n" +
        		"The  --include-ids=y is always included and does not appear here..", p));
        
    }

    /* (non-Javadoc)
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

    /**
     * should we use py lint?
     * @return
     */
    public static boolean usePyLint() {
		if(!isPylintConfigured(PyLintPrefPage.getPyLintLocation()))
			return false;
		
        return PydevPrefs.getPreferences().getBoolean(USE_PYLINT);
    }

	/**
	 * Checks if location of pylint is properly configured.
	 */
	public static boolean isPylintConfigured(String pylintLocation) {
		
		File pylint = new File(pylintLocation);

		if(!pylint.exists() && pylint.isFile()) {
			return false;
		}
		return true;
	}

	public static boolean useErrors(){
        return PydevPrefs.getPreferences().getBoolean(USE_ERRORS);
	}
	public static boolean useWarnings(){
        return PydevPrefs.getPreferences().getBoolean(USE_WARNINGS);
	}
	public static boolean useFatal(){
        return PydevPrefs.getPreferences().getBoolean(USE_FATAL);
	}
	
    public static boolean useCodingStandard() {
        return PydevPrefs.getPreferences().getBoolean(USE_CODING_STANDARD);
    }

    public static boolean useRefactorTips() {
        return PydevPrefs.getPreferences().getBoolean(USE_REFACTOR);
    }

    public static String getPylintArgs(){
        return PydevPrefs.getPreferences().getString(PYLINT_ARGS);
	}

}
