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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.python.pydev.ast.interpreter_managers.IInterpreterProviderFactory.InterpreterType;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IGrammarVersionProvider.AdditionalGrammarVersionsToCheck;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.ArrayUtils;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.dialogs.SelectNDialog;
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterPreferencesPage;
import org.python.pydev.ui.pythonpathconf.AutoConfigMaker;
import org.python.pydev.ui.pythonpathconf.InterpreterConfigHelpers;
import org.python.pydev.ui.pythonpathconf.ObtainInterpreterInfoOperation;

/**
 * @author Fabio Zadrozny
 */
public class PyProjectPythonDetails extends PropertyPage {

    private static final String ADDITIONAL_SYNTAX_PREFIX = "Additional syntax validation: ";
    private static final String ADDITIONAL_SYNTAX_NO_SELECTED = ADDITIONAL_SYNTAX_PREFIX
            + "<no additional grammars selected>.";

    /**
     * This class provides a way to show to the user the options available to configure a project with the
     * correct interpreter and grammar.
     */
    public static class ProjectInterpreterAndGrammarConfig {
        private static final String DEFAULT_PREFIX = IPythonNature.DEFAULT_INTERPRETER + "  --  currently: ";
        private static final String INTERPRETER_NOT_CONFIGURED_MSG = "<a>Please configure an interpreter before proceeding.</a>";
        public Button radioPy;
        public Button radioJy;
        public Button radioIron;
        public Combo comboGrammarVersion;
        public Label versionLabel;
        public Combo interpretersChoice;
        private Link interpreterNoteText;
        private SelectionListener selectionListener;
        private ICallback0<Object> onSelectionChanged;
        private Label interpreterLabel;
        private Label labelAdditionalGrammarsSelected;
        private ICallback0<String> getProjectLocation;

        public ProjectInterpreterAndGrammarConfig(ICallback0<String> getProjectLocation) {
            //Don't want to display "config interpreter" dialog when this dialog does that already.
            PyDialogHelpers.enableAskInterpreterStep(false);
            this.getProjectLocation = getProjectLocation;
        }

        /**
         * Optionally, a callback may be passed to be called whenever the selection of the project type changes.
         */
        public ProjectInterpreterAndGrammarConfig(ICallback0<Object> onSelectionChanged,
                ICallback0<String> getProjectLocation, CallbackWithListeners<Event> onLocationChanged) {
            this.onSelectionChanged = onSelectionChanged;
            this.getProjectLocation = getProjectLocation;
            onLocationChanged.registerListener((event) -> {
                updateInterpretersAndDefaultInCombo();
                return null;
            });
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
                s = numberToUi(s);
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
            final String[] idToConfig = new String[] {
                    "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPagePython" };
            interpretersChoice = new Combo(topComp, SWT.READ_ONLY);
            selectionListener = new SelectionListener() {

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {

                }

                /**
                 * @param e can be null to force an update.
                 */
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (e != null) {
                        Button source = (Button) e.getSource();
                        if (!source.getSelection()) {
                            return; //we'll get 2 notifications: selection of one and deselection of the other, so, let's just treat the selection
                        }
                    }

                    IInterpreterManager interpreterManager = getInterpreterManager();
                    int interpreterType = interpreterManager.getInterpreterType();

                    IInterpreterInfo[] interpretersInfo = interpreterManager.getInterpreterInfos();
                    if (interpretersInfo.length > 0) {
                        String defaultEntry = DEFAULT_PREFIX + interpretersInfo[0].getName();

                        if (interpreterType == IInterpreterManager.INTERPRETER_TYPE_PYTHON) {
                            File projectlocation = new File(getProjectLocation.call());
                            IInterpreterInfo pipenvInterpreterInfoForProjectLocation = PythonNature
                                    .getPipenvInterpreterInfoForProjectLocation(interpretersInfo, projectlocation);
                            if (pipenvInterpreterInfoForProjectLocation != null) {
                                defaultEntry = DEFAULT_PREFIX + pipenvInterpreterInfoForProjectLocation.getName();
                            }
                        }

                        ArrayList<String> interpretersWithDefault = new ArrayList<String>();
                        interpretersWithDefault.add(defaultEntry);
                        for (IInterpreterInfo info : interpretersInfo) {
                            interpretersWithDefault.add(info.getName());
                        }
                        interpretersChoice.setItems(interpretersWithDefault.toArray(new String[0]));

                        interpretersChoice.setVisible(true);
                        interpreterNoteText.setText("<a>Click here to configure an interpreter not listed.</a>");
                        interpretersChoice.setText(defaultEntry);

                    } else {
                        interpretersChoice.setVisible(false);
                        interpreterNoteText.setText(INTERPRETER_NOT_CONFIGURED_MSG);

                    }
                    //config which preferences page should be opened!
                    String configPageId = InterpreterConfigHelpers.getConfigPageIdFromInterpreterType(interpreterType);
                    idToConfig[0] = configPageId;
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
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String interpreterName = getProjectInterpreter();
                    if (interpreterName != null) {
                        IInterpreterManager interpreterManager = getInterpreterManager();
                        int interpreterType = interpreterManager.getInterpreterType();
                        if (interpreterType == IPythonNature.INTERPRETER_TYPE_PYTHON) {
                            // Let's see if it's from pipenv
                            final String projectLocation = getProjectLocation.call();
                            int USE_PIPENV = 1;
                            int openQuestionWithChoices = PyDialogHelpers.openQuestionWithChoices(
                                    "How to config interpreter?",
                                    "How would you like to add a new interpreter?",
                                    "Open interpreter preferences page", // 0
                                    "Create using pipenv"); // 1

                            if (openQuestionWithChoices == -1) { // Cancelled.
                                return;
                            }
                            if (openQuestionWithChoices == USE_PIPENV) {
                                IInterpreterInfo[] interpreterInfos = interpreterManager.getInterpreterInfos();
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                PrintWriter logger = new PrintWriter(out);
                                Map<String, IInterpreterInfo> nameToInfo = new HashMap<String, IInterpreterInfo>();
                                for (IInterpreterInfo info : interpreterInfos) {
                                    nameToInfo.put(info.getName(), info);
                                }
                                try {
                                    ObtainInterpreterInfoOperation operation = InterpreterConfigHelpers
                                            .createPipenvInterpreter(interpreterInfos,
                                                    interpreterNoteText.getShell(), logger, nameToInfo,
                                                    projectLocation, interpreterManager);
                                    if (operation != null) {
                                        interpreterNoteText.setText("Configuration in progress...");
                                        AutoConfigMaker.applyOperation(createOnJobComplete(), operation,
                                                interpreterManager,
                                                null, false);
                                    }
                                } catch (Exception e1) {
                                    Log.log(e1);
                                }
                                String s = out.toString();
                                if (!s.isEmpty()) {
                                    Log.logInfo(s);
                                }
                                return;
                            }

                            // Continue to show interpreters page.
                        }

                        // Default is just showing preferences editor.

                        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
                                null, idToConfig[0], null, null);
                        AbstractInterpreterPreferencesPage selectedPage = (AbstractInterpreterPreferencesPage) dialog
                                .getSelectedPage();
                        selectedPage.setDefaultProjectLocation(getProjectLocation.call());
                        dialog.open();
                        //just to re-update it again
                        updateInterpretersAndDefaultInCombo();

                    } else {
                        // None there yet...
                        MessageDialog mdialog = new MessageDialog(null, "Configure interpreter", null,
                                "How would you like to configure the interpreter?", MessageDialog.QUESTION,
                                InterpreterConfigHelpers.CONFIG_NAMES_FOR_FIRST_INTERPRETER, 0);
                        int open = mdialog.open();
                        if (open == InterpreterConfigHelpers.CONFIG_MANUAL) {
                            PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null,
                                    idToConfig[0], null, null);
                            dialog.open();
                            //just to re-update it again
                            updateInterpretersAndDefaultInCombo();
                        } else if (open != PyDialogHelpers.INTERPRETER_CANCEL_CONFIG) {
                            //auto-config
                            interpreterNoteText.setText("Configuration in progress...");

                            InterpreterType interpreterType = getInterpreterType();

                            boolean advanced = open == InterpreterConfigHelpers.CONFIG_ADV_AUTO;
                            AutoConfigMaker a = new AutoConfigMaker(interpreterType, advanced, null, null);
                            if (a.autoConfigSingleApply(createOnJobComplete())) {
                                triggerCallback();
                            } else {
                                updateInterpretersAndDefaultInCombo();
                            }
                        }
                    }
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                }
            });

            //Additional grammar validations
            Composite composite = new Composite(topComp, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));

            labelAdditionalGrammarsSelected = new Label(composite, SWT.NONE);

            labelAdditionalGrammarsSelected.setText(ADDITIONAL_SYNTAX_NO_SELECTED);

            gd = new GridData(GridData.FILL_HORIZONTAL);
            labelAdditionalGrammarsSelected.setLayoutData(gd);

            Button button = new Button(composite, SWT.PUSH);
            button.setText("...");
            gd = new GridData();
            button.setLayoutData(gd);
            button.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    List<String> grammarversionsrep = new ArrayList<>(IGrammarVersionProvider.grammarVersionsRep);
                    final String NO_VALIDATION = "No additional syntax validation";
                    grammarversionsrep.add(NO_VALIDATION);
                    String[] selected = SelectNDialog.selectMulti(grammarversionsrep,
                            new LabelProvider(),
                            "Select additional grammars for syntax validation");
                    if (ArrayUtils.contains(selected, NO_VALIDATION)) {
                        selected = null;
                    }
                    if (selected == null || selected.length == 0) {
                        labelAdditionalGrammarsSelected.setText(ADDITIONAL_SYNTAX_NO_SELECTED);
                    } else {
                        labelAdditionalGrammarsSelected
                                .setText(ADDITIONAL_SYNTAX_PREFIX + StringUtils.join(", ", selected));
                    }
                }
            });

            gd = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gd);

            return topComp;
        }

        private void triggerCallback() {
            if (onSelectionChanged != null) {
                try {
                    onSelectionChanged.call();
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
                return "python " + getGrammarVersionSelectedFromCombo();
            }
            if (radioJy.getSelection()) {
                return "jython " + getGrammarVersionSelectedFromCombo();
            }
            if (radioIron.getSelection()) {
                return "ironpython " + getGrammarVersionSelectedFromCombo();
            }
            throw new RuntimeException("Some radio must be selected");
        }

        private String getGrammarVersionSelectedFromCombo() {
            String ret = comboGrammarVersion.getText();
            ret = numberFromUi(ret);
            return ret;
        }

        public String getProjectInterpreter() {
            if (INTERPRETER_NOT_CONFIGURED_MSG.equals(interpreterNoteText.getText())) {
                return null;
            }
            String ret = interpretersChoice.getText();
            if (ret.startsWith(DEFAULT_PREFIX)) {
                return IPythonNature.DEFAULT_INTERPRETER;
            }
            return ret;
        }

        public void setDefaultSelection() {
            radioPy.setSelection(true);
            comboGrammarVersion.setText(numberToUi(IPythonNature.Versions.INTERPRETER_VERSION));
            //Just to update things
            this.selectionListener.widgetSelected(null);
        }

        public String getAdditionalGrammarValidation() {
            String text = labelAdditionalGrammarsSelected.getText();
            if (text.equals(ADDITIONAL_SYNTAX_NO_SELECTED)) {
                return null;
            }
            return text.substring(ADDITIONAL_SYNTAX_PREFIX.length());
        }

        private IInterpreterManager getInterpreterManager() {
            IInterpreterManager interpreterManager;

            if (radioJy.getSelection()) {
                interpreterManager = InterpreterManagersAPI.getJythonInterpreterManager();

            } else if (radioIron.getSelection()) {
                interpreterManager = InterpreterManagersAPI.getIronpythonInterpreterManager();

            } else {
                interpreterManager = InterpreterManagersAPI.getPythonInterpreterManager();
            }
            return interpreterManager;
        }

        private InterpreterType getInterpreterType() {
            InterpreterType interpreterType;
            if (radioJy.getSelection()) {
                interpreterType = InterpreterType.JYTHON;
            } else if (radioIron.getSelection()) {
                interpreterType = InterpreterType.IRONPYTHON;
            } else {
                interpreterType = InterpreterType.PYTHON;
            }
            return interpreterType;
        }

        private void updateInterpretersAndDefaultInCombo() {
            selectionListener.widgetSelected(null);
        }

        private JobChangeAdapter createOnJobComplete() {
            JobChangeAdapter onJobComplete = new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    //Update the display when the configuration has ended.
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            //Only update if the page is still there.
                            //If something is disposed, it has been closed.
                            if (!interpreterNoteText.isDisposed()) {
                                updateInterpretersAndDefaultInCombo();
                            }
                        }
                    });
                };
            };
            return onJobComplete;
        }

    }

    static String numberFromUi(String s) {
        if ("Same as interpreter".equals(s)) {
            return IPythonNature.Versions.INTERPRETER_VERSION;
        }
        if ("3.0 - 3.5".equals(s)) {
            s = "3.0";
        }
        return s;
    }

    static String numberToUi(String s) {
        if (IPythonNature.Versions.INTERPRETER_VERSION.equals(s)) {
            return "Same as interpreter";
        }
        if ("3.0".equals(s)) {
            s = "3.0 - 3.5";
        }
        return s;
    }

    /**
     * The element.
     */
    public IAdaptable element;
    public ProjectInterpreterAndGrammarConfig projectConfig = new ProjectInterpreterAndGrammarConfig(
            () -> {
                return getProject().getLocation().toOSString();
            });

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
        return getElement().getAdapter(IProject.class);
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
            String version = pythonNature.getVersion(false);
            if (version.startsWith(IPythonNature.Versions.PYTHON_PREFIX)) {
                projectConfig.radioPy.setSelection(true);

            } else if (version.startsWith(IPythonNature.Versions.JYTHON_PREFIX)) {
                projectConfig.radioJy.setSelection(true);

            } else if (version.startsWith(IPythonNature.Versions.IRONYTHON_PREFIX)) {
                projectConfig.radioIron.setSelection(true);
            } else {
                Log.log("Could not recognize version: " + version + " for project: " + getProject());
            }

            //We must set the grammar version too (that's from a string in the format "Python 2.4" or "Python interpreter" and we only want
            //the version).
            String v = StringUtils.split(version, ' ').get(1);
            projectConfig.comboGrammarVersion.setText(numberToUi(v));

            //Update interpreter
            projectConfig.selectionListener.widgetSelected(null);
            String configuredInterpreter = pythonNature.getProjectInterpreterName();
            if (configuredInterpreter != null) {
                projectConfig.interpretersChoice.setText(configuredInterpreter);
            }

            AdditionalGrammarVersionsToCheck additionalGrammarVersions = null;
            try {
                additionalGrammarVersions = pythonNature.getAdditionalGrammarVersions();
            } catch (MisconfigurationException e) {

            }
            FastStringBuffer buf = new FastStringBuffer();
            if (additionalGrammarVersions != null) {
                Set<Integer> grammarVersions = additionalGrammarVersions.getGrammarVersions();
                if (grammarVersions != null) {
                    for (Integer grammarV : new TreeSet<Integer>(grammarVersions)) {
                        String rep = IGrammarVersionProvider.grammarVersionToRep.get(grammarV);
                        if (rep != null) {
                            if (buf.length() > 0) {
                                buf.append(", ");
                            }
                            buf.append(rep);
                        }
                    }
                }
            }
            if (buf.length() == 0) {
                projectConfig.labelAdditionalGrammarsSelected.setText(ADDITIONAL_SYNTAX_NO_SELECTED);
            } else {
                projectConfig.labelAdditionalGrammarsSelected.setText(ADDITIONAL_SYNTAX_PREFIX + buf.toString());
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
                pythonNature.setAdditionalGrammarValidation(projectConfig.getAdditionalGrammarValidation());
            } catch (CoreException e) {
                Log.log(e);
            }
        }
        //re-enable "configure interpreter" dialogs
        PyDialogHelpers.enableAskInterpreterStep(true);
        return true;
    }
}