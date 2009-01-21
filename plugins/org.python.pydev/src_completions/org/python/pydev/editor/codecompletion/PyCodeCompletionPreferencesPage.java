/*
 * Created on Aug 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.docutils.WordUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * The preferences for autocompletion should only be reactivated when the code completion feature gets better (more stable and precise).
 * 
 * @author Fabio Zadrozny
 */
public class PyCodeCompletionPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{

    public static final String USE_CODECOMPLETION = "USE_CODECOMPLETION";
    public static final boolean DEFAULT_USE_CODECOMPLETION = true;
    
    public static final String ATTEMPTS_CODECOMPLETION = "ATTEMPTS_CODECOMPLETION";
    public static final int DEFAULT_ATTEMPTS_CODECOMPLETION = 20;

    public static final String AUTOCOMPLETE_ON_DOT = "AUTOCOMPLETE_ON_DOT";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_DOT = true;
    
    public static final String AUTOCOMPLETE_ON_ALL_ASCII_CHARS = "AUTOCOMPLETE_ON_ALL_ASCII_CHARS";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_ALL_ASCII_CHARS = false;

    public static final String USE_AUTOCOMPLETE = "USE_AUTOCOMPLETE";
    public static final boolean DEFAULT_USE_AUTOCOMPLETE = true;

    public static final String AUTOCOMPLETE_DELAY = "AUTOCOMPLETE_DELAY";
    public static final int DEFAULT_AUTOCOMPLETE_DELAY = 0;

    public static final String AUTOCOMPLETE_ON_PAR = "AUTOCOMPLETE_ON_PAR";
    public static final boolean DEFAULT_AUTOCOMPLETE_ON_PAR = false;
    
    public static final String ARGUMENTS_DEEP_ANALYSIS_N_CHARS = "DEEP_ANALYSIS_N_CHARS";
    public static final int DEFAULT_ARGUMENTS_DEEP_ANALYSIS_N_CHARS = 1;
    
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

        addField(new IntegerFieldEditor(
                ATTEMPTS_CODECOMPLETION, "Timeout to connect to shell (secs).", p));

        addField(new IntegerFieldEditor(
                AUTOCOMPLETE_DELAY, "Autocompletion delay: ", p));

        
        
        String tooltip = WordUtils.wrap("Determines the number of chars in the qualifier request " +
                "for which constructs such as 'from xxx import yyy' should be " +
                "analyzed to get its actual token and if it maps to a method, its paramaters will be added in the completion.", 80);
        IntegerFieldEditor deepAnalysisFieldEditor = new IntegerFieldEditor(
                        ARGUMENTS_DEEP_ANALYSIS_N_CHARS, "Minimum number of chars in qualifier for\ndeep analysis for parameters in 'from' imports:", p);
        addField(deepAnalysisFieldEditor);
        deepAnalysisFieldEditor.getLabelControl(p).setToolTipText(tooltip);
        deepAnalysisFieldEditor.getTextControl(p).setToolTipText(tooltip);
        
        
        
        addField(new BooleanFieldEditor(
                USE_CODECOMPLETION, "Use code completion?", p));

        addField(new BooleanFieldEditor(
                AUTOCOMPLETE_ON_DOT, "Autocomplete on '.'?", p));

        addField(new BooleanFieldEditor(
                AUTOCOMPLETE_ON_PAR, "Autocomplete on '('?", p));
        
        addField(new BooleanFieldEditor(
                AUTOCOMPLETE_ON_PAR, "Autocomplete on ','?", p));
        
        addField(new BooleanFieldEditor(
                AUTOCOMPLETE_ON_ALL_ASCII_CHARS, "Autocomplete on all letter chars and '_'?", p));
        
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }

    public static boolean useCodeCompletion() {
        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.USE_CODECOMPLETION);
    }

    public static int getNumberOfConnectionAttempts() {
        try{
            Preferences preferences = PydevPrefs.getPreferences();
            return preferences.getInt(PyCodeCompletionPreferencesPage.ATTEMPTS_CODECOMPLETION);
        }catch (NullPointerException e) {
            return 20;
        }
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
    
    public static boolean useAutocompleteOnAllAsciiChars() {
        return PydevPrefs.getPreferences().getBoolean(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_ON_ALL_ASCII_CHARS);
    }
    
    public static int getAutocompleteDelay() {
        return PydevPrefs.getPreferences().getInt(PyCodeCompletionPreferencesPage.AUTOCOMPLETE_DELAY);
    }
    
    public static int getArgumentsDeepAnalysisNChars() {
        if(PydevPlugin.getDefault() == null){ //testing
            return 0;
        }
        return PydevPrefs.getPreferences().getInt(PyCodeCompletionPreferencesPage.ARGUMENTS_DEEP_ANALYSIS_N_CHARS);
    }


}