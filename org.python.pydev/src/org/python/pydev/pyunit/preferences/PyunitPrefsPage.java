/*
 * Author: atotic
 * Created: Jun 23, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.pyunit.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.LabelFieldEditor;

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
    public static final String PYUNIT_TEST_FILTER = "PYUNIT_TEST_FILTER";
    public static final String DEFAULT_PYUNIT_TEST_FILTER = "";
    /**
     * Initializer sets the preference store
     */
    public PyunitPrefsPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
    }
    
    /**
     * Creates the editors
     */
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

         RadioGroupFieldEditor verbosity_editor= new RadioGroupFieldEditor(
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

         StringFieldEditor filter_editor = new StringFieldEditor( 
                 PYUNIT_TEST_FILTER, 
                 "Filter (regex)", 
                 p);

        String s = "filter examples:\n" +
        ".* or blank - all tests\n" +
        "_abc.* - any test with method name starting with 'test_abc'. \n" +
        "         matches test_abc, test_abc123, test_abcXXXXXX, etc. \n" +
        "_abc,_123 - comma seperate (no spaces) filter for more values \n" + 
        "\n" +
        "Note: this filters on the method names of all <TestCase>s found\n" + 
        "      the string 'test' is automatically prepended to the regex\n";
         
        addField(verbosity_editor);
        addField(filter_editor);
        addField(new LabelFieldEditor("LabelFieldEditor", s, p));
    }

    

    /**
     * Sets default preference values
     */
    protected void initializeDefaultPreferences(Preferences prefs) {
    }
}
