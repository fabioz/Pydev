/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeCompletionPreferencesPage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage , Preferences.IPropertyChangeListener {

    
	public static final String AUTOCOMPLETE_ON_DOT = "AUTOCOMPLETE_ON_DOT";
	public static final boolean DEFAULT_AUTOCOMPLETE_ON_DOT = true;

	public static final String USE_AUTOCOMPLETE = "USE_AUTOCOMPLETE";
	public static final boolean DEFAULT_USE_AUTOCOMPLETE = true;

	public static final String AUTOCOMPLETE_DELAY = "AUTOCOMPLETE_DELAY";
	public static final int DEFAULT_AUTOCOMPLETE_DELAY = 250;

	public static final String AUTOCOMPLETE_ON_PAR = "AUTOCOMPLETE_ON_PAR";
	public static final boolean DEFAULT_AUTOCOMPLETE_ON_PAR = false;

	/**
     */
    public PyCodeCompletionPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    protected void createFieldEditors() {
		Composite p = getFieldEditorParent();

		addField(new BooleanFieldEditor(
		        USE_AUTOCOMPLETE, "Use autocompletion?", p));

		addField(new IntegerFieldEditor(
		        AUTOCOMPLETE_DELAY, "Autocompletion delay: ", p));

		addField(new BooleanFieldEditor(
		        AUTOCOMPLETE_ON_DOT, "Autocomplete on '.'?", p));

		addField(new BooleanFieldEditor(
		        AUTOCOMPLETE_ON_PAR, "Autocomplete on '('?", p));
}

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
        // TODO Auto-generated method stub
        PydevPrefs.getPreferences().addPropertyChangeListener(this);
    }
	/**
	 * Sets default preference values
	 */
	public static void initializeDefaultPreferences(Preferences prefs) {
		prefs.setDefault(AUTOCOMPLETE_ON_DOT, DEFAULT_AUTOCOMPLETE_ON_DOT);
		prefs.setDefault(USE_AUTOCOMPLETE, DEFAULT_USE_AUTOCOMPLETE);
		prefs.setDefault(AUTOCOMPLETE_DELAY, DEFAULT_AUTOCOMPLETE_DELAY);
		prefs.setDefault(AUTOCOMPLETE_ON_PAR, DEFAULT_AUTOCOMPLETE_ON_PAR);
	}

    public static boolean isToAutocompleteOnDot() {
        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_DOT);
    }

    public static boolean isToAutocompleteOnPar() {
        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_PAR);
    }

    public static boolean useAutocomplete() {
        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.USE_AUTOCOMPLETE);
    }

    public static int getAutocompleteDelay() {
        return PydevPrefs.getPreferences().getInt(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_DELAY);
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
//		System.out.println( event.getProperty()
//		 + "\n\told setting: "
//		 + event.getOldValue()
//		 + "\n\tnew setting: "
//		 + event.getNewValue());
        
    }

}