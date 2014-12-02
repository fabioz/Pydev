/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.pyunit.preferences;

import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.field_editors.ComboFieldEditor;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.MultiStringFieldEditor;
import org.python.pydev.shared_ui.tooltips.presenter.AbstractTooltipInformationPresenter;
import org.python.pydev.shared_ui.tooltips.presenter.ToolTipPresenterHandler;

public class PyUnitPrefsPage2 extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private class PyUnitPageLinkListener implements SelectionListener {

        private String fTag;

        public PyUnitPageLinkListener(String tag) {
            this.fTag = tag;
        }

        public void widgetSelected(SelectionEvent e) {
            int testRunner = getTestRunner();
            String tag;
            boolean addEquals = true;
            boolean addSpace = false;
            if (testRunner == TEST_RUNNER_PYDEV) {
                tag = "--" + fTag;

            } else if (testRunner == TEST_RUNNER_PY_TEST) {
                if ("n".equals(fTag)) {
                    tag = "-" + fTag;
                    addEquals = false;
                    addSpace = true;
                } else if ("showlocals".equals(fTag) || "runxfail".equals(fTag)) {
                    tag = "--" + fTag;
                    addEquals = false;
                } else {
                    //durations, maxfail, tb
                    tag = "--" + fTag;
                }

            } else {
                tag = fTag;
            }

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
                if (addEquals) {
                    buf.append('=');
                }
                if (addSpace) {
                    buf.append(' ');
                }
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
    private Composite parentPyDev;
    private Composite parentNose;
    private Composite parentPyTest;

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
    @Override
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

        final Composite parentAll = p;
        final StackLayout stackLayout = new StackLayout();

        final ComboFieldEditor comboField = createTestRunnerEditor(p);
        addField(comboField);
        Combo combo = comboField.getCombo();

        parametersField = new MultiStringFieldEditor(TEST_RUNNER_DEFAULT_PARAMETERS,
                "Parameters for test runner   (click links below to add flags)", p);
        addField(parametersField);

        addField(new BooleanFieldEditor(USE_PYUNIT_VIEW, "Show the results in the unittest results view?", p));

        String s = "Note: if unchecked, no xml-rpc communication will be done when running tests\nand the output will only be shown in the console.";
        addField(new LabelFieldEditor("LabelFieldEditor", s, p));

        String s2 = "Parameters for PyDev test runner (hover for description):" + "";
        addField(new LabelFieldEditor("LabelFieldEditor2", s2, p));

        final Composite contentPanel = new Composite(parentAll, SWT.None);
        contentPanel.setLayout(stackLayout);

        parentPyTest = p = new Composite(contentPanel, SWT.None);
        add("-<a>n</a> number of processes (requires xdist plugin)", "n",
                "Sets the number of processes to be used to run\n"
                        + "tests (requires py.test xdist plugin)\n\n", p);

        add("--<a>maxfail</a>=number (max number of failures)", "maxfail", "When the maximum number of failures\n"
                + "is reached execution stops.\n\n", p);

        add("--<a>tb</a>=long | native | short | line", "tb", "Traceback style.\n"
                + "long = the default informative traceback formatting\n"
                + "native = the Python standard library formatting\n"
                + "short = a shorter traceback format\n"
                + "line = only one line per failure\n\n", p);

        add("--<a>capture</a>=no | sys | fd", "capture", "Capture stdout/stderr\nno = disable capture\n"
                + "sys = replace stdout/stderr with in-mem files\n"
                + "fd = also point filedescriptors 1 and 2 to temp file\n\n", p);

        add("--<a>showlocals</a>", "showlocals", "Show local variables in tracebacks.\n\n", p);

        add("--<a>runxfail</a>", "runxfail", "Run tests even if they are marked xfali.\n\n", p);

        add("--<a>assert</a>=plain | reinterp | rewrite", "assert",
                "Control assertion debugging tools. 'plain' performs no\n" +
                        "assertion debugging. 'reinterp' reinterprets assert\n" +
                        "statements after they failed to provide assertion\n" +
                        "expression information. 'rewrite' (the default)\n" +
                        "rewrites assert statements in test modules on import\n" +
                        "to provide assert expression information..\n\n", p);

        add("--<a>durations</a>=number of slower tests to show", "durations", "Profiling test execution duration\n"
                + "(shows n slowest tests).\n\n", p);

        parentNose = p = new Composite(contentPanel, SWT.None);

        parentPyDev = p = new Composite(contentPanel, SWT.None);
        add("--<a>verbosity</a>=number", "verbosity", "Sets the verbosity level for the run (0-9)\n"
                + " 0: almost no output\n" + " 9: many details", p);

        add("--<a>jobs</a>=number", "jobs", "The number of processes to be used to run the tests.\n\n"
                + "The --split_jobs flag actually mandates how the tests will be scheduled (if jobs > 1).", p);

        add("--<a>split_jobs</a>=tests | module", "split_jobs",
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

        add("--<a>django</a>=true | false (default is true on django projects and false otherwise)", "django",
                "Whether the django runner should be used for setup/teardown of the django test environment\n\n", p);

        combo.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String comboValue = comboField.getComboValue();
                int val = 0;
                try {
                    val = Integer.parseInt(comboValue);
                } catch (NumberFormatException e1) {
                    Log.log(e1);
                    return;
                }
                layoutTestRunnerOptions(stackLayout, val, contentPanel);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

            }
        });

        layoutTestRunnerOptions(stackLayout, getTestRunner(), contentPanel);

    }

    private void add(String linkText, String flag, String tooltip, Composite p) {
        LinkFieldEditor field = new LinkFieldEditor("link_" + flag, linkText, p,
                new PyUnitPrefsPage2.PyUnitPageLinkListener(flag),
                tooltip + "\n", tooltipPresenter);
        addField(field);
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

    private void layoutTestRunnerOptions(final StackLayout stackLayout, final int val,
            final Composite contentPanel) {

        switch (val) {
            case TEST_RUNNER_PYDEV:
                stackLayout.topControl = parentPyDev;
                break;

            case TEST_RUNNER_PY_TEST:
                stackLayout.topControl = parentPyTest;
                break;

            case TEST_RUNNER_NOSE:
                stackLayout.topControl = parentNose;
                break;

        }
        contentPanel.layout();
    }

    public static String getTestRunnerParameters(ILaunchConfiguration config, IProject project) {
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
            default:
                //Only add --django when we have a django nature in the default runner
                try {
                    if (project.hasNature(PythonNature.DJANGO_NATURE_ID)) {
                        if (!ret.contains("--django")) {
                            ret += " --django=true";
                        }
                    }
                } catch (CoreException e) {
                    Log.log(e); //just ignore
                }
                break;
        }

        return ret;
    }

    /**
     * See: TEST_RUNNER_NOSE or TEST_RUNNER_PY_TEST (any other value indicates the default runner).
     */
    public static int getTestRunner() {
        IPreferenceStore prefs = PydevPrefs.getPreferenceStore();
        return prefs.getInt(TEST_RUNNER);
    }

    public static boolean getUsePyUnitView() {
        return PydevPrefs.getPreferenceStore().getBoolean(USE_PYUNIT_VIEW);
    }

    public static void showPage() {
        String id = "org.python.pydev.prefs.pyunitPage";
        PreferenceDialog prefDialog = PreferencesUtil.createPreferenceDialogOn(null, id, null, null);
        prefDialog.open();
    }

    public static boolean isPyTestRun() {
        return getTestRunner() == TEST_RUNNER_PY_TEST;
    }

}
