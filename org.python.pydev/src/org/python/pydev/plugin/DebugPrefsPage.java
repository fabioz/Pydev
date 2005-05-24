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
		IntegerFieldEditor ife = new IntegerFieldEditor(PydevPrefs.CONNECT_TIMEOUT, "Connect timeout for debugger (ms)", p, 10);
		addField(ife);
	}

	

	/**
	 * Sets default preference values
	 */
	protected void initializeDefaultPreferences(Preferences prefs) {
	}
}
