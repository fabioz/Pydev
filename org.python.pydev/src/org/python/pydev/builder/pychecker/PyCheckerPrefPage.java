/*
 * Created on Oct 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.pychecker;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class PyCheckerPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    public static final String PYCHECKER_FILE_LOCATION = "PYCHECKER_FILE_LOCATION";

    public static final String USE_PYCHECKER = "USE_PYCHECKER";

    public static final boolean DEFAULT_USE_PYCHECKER = true;

    public PyCheckerPrefPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Pychecker");
    }

    public static void initializeDefaultPreferences(Preferences prefs) {
        try {
            File s = PydevPlugin.getScriptWithinPySrc("ThirdParty/pychecker/checker.py");
            prefs.setDefault(PYCHECKER_FILE_LOCATION, s.toString());
            prefs.setDefault(USE_PYCHECKER, DEFAULT_USE_PYCHECKER);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(USE_PYCHECKER, "Use pychecker?", p));
        FileFieldEditor fileField = new FileFieldEditor(PYCHECKER_FILE_LOCATION, "Location of pychecker:", true, p);
        addField(fileField);
    }

    public void init(IWorkbench workbench) {
    }

    /**
     * @return
     */
    public static String getPyCheckerLocation() {
        return PydevPrefs.getPreferences().getString(PYCHECKER_FILE_LOCATION);
    }

    /**
     * should we use py checker?
     * @return
     */
    public static boolean usePyChecker() {
		if(!isPycheckerConfigured(PyCheckerPrefPage.getPyCheckerLocation()))
			return false;
		
        return PydevPrefs.getPreferences().getBoolean(USE_PYCHECKER);
    }

	/**
	 * Checks if location of pychecker is properly configured.
	 */
	static boolean isPycheckerConfigured(String pycheckerLocation) {
		
		File pychecker = new File(pycheckerLocation);

		if(!pychecker.exists() && pychecker.isFile()) {
			return false;
		}
		return true;
	}
}