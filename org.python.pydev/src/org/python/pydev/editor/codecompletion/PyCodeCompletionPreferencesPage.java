/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevPrefs;
import org.python.pydev.utils.LabelFieldEditor;

/**
 * The preferences for autocompletion should only be reactivated when the code completion feature gets better (more stable and precise).
 * 
 * @author Fabio Zadrozny
 */
public class PyCodeCompletionPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    public static final String USE_CODECOMPLETION = "USE_CODECOMPLETION";
    public static final boolean DEFAULT_USE_CODECOMPLETION = false;
    
    //	public static final String AUTOCOMPLETE_ON_DOT = "AUTOCOMPLETE_ON_DOT";
    //	public static final boolean DEFAULT_AUTOCOMPLETE_ON_DOT = true;
    //
    //	public static final String USE_AUTOCOMPLETE = "USE_AUTOCOMPLETE";
    //	public static final boolean DEFAULT_USE_AUTOCOMPLETE = true;
    //
    //	public static final String AUTOCOMPLETE_DELAY = "AUTOCOMPLETE_DELAY";
    //	public static final int DEFAULT_AUTOCOMPLETE_DELAY = 250;
    //
    //	public static final String AUTOCOMPLETE_ON_PAR = "AUTOCOMPLETE_ON_PAR";
    //	public static final boolean DEFAULT_AUTOCOMPLETE_ON_PAR = false;
    private Label labelWarning;

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
		        USE_CODECOMPLETION, "Use code completion?", p));

        String w = "\nWARNINGS for code completion:\n\n" 
            	+ "Code completion works on top of a python shell and really \n"
                + "EXECUTES THE CODE YOU WRITE on the top level on the module.\n\n"
                + "So, you should NEVER call code that allocates resources or \n" 
                + "make any other dangerous initializations in the global scope of \n" 
                + "the module (not only because of code completion, as a simple \n" 
                + "import of that code would be dangerous).\n\n"
                + "Code completion might also not work if the interpreter is not \n" 
                + "correctly set, as it creates a shell to make code completion.\n\n" 
        		+ "Code completion is activated by Ctrl+Space, as are the templates, so,\n" 
				+ "if you stop using code completion, the templates should still appear.\n\n" +
				"AUTOCOMPLETION NOTE: autocompletion has been deactivated by default because\n" +
				"sometimes it would seem that the editor hanged, and many times no tips are\n" +
				"gotten, e.g.: Tips on parameters are NEVER gotten.\n\n" +
				"See http://pydev.sourceforge.net/codecompletion.html for more information.\n";

        FieldEditor fe = new LabelFieldEditor("Warning", w, p);
        addField(fe);


        //        addField(new BooleanFieldEditor(
        //		        USE_AUTOCOMPLETE, "Use autocompletion?", p));
        //
        //		addField(new IntegerFieldEditor(
        //		        AUTOCOMPLETE_DELAY, "Autocompletion delay: ", p));
        //
        //		addField(new BooleanFieldEditor(
        //		        AUTOCOMPLETE_ON_DOT, "Autocomplete on '.'?", p));
        //
        //		addField(new BooleanFieldEditor(
        //		        AUTOCOMPLETE_ON_PAR, "Autocomplete on '('?", p));

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    /**
     * Sets default preference values
     */
    public static void initializeDefaultPreferences(Preferences prefs) {
		prefs.setDefault(USE_CODECOMPLETION, DEFAULT_USE_CODECOMPLETION);
        //		prefs.setDefault(AUTOCOMPLETE_ON_DOT, DEFAULT_AUTOCOMPLETE_ON_DOT);
        //		prefs.setDefault(USE_AUTOCOMPLETE, DEFAULT_USE_AUTOCOMPLETE);
        //		prefs.setDefault(AUTOCOMPLETE_DELAY, DEFAULT_AUTOCOMPLETE_DELAY);
        //		prefs.setDefault(AUTOCOMPLETE_ON_PAR, DEFAULT_AUTOCOMPLETE_ON_PAR);
    }

    public static boolean useCodeCompletion() {
        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.USE_CODECOMPLETION);
    }

    //    public static boolean isToAutocompleteOnDot() {
    //        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_DOT);
    //    }
    //
    //    public static boolean isToAutocompleteOnPar() {
    //        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_PAR);
    //    }
    //
    //    public static boolean useAutocomplete() {
    //        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.USE_AUTOCOMPLETE);
    //    }
    //
    //    public static int getAutocompleteDelay() {
    //        return PydevPrefs.getPreferences().getInt(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_DELAY);
    //    }


}