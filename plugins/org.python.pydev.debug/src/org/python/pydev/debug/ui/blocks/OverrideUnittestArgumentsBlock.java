/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.blocks;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.pyunit.preferences.PyUnitPrefsPage2;


public class OverrideUnittestArgumentsBlock extends AbstractLaunchConfigurationTab {

    private Button buttonAskOverride;
    private Combo comboSelectRunner;
    private Text textRunnerParameters;

    @Override
    public void createControl(Composite parent) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        setControl(group);

        GridLayout topLayout = new GridLayout();
        group.setLayout(topLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        group.setLayoutData(gd);
        group.setFont(font);
        group.setText("PyUnit");

        buttonAskOverride = new Button(group, SWT.CHECK);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        buttonAskOverride.setLayoutData(gd);
        buttonAskOverride.setFont(font);
        buttonAskOverride.setText("Override PyUnit preferences for this launch?");
        buttonAskOverride.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateOverrideState();
                updateLaunchConfigurationDialog();
            }
        });

        comboSelectRunner = new Combo(group, SWT.SINGLE | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        comboSelectRunner.setLayoutData(gd);
        comboSelectRunner.setFont(font);
        for (String[] s : PyUnitPrefsPage2.ENTRY_NAMES_AND_VALUES) {
            comboSelectRunner.add(s[0]);
            comboSelectRunner.setData(s[0], Integer.parseInt(s[1]));
        }
        comboSelectRunner.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                updateLaunchConfigurationDialog();
            }
        });

        textRunnerParameters = new Text(group, SWT.MULTI | SWT.BORDER);
        gd = new GridData(GridData.FILL_BOTH);
        textRunnerParameters.setLayoutData(gd);
        textRunnerParameters.setFont(font);

        textRunnerParameters.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent evt) {
                updateLaunchConfigurationDialog();
            }
        });

    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS_CHOICE, (String) null);
        configuration.setAttribute(PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_TEST_RUNNER, (String) null);
        configuration.setAttribute(PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS, (String) null);
    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {

        //Override selection
        IPreferenceStore prefs = PydevPrefs.getPreferenceStore();
        try {
            buttonAskOverride.setSelection(configuration.getAttribute(
                    PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS_CHOICE, false));
        } catch (CoreException e) {
            buttonAskOverride.setSelection(false);
            Log.log(e);
        }

        //Test runner
        boolean testRunnerSet = false;
        try {
            int defaultTestRunner = prefs.getInt(PyUnitPrefsPage2.TEST_RUNNER);

            int testRunner = configuration.getAttribute(PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_TEST_RUNNER,
                    defaultTestRunner);
            for (String[] s : PyUnitPrefsPage2.ENTRY_NAMES_AND_VALUES) {
                if (Integer.parseInt(s[1]) == testRunner) {
                    comboSelectRunner.setText(s[0]);
                    testRunnerSet = true;
                    break;
                }
            }
        } catch (CoreException e) {
            Log.log(e);
        }
        if (!testRunnerSet) {
            comboSelectRunner.setText(PyUnitPrefsPage2.ENTRY_NAMES_AND_VALUES[0][0]);
        }

        //Parameters
        try {
            String params = configuration.getAttribute(PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS,
                    prefs.getString(PyUnitPrefsPage2.TEST_RUNNER_DEFAULT_PARAMETERS));
            textRunnerParameters.setText(params);
        } catch (CoreException e) {
            Log.log(e);
        }
        updateOverrideState();
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS_CHOICE,
                buttonAskOverride.getSelection());
        int data = (Integer) comboSelectRunner.getData(comboSelectRunner.getText());
        configuration.setAttribute(PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_TEST_RUNNER, data);
        configuration.setAttribute(PyUnitPrefsPage2.LAUNCH_CONFIG_OVERRIDE_PYUNIT_RUN_PARAMS,
                textRunnerParameters.getText());
    }

    @Override
    public String getName() {
        return "PyUnit";
    }

    protected void updateOverrideState() {
        boolean sel = buttonAskOverride.getSelection();
        comboSelectRunner.setEnabled(sel);
        textRunnerParameters.setEnabled(sel);
    }

}
