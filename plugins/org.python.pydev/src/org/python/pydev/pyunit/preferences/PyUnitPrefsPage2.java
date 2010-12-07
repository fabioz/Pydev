package org.python.pydev.pyunit.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.utils.ComboFieldEditor;
import org.python.pydev.utils.LabelFieldEditor;
import org.python.pydev.utils.MultiStringFieldEditor;

public class PyUnitPrefsPage2 extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    
    public static final int TEST_RUNNER_PYDEV = 0;
    public static final int TEST_RUNNER_NOSE = 1;
    public static final int TEST_RUNNER_PY_TEST = 2;
    
    public static final String TEST_RUNNER = "PYDEV_TEST_RUNNER";
    public static final int DEFAULT_TEST_RUNNER = TEST_RUNNER_PYDEV;
    
    public static final String TEST_RUNNER_DEFAULT_PARAMETERS = "PYDEV_TEST_RUNNER_DEFAULT_PARAMETERS";
    public static final String DEFAULT_TEST_RUNNER_DEFAULT_PARAMETERS = "--verbosity 0";
    
    public static final String USE_PYUNIT_VIEW = "PYDEV_USE_PYUNIT_VIEW";
    public static final boolean DEFAULT_USE_PYUNIT_VIEW = true;
    

    /**
     * Create the preference page.
     */
    public PyUnitPrefsPage2() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    /**
     * Creates the editors
     */
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        
        addField(createTestRunnerEditor(p));
        addField(new MultiStringFieldEditor(TEST_RUNNER_DEFAULT_PARAMETERS, "Parameters for test runner", p));
        addField(new BooleanFieldEditor(USE_PYUNIT_VIEW, "Show the results in the unittest results view?", p));
        String s = "Note: if unchecked, no xml-rpc communication will be done when running tests\nand the output will only be shown in the console.";
        addField(new LabelFieldEditor("LabelFieldEditor", s, p));
        
    }

    public static ComboFieldEditor createTestRunnerEditor(Composite p){
        return new ComboFieldEditor(
                 TEST_RUNNER, 
                 "Test Runner", 
                 new String[][] {
                     {"Pydev test runner", Integer.toString(TEST_RUNNER_PYDEV)},
                     {"Nose test runner", Integer.toString(TEST_RUNNER_NOSE)},
                 },
                 p
         );
    }
    
    /**
     * Initialize the preference page.
     */
    public void init(IWorkbench workbench) {
        // Initialize the preference page
    }

    public static String getTestRunnerParameters() {
        IPreferenceStore prefs = PydevPrefs.getPreferenceStore();
        int testRunner = prefs.getInt(TEST_RUNNER);
        String ret = prefs.getString(TEST_RUNNER_DEFAULT_PARAMETERS);
        
        switch(testRunner){
            case TEST_RUNNER_NOSE:
                ret = "--nose-params "+ret; //From this point onwards, only nose parameters.
                break;
        }
        
        return ret;
    }

    public static boolean getUsePyUnitView() {
        return PydevPrefs.getPreferenceStore().getBoolean(USE_PYUNIT_VIEW);
    }
    
    public static void showPage() {
        String id = "org.python.pydev.prefs.pyunitPage";
        PreferenceDialog prefDialog = PreferencesUtil.createPreferenceDialogOn(null, id, null, null);
        prefDialog.open();
    }

}
