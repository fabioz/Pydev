/*
 * Author: atotic
 * Created: Jun 23, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.plugin;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Debug preferences.
 * 
 * <p>Simple 1 page debug preferences page.
 * <p>Prefeernce constants are defined in Constants.java
 */
public class PyunitPrefsPage extends FieldEditorPreferencePage 
	implements IWorkbenchPreferencePage{

    public static final String PYUNIT_VERBOSITY = "PYUNIT_VERBOSITY";
    public static final int DEFAULT_PYUNIT_VERBOSITY = 2;

	/**
	 * Initializer sets the preference store
	 */
	public PyunitPrefsPage() {
		super("Debug", GRID);
		setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
	}

	public void init(IWorkbench workbench) {
	}
	
	/**
	 * Creates the editors
	 */
	protected void createFieldEditors() {
		Composite p = getFieldEditorParent();

 		RadioGroupFieldEditor editor= new RadioGroupFieldEditor(
 				PYUNIT_VERBOSITY, 
 				"Verbosity", 
 				1,
	 			new String[][] {
	 				{"Verbose - prints name of test as it runs", "2"},
	 				{"Quiet - prints '.' as each test runs", "1"},
	 				{"Silent - prints nothing", "0"},
	 			},
	 			p
 		);	
		
//		IntegerFieldEditor editor = new IntegerFieldEditor(PydevPrefs.PYUNIT_VERBOSITY, "Verbosity (0=xxx\n, 1=xxx\n, 2=xxx\n)?", p, 1);
		
		addField(editor);
	}

	

	/**
	 * Sets default preference values
	 */
	protected void initializeDefaultPreferences(Preferences prefs) {
	}
}
