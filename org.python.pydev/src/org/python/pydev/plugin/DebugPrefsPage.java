/*
 * Author: atotic
 * Created: Jun 23, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.plugin;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.ui.InterpreterEditor;

/**
 * Debug preferences.
 * 
 * <p>Simple 1 page debug preferences page.
 * <p>Prefeernce constants are defined in Constants.java
 */
public class DebugPrefsPage extends FieldEditorPreferencePage 
	implements IWorkbenchPreferencePage{


	/**
	 * Initializer sets the preference store
	 */
	public DebugPrefsPage() {
		super(GRID);
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
	}

	public void init(IWorkbench workbench) {
		String interpreterPath = getPreferenceStore().getString(PydevPrefs.INTERPRETER_PATH);
		// If the interpreter path is empty, always try to come up with something
		if (interpreterPath == null || interpreterPath.length() == 0) {
			getPreferenceStore().setDefault(PydevPrefs.INTERPRETER_PATH, getDefaultInterpreterPath());
			getPreferenceStore().setToDefault(PydevPrefs.INTERPRETER_PATH);
		}
	}
	
	/**
	 * Creates the editors
	 */
	protected void createFieldEditors() {
		Composite p = getFieldEditorParent();
		InterpreterEditor pathEditor = new InterpreterEditor (
		PydevPrefs.INTERPRETER_PATH, "Python interpreters (for example python.exe)", p);
		addField(pathEditor);
		IntegerFieldEditor ife = new IntegerFieldEditor(PydevPrefs.CONNECT_TIMEOUT, "Connect timeout (ms)", p, 1);
		ife.setValidRange(1000, 200000);	
		addField(ife);
	}

	/**
	 * Return the default python executable
	 * I tried making this smarter, but you can't do much without getenv(PATH)
	 */
	private String getDefaultInterpreterPath() {
		String executable = "python";
		return executable;
// ideally, I'd search the system path here, but getenv has been disabled
// some code on finding the binary
//		java.util.Properties p = System.getProperties();
//		java.util.Enumeration keys = p.keys();
//		while( keys.hasMoreElements() ) {
//			System.out.println( keys.nextElement() );
//		}
//		StringBuffer retVal = new StringBuffer();
//		String sysPath = System.getProperty("sys.path");
//		if (sysPath == null) 
//			sysPath = System.getenv("PATH");
//		if (sysPath != null) {
//			StringTokenizer st = new StringTokenizer(sysPath, File.pathSeparator + "\n\r");
//			while (st.hasMoreElements()) {
//				String path =st.nextToken();
//				System.out.println(path);
//			}
//		}
//		return retVal.toString();
	}
	

	/**
	 * Sets default preference values
	 */
	protected void initializeDefaultPreferences(Preferences prefs) {
		prefs.setDefault(PydevPrefs.INTERPRETER_PATH, getDefaultInterpreterPath());
	}
}
