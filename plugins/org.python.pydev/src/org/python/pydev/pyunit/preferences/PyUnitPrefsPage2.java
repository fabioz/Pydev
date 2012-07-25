/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.pyunit.preferences;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.core.tooltips.presenter.AbstractTooltipInformationPresenter;
import org.python.pydev.core.tooltips.presenter.ToolTipPresenterHandler;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.utils.ComboFieldEditor;
import org.python.pydev.utils.LabelFieldEditor;
import org.python.pydev.utils.LinkFieldEditor;
import org.python.pydev.utils.MultiStringFieldEditor;

public class PyUnitPrefsPage2 extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private class PyUnitPageLinkListener implements SelectionListener {

        private String tag;

        public PyUnitPageLinkListener(String tag) {
            this.tag = "--" + tag;
        }

        public void widgetSelected(SelectionEvent e) {
            Text textControl = parametersField.getTextControl();
            String currentText = textControl.getText();
            StringTokenizer stringTokenizer = new StringTokenizer(currentText);
            FastStringBuffer buf = new FastStringBuffer(currentText.length() * 2);

            boolean found = false;
            while (stringTokenizer.hasMoreTokens()) {
                String tok = stringTokenizer.nextToken();
                if (tok.startsWith(tag)) {
                    found = true;
                }
                buf.append(tok);
                buf.append('\n');
            }
            if (!found) {
                buf.append(tag);
                buf.append('=');
            } else {
                buf.deleteLast(); //remove the last '\n'
            }
            textControl.setText(buf.toString());
            textControl.setSelection(textControl.getSize());
            textControl.setFocus();
        }

        public void widgetDefaultSelected(SelectionEvent e) {
        }

    }

    public static final int TEST_RUNNER_PYDEV = 0;
    public static final int TEST_RUNNER_NOSE = 1;
    public static final int TEST_RUNNER_PY_TEST = 2;

    public static final String[][] ENTRY_NAMES_AND_VALUES = new String[][] {
            { "PyDev test runner", Integer.toString(TEST_RUNNER_PYDEV) },
            { "Nose test runner", Integer.toString(TEST_RUNNER_NOSE) },
            { "Py.test runner", Integer.toString(TEST_RUNNER_PY_TEST) }, };

    public static final String TEST_RUNNER = "PYDEV_TEST_RUNNER";
    public static final int DEFAULT_TEST_RUNNER = TEST_RUNNER_PYDEV;

    public static final String TEST_RUNNER_DEFAULT_PARAMETERS = "PYDEV_TEST_RUNNER_DEFAULT_PARAMETERS";
    public static final String DEFAULT_TEST_RUNNER_DEFAULT_PARAMETERS = "--verbosity 0";

    public static final String USE_PYUNIT_VIEW = "PYDEV_USE_PYUNIT_VIEW";
    public static final boolean DEFAULT_USE_PYUNIT_VIEW = true;

    public static final String LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS_CHOICE = "LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS_CHOICE";
    public static final String LAUNCH_CONFIG_OVERRIDE_TEST_RUNNER = "LAUNCH_CONFIG_OVERRIDE_TEST_RUNNER";
    public static final String LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS = "LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS";
    private MultiStringFieldEditor parametersField;
    private ToolTipPresenterHandler tooltipPresenter;

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
        IInformationPresenter presenter = new AbstractTooltipInformationPresenter() {

            @Override
            protected void onUpdatePresentation(String hoverInfo, TextPresentation presentation) {
            }

            @Override
            protected void onHandleClick(Object data) {
            }
        };

        Composite p = getFieldEditorParent();
        tooltipPresenter = new ToolTipPresenterHandler(p.getShell(), presenter,
                "Tip: Click on the link to add it as a parameter to the test runner.");

        addField(createTestRunnerEditor(p));
        parametersField = new MultiStringFieldEditor(TEST_RUNNER_DEFAULT_PARAMETERS,
                "Parameters for test runner   (click links below to add flags)", p);
        addField(parametersField);

        addField(new BooleanFieldEditor(USE_PYUNIT_VIEW, "Show the results in the unittest results view?", p));

        String s = "Note: if unchecked, no xml-rpc communication will be done when running tests\nand the output will only be shown in the console.";
        addField(new LabelFieldEditor("LabelFieldEditor", s, p));

        String s2 = "Parameters for PyDev test runner (hover for description):" + "";
        addField(new LabelFieldEditor("LabelFieldEditor2", s2, p));

        add("--<a>verbosity</a>=number", "verbosity", "Sets the verbosity level for the run (0-9)\n"
                + " 0: almost no output\n" + " 9: many details", p);

        add("--<a>jobs</a>=number", "jobs", "The number of processes to be used to run the tests.\n\n"
                + "The --split_jobs flag actually mandates how the tests will be scheduled (if jobs > 1).", p);

        add("--<a>split_jobs</a>=tests|module", "split_jobs",
                "tests:  if 'tests' is passed (default), a process will randomly get a\n"
                        + "\tnew test to be run after the current one finishes running.\n\n"
                        + "module: if 'module' is passed, a given job will always run all the\n"
                        + "\ttests from a module and then get a new module to run tests from.", p);

        add("--<a>include_files</a>=comma separated list of patterns to match files to include", "include_files",
                "Patters to match filenames to be included during test discovery.\n\n"
                        + "Patters are fnmatch-style patterns (i.e.: test*, todo* and not regexps).\n\n"
                        + "Note that *.py,*.pyw files are already pre-selected, so, patterns\n"
                        + "will be matched against those pre-selected by default.", p);

        add("--<a>exclude_files</a>=comma separated list of patterns to match files to exclude", "exclude_files",
                "Patters to match filenames to be excluded during test discovery.\n\n"
                        + "Patters are fnmatch-style patterns (i.e.: test*, todo* and not regexps).\n\n"
                        + "Note that *.py,*.pyw files are already pre-selected, so, patterns\n"
                        + "will be matched against those pre-selected by default.", p);

        add("--<a>include_tests</a>=comma separated list of patterns to match tests to include", "include_tests",
                "Patters to match tests (method names) to be included during test discovery.\n\n"
                        + "Patters are fnmatch-style patterns (i.e.: *_todo, *_slow and not regexps).\n\n", p);

        add("--<a>exclude_tests</a>=comma separated list of patterns to match tests to exclude", "exclude_tests",
                "Patters to match tests (method names) to be excluded during test discovery.\n\n"
                        + "Patters are fnmatch-style patterns (i.e.: *_todo, *_slow and not regexps).\n\n", p);

    }

    private void add(String linkText, String flag, String tooltip, Composite p) {
        addField(new LinkFieldEditor("link_" + flag, linkText, p, new PyUnitPrefsPage2.PyUnitPageLinkListener(flag),
                tooltip + "\n", tooltipPresenter));
    }

    public static ComboFieldEditor createTestRunnerEditor(Composite p) {
        return new ComboFieldEditor(TEST_RUNNER, "Test Runner", ENTRY_NAMES_AND_VALUES, p);
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
        if (override) {
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

        switch (testRunner) {
            case TEST_RUNNER_NOSE:
                ret = "--nose-params " + ret; //From this point onwards, only nose parameters.
                break;
            case TEST_RUNNER_PY_TEST:
                ret = "--py-test-params " + ret; //From this point onwards, only py.test parameters.
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
