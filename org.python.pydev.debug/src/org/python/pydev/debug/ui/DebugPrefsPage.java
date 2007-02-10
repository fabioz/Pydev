/*
 * Author: atotic
 * Created: Jun 23, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui;

import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

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
	@SuppressWarnings("unchecked")
    protected void createFieldEditors() {
		Composite p = getFieldEditorParent();
		addField(new IntegerFieldEditor(PydevPrefs.CONNECT_TIMEOUT, "Connect timeout for debugger (ms)", p, 10));
        List<IDebugPreferencesPageParticipant> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_DEBUG_PREFERENCES_PAGE);
        for (IDebugPreferencesPageParticipant participant : participants) {
            participant.createFieldEditors(this, p);
        }
	}

    /**
     * Make it available for extensions
     */
    @Override
    public void addField(FieldEditor editor) {
        super.addField(editor);
    }
	

	/**
	 * Sets default preference values
	 */
	protected void initializeDefaultPreferences(Preferences prefs) {
	}
}
