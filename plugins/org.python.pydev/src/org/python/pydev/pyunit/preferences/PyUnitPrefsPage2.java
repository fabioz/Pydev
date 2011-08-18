/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.pyunit.preferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.utils.ComboFieldEditor;
import org.python.pydev.utils.LabelFieldEditor;
import org.python.pydev.utils.MultiStringFieldEditor;

public class PyUnitPrefsPage2 extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
    
    public static final int TEST_RUNNER_PYDEV = 0;
    public static final int TEST_RUNNER_NOSE = 1;
    public static final int TEST_RUNNER_PY_TEST = 2;
    
    public static final String[][] ENTRY_NAMES_AND_VALUES = new String[][] {
                         {"PyDev test runner", Integer.toString(TEST_RUNNER_PYDEV)},
                         {"Nose test runner", Integer.toString(TEST_RUNNER_NOSE)},
                         {"Py.test runner", Integer.toString(TEST_RUNNER_PY_TEST)},
                     };
    
    public static final String TEST_RUNNER = "PYDEV_TEST_RUNNER";
    public static final int DEFAULT_TEST_RUNNER = TEST_RUNNER_PYDEV;
    
    public static final String TEST_RUNNER_DEFAULT_PARAMETERS = "PYDEV_TEST_RUNNER_DEFAULT_PARAMETERS";
    public static final String DEFAULT_TEST_RUNNER_DEFAULT_PARAMETERS = "--verbosity 0";
    
    public static final String USE_PYUNIT_VIEW = "PYDEV_USE_PYUNIT_VIEW";
    public static final boolean DEFAULT_USE_PYUNIT_VIEW = true;
    
    
    public static final String LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS_CHOICE = "LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS_CHOICE";
    public static final String LAUNCH_CONFIG_OVERRIDE_TEST_RUNNER = "LAUNCH_CONFIG_OVERRIDE_TEST_RUNNER";
    public static final String LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS = "LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS";
    
    
    

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
        String s2 = "Parameters for PyDev test runner:\n\n" +
        		"    --verbosity=number\n" +
        		"        Sets the verbosity level for the run\n\n" +
        		"    --jobs=number\n" +
        		"        The number of processes to be used to run the tests\n\n" +
        		"    --split_jobs=tests|module\n" +
        		"        if tests is passed (default), the tests will be split\n" +
        		"        independently to each process if module is passed, a\n" +
        		"        given job will always receive all the tests from a module" +
        		"\n" +
        		"";
        addField(new LabelFieldEditor("LabelFieldEditor2", s2, p));
        
    }

    public static ComboFieldEditor createTestRunnerEditor(Composite p){
        return new ComboFieldEditor(
                 TEST_RUNNER, 
                 "Test Runner", 
                 ENTRY_NAMES_AND_VALUES,
                 p
         );
    }
    
    /**
     * Initialize the preference page.
     */
    public void init(IWorkbench workbench) {
        // Initialize the preference page
    }

    public static String getTestRunnerParameters(ILaunchConfiguration config) {
        boolean override = false;
        try {
            override = config.getAttribute(LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS_CHOICE, false);
        } catch (CoreException e) {
            Log.log(e);
        }
        IPreferenceStore prefs = PydevPrefs.getPreferenceStore();
        int testRunner = prefs.getInt(TEST_RUNNER);
        String ret = prefs.getString(TEST_RUNNER_DEFAULT_PARAMETERS);
        if(override){
            try {
                testRunner = config.getAttribute(LAUNCH_CONFIG_OVERRIDE_TEST_RUNNER, testRunner);
            } catch (CoreException e) {
                Log.log(e);
            }
            try {
                ret = config.getAttribute(LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS, ret);
            } catch (CoreException e) {
                Log.log(e);
            }
        }
        
        switch(testRunner){
            case TEST_RUNNER_NOSE:
                ret = "--nose-params "+ret; //From this point onwards, only nose parameters.
                break;
            case TEST_RUNNER_PY_TEST:
                ret = "--py-test-params "+ret; //From this point onwards, only py.test parameters.
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
