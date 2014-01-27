/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 25, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.AutoConfigMaker;
import org.python.pydev.ui.pythonpathconf.IInterpreterProviderFactory.InterpreterType;
import org.python.pydev.ui.pythonpathconf.InterpreterConfigHelpers;
import org.python.pydev.utils.ICallback;

/**
 * @author Fabio Zadrozny
 */
public class PyProjectPythonDetails extends PropertyPage {

    /**
     * This class provides a way to show to the user the options available to configure a project with the
     * correct interpreter and grammar.
     */
    public static class ProjectInterpreterAndGrammarConfig {
        private static final String INTERPRETER_NOT_CONFIGURED_MSG = "<a>Please configure an interpreter before proceeding.</a>";
        public Button radioPy;
        public Button radioJy;
        public Button radioIron;
        public Combo comboGrammarVersion;
        public Label versionLabel;
        public Combo interpretersChoice;
        private Link interpreterNoteText;
        private SelectionListener selectionListener;
        private ICallback onSelectionChanged;
        private Label interpreterLabel;

        public ProjectInterpreterAndGrammarConfig() {
            //Don't want to display "config interpreter" dialog when this dialog does that already.
            PyDialogHelpers.enableAskInterpreterStep(false);
        }

        /**
         * Optionally, a callback may be passed to be called whenever the selection of the project type changes.
         */
        public ProjectInterpreterAndGrammarConfig(ICallback callback) {
            this.onSelectionChanged = callback;
        }

        public Control doCreateContents(Composite p) {
            Composite topComp = new Composite(p, SWT.NONE);
            GridLayout innerLayout = new GridLayout();
            innerLayout.numColumns = 1;
            innerLayout.marginHeight = 0;
            innerLayout.marginWidth = 0;
            topComp.setLayout(innerLayout);
            GridData gd = new GridData(GridData.FILL_BOTH);
            topComp.setLayoutData(gd);

            //Project type
            Group group = new Group(topComp, SWT.NONE);
            group.setText("Choose the project type");
            GridLayout layout = new GridLayout();
            layout.horizontalSpacing = 8;
            layout.numColumns = 3;
            group.setLayout(layout);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            group.setLayoutData(gd);

            radioPy = new Button(group, SWT.RADIO | SWT.LEFT);
            radioPy.setText("Python");

            radioJy = new Button(group, SWT.RADIO | SWT.LEFT);
            radioJy.setText("Jython");

            radioIron = new Button(group, SWT.RADIO | SWT.LEFT);
            radioIron.setText("IronPython");

            //Grammar version
            versionLabel = new Label(topComp, 0);
            versionLabel.setText("Grammar Version");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            versionLabel.setLayoutData(gd);

            comboGrammarVersion = new Combo(topComp, SWT.READ_ONLY);
            for (String s : IPythonNature.Versions.VERSION_NUMBERS) {
                comboGrammarVersion.add(s);
            }

            gd = new GridData(GridData.FILL_HORIZONTAL);
            comboGrammarVersion.setLayoutData(gd);

            //Interpreter
            interpreterLabel = new Label(topComp, 0);
            interpreterLabel.setText("Interpreter");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            interpreterLabel.setLayoutData(gd);

            //interpreter configured in the project
            final String[] idToConfig = new String[] { "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPagePython" };
            interpretersChoice = new Combo(topComp, SWT.READ_ONLY);
            selectionListener = new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent e) {

                }

                /**
                 * @param e can be null to force an update.
                 */
                public void widgetSelected(SelectionEvent e) {
                    if (e != null) {
                        Button source = (Button) e.getSource();
                        if (!source.getSelection()) {
                            return; //we'll get 2 notifications: selection of one and deselection of the other, so, let's just treat the selection
                        }
                    }

                    IInterpreterManager interpreterManager;

                    if (radioJy.getSelection()) {
                        interpreterManager = PydevPlugin.getJythonInterpreterManager();

                    } else if (radioIron.getSelection()) {
                        interpreterManager = PydevPlugin.getIronpythonInterpreterManager();

                    } else {
                        interpreterManager = PydevPlugin.getPythonInterpreterManager();
                    }

                    IInterpreterInfo[] interpretersInfo = interpreterManager.getInterpreterInfos();
                    if (interpretersInfo.length > 0) {
                        ArrayList<String> interpretersWithDefault = new ArrayList<String>();
                        interpretersWithDefault.add(IPythonNature.DEFAULT_INTERPRETER);
                        for (IInterpreterInfo info : interpretersInfo) {
                            interpretersWithDefault.add(info.getName());
                        }
                        interpretersChoice.setItems(interpretersWithDefault.toArray(new String[0]));

                        interpretersChoice.setVisible(true);
                        interpreterNoteText.setText("<a>Click here to configure an interpreter not listed.</a>");
                        interpretersChoice.setText(IPythonNature.DEFAULT_INTERPRETER);

                    } else {
                        interpretersChoice.setVisible(false);
                        interpreterNoteText.setText(INTERPRETER_NOT_CONFIGURED_MSG);

                    }
                    //config which preferences page should be opened!
                    switch (interpreterManager.getInterpreterType()) {
                        case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                            idToConfig[0] = "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPagePython";
                            break;

                        case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                            idToConfig[0] = "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPageJython";
                            break;

                        case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                            idToConfig[0] = "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPageIronpython";
                            break;

                        default:
                            throw new RuntimeException("Cannot recognize type: "
                                    + interpreterManager.getInterpreterType());

                    }
                    triggerCallback();
                }
            };

            gd = new GridData(GridData.FILL_HORIZONTAL);
            interpretersChoice.setLayoutData(gd);
            radioPy.addSelectionListener(selectionListener);
            radioJy.addSelectionListener(selectionListener);
            radioIron.addSelectionListener(selectionListener);

            interpreterNoteText = new Link(topComp, SWT.LEFT | SWT.WRAP);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            interpreterNoteText.setLayoutData(gd);

            interpreterNoteText.addSelectionListener(new SelectionListener() {
                public void widgetSelected(SelectionEvent e) {
                    String interpreterName = getProjectInterpreter();
                    if (interpreterName != null) {
                        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
                                null, idToConfig[0], null, null);
                        dialog.open();
                        //just to re-update it again
                        selectionListener.widgetSelected(null);
                    }
                    else {
                        MessageDialog mdialog = new MessageDialog(null, "Configure interpreter", null,
                                "How would you like to configure the interpreter?", MessageDialog.QUESTION,
                                InterpreterConfigHelpers.CONFIG_NAMES, 0);
                        int open = mdialog.open();
                        if (open == InterpreterConfigHelpers.CONFIG_MANUAL) {
                            PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null,
                                    idToConfig[0], null, null);
                            dialog.open();
                            //just to re-update it again
                            selectionListener.widgetSelected(null);
                        }
                        else if (open != PyDialogHelpers.INTERPRETER_CANCEL_CONFIG) {
                            //auto-config
                            InterpreterType interpreterType;
                            if (radioJy.getSelection()) {
                                interpreterType = InterpreterType.JYTHON;
                            } else if (radioIron.getSelection()) {
                                interpreterType = InterpreterType.IRONPYTHON;
                            } else {
                                interpreterType = InterpreterType.PYTHON;
                            }

                            JobChangeAdapter onJobComplete = new JobChangeAdapter() {
                                @Override
                                public void done(IJobChangeEvent event) {
                                    //Update the display when the configuration has ended.
                                    Display.getDefault().asyncExec(new Runnable() {
                                        public void run() {
                                            //Only update if the page is still there.
                                            //If something is disposed, it has been closed.
                                            if (!interpreterNoteText.isDisposed()) {
                                                selectionListener.widgetSelected(null);
                                            }
                                        }
                                    });
                                };
                            };

                            interpreterNoteText.setText("Configuration in progress...");
                            boolean advanced = open == InterpreterConfigHelpers.CONFIG_ADV_AUTO;
                            AutoConfigMaker a = new AutoConfigMaker(interpreterType, advanced, null, null);
                            if (a.autoConfigSingleApply(onJobComplete)) {
                                triggerCallback();
                            } else {
                                selectionListener.widgetSelected(null);
                            }
                        }
                    }
                }

                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });

            return topComp;
        }

        private void triggerCallback() {
            if (onSelectionChanged != null) {
                try {
                    onSelectionChanged.call(null);
                } catch (Exception e1) {
                    Log.log(e1);
                }
            }
        }

        /**
         * @return a string as specified in the constants in IPythonNature
         * @see IPythonNature#PYTHON_VERSION_XXX
         * @see IPythonNature#JYTHON_VERSION_XXX
         * @see IPythonNature#IRONPYTHON_VERSION_XXX
         */
        public String getSelectedPythonOrJythonAndGrammarVersion() {
            if (radioPy.getSelection()) {
                return "python " + comboGrammarVersion.getText();
            }
            if (radioJy.getSelection()) {
                return "jython " + comboGrammarVersion.getText();
            }
            if (radioIron.getSelection()) {
                return "ironpython " + comboGrammarVersion.getText();
            }
            throw new RuntimeException("Some radio must be selected");
        }

        public String getProjectInterpreter() {
            if (INTERPRETER_NOT_CONFIGURED_MSG.equals(interpreterNoteText.getText())) {
                return null;
            }
            return interpretersChoice.getText();
        }

        public void setDefaultSelection() {
            radioPy.setSelection(true);
            comboGrammarVersion.setText(IPythonNature.Versions.LAST_VERSION_NUMBER);
            //Just to update things
            this.selectionListener.widgetSelected(null);
        }

    }

    /**
     * The element.
     */
    public IAdaptable element;
    public ProjectInterpreterAndGrammarConfig projectConfig = new ProjectInterpreterAndGrammarConfig();

    /*
     *  (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
     */
    @Override
    public IAdaptable getElement() {
        return element;
    }

    /**
     * Sets the element that owns properties shown on this page.
     *
     * @param element the element
     */
    @Override
    public void setElement(IAdaptable element) {
        this.element = element;
    }

    public IProject getProject() {
        return (IProject) getElement().getAdapter(IProject.class);
    }

    @Override
    public Control createContents(Composite p) {
        Control contents = projectConfig.doCreateContents(p);
        setSelected();
        return contents;
    }

    private void setSelected() {
        PythonNature pythonNature = PythonNature.getPythonNature(getProject());
        try {
            //Set whether it's Python/Jython
            String version = pythonNature.getVersion();
            if (IPythonNature.Versions.ALL_PYTHON_VERSIONS.contains(version)) {
                projectConfig.radioPy.setSelection(true);

            } else if (IPythonNature.Versions.ALL_IRONPYTHON_VERSIONS.contains(version)) {
                projectConfig.radioIron.setSelection(true);

            } else if (IPythonNature.Versions.ALL_JYTHON_VERSIONS.contains(version)) {
                projectConfig.radioJy.setSelection(true);
            }

            //We must set the grammar version too (that's from a string in the format "Python 2.4" and we only want
            //the version).
            String v = StringUtils.split(version, ' ').get(1);
            projectConfig.comboGrammarVersion.setText(v);

            //Update interpreter
            projectConfig.selectionListener.widgetSelected(null);
            String configuredInterpreter = pythonNature.getProjectInterpreterName();
            if (configuredInterpreter != null) {
                projectConfig.interpretersChoice.setText(configuredInterpreter);
            }

        } catch (CoreException e) {
            Log.log(e);
        }
    }

    @Override
    protected void performApply() {
        doIt();
    }

    @Override
    public boolean performOk() {
        return doIt();
    }

    @Override
    public boolean performCancel() {
        //re-enable "configure interpreter" dialogs
        PyDialogHelpers.enableAskInterpreterStep(true);
        return super.performCancel();
    }

    private boolean doIt() {
        IProject project = getProject();

        if (project != null) {
            PythonNature pythonNature = PythonNature.getPythonNature(project);

            try {
                String projectInterpreter = projectConfig.getProjectInterpreter();
                if (projectInterpreter == null) {
                    return false;
                }
                pythonNature.setVersion(projectConfig.getSelectedPythonOrJythonAndGrammarVersion(), projectInterpreter);
            } catch (CoreException e) {
                Log.log(e);
            }
        }
        //re-enable "configure interpreter" dialogs
        PyDialogHelpers.enableAskInterpreterStep(true);
        return true;
    }
}