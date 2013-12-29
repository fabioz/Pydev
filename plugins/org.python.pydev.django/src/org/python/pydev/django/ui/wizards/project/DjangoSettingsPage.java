/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.ui.wizards.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.runners.UniversalRunner;
import org.python.pydev.runners.UniversalRunner.AbstractRunner;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.wizards.project.IWizardNewProjectNameAndLocationPage;

@SuppressWarnings("serial")
public class DjangoSettingsPage extends WizardPage {

    public static final String CPYTHON = "cpython";
    public static final String JYTHON = "jython";

    public static String DJANGO_14 = "1.4 or later";
    public static String DJANGO_12_OR_13 = "1.2 or 1.3";
    public static String DJANGO_11_OR_EARLIER = "1.1 or earlier";

    protected static final String GET_DJANGO_VERSION = "import django;print(django.get_version());";

    /**
     * The default version to be used.
     */
    private String defaultVersion = DJANGO_14;

    private void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    static final ArrayList<String> DJANGO_VERSIONS = new ArrayList<String>() {
        {
            add(DJANGO_14);
            add(DJANGO_12_OR_13);
            add(DJANGO_11_OR_EARLIER);
        }
    };

    static final Map<String, List<String>> DB_ENGINES = new HashMap<String, List<String>>() {
        {
            put(CPYTHON, new ArrayList<String>() {
                {
                    add("sqlite3");
                    add("postgresql_psycopg2");
                    add("mysql");
                    add("oracle");
                    add("other (just type in combo)");
                }
            });
            put(JYTHON, new ArrayList<String>() {
                {
                    add("doj.backends.zxjdbc.sqlite3");
                    add("doj.backends.zxjdbc.postgresql");
                    add("doj.backends.zxjdbc.mysql");
                    add("doj.backends.zxjdbc.oracle");
                    add("other (just type in combo)");
                }
            });
        }
    };

    private static final int SIZING_TEXT_FIELD_WIDTH = 250;

    private Combo djVersionCombo;
    private Combo engineCombo;
    private Text nameText;
    private Text hostText;
    private Text portText;
    private Text userText;
    private Text passText;
    private ICallback0<IWizardNewProjectNameAndLocationPage> projectPageCallback;
    private String previousProjectType = "";
    private String previousProjectInterpreter = "";

    public DjangoSettingsPage(String pageName, ICallback0<IWizardNewProjectNameAndLocationPage> projectPage) {
        super(pageName);
        this.projectPageCallback = projectPage;
        setTitle("Django Settings");
        setDescription("Basic Django Settings");
    }

    private Label newLabel(Composite parent, String label) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(label);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return l;
    }

    private Text newText(Composite parent) {
        Text t = new Text(parent, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = SIZING_TEXT_FIELD_WIDTH;
        t.setLayoutData(gd);
        return t;
    }

    @Override
    public void setPreviousPage(IWizardPage page) {
        super.setPreviousPage(page);
        final IWizardNewProjectNameAndLocationPage projectPage = projectPageCallback.call();
        final String projectType = projectPage.getProjectType();

        if (djVersionCombo.getItemCount() == 0) {
            //fill it only if it's still not properly filled
            djVersionCombo.removeAll();
            for (String version : DJANGO_VERSIONS) {
                djVersionCombo.add(version);
            }

        }

        final String projectInterpreter = projectPage.getProjectInterpreter();
        if (!projectType.equals(previousProjectType) || !projectInterpreter.equals(previousProjectInterpreter)) {
            discoverDefaultVersion(projectType, projectInterpreter);
        }
        djVersionCombo.setText(defaultVersion);

        if (!projectType.equals(previousProjectType)) {
            List<String> engines = DB_ENGINES.get(projectType.startsWith("jython") ? DjangoSettingsPage.JYTHON
                    : DjangoSettingsPage.CPYTHON);
            engineCombo.removeAll();
            for (String engine : engines) {
                engineCombo.add(engine);
            }

            engineCombo.setText(engines.get(0));
        }

        this.previousProjectType = projectType;
        this.previousProjectInterpreter = projectInterpreter;

        //Always update the sqlite path if needed.
        updateSqlitePathIfNeeded(projectPage);
    }

    protected void discoverDefaultVersion(final String projectType, final String projectInterpreter) {
        defaultVersion = DJANGO_14; //It should be discovered below, but if not found for some reason, this will be the default.

        SystemPythonNature nature;
        try {
            final int interpreterType = PythonNature.getInterpreterTypeFromVersion(projectType);
            IInterpreterManager interpreterManagerFromType = PydevPlugin.getInterpreterManagerFromType(interpreterType);
            IInterpreterInfo interpreterInfo;
            if (IPythonNature.DEFAULT_INTERPRETER.equals(projectInterpreter)) {
                interpreterInfo = interpreterManagerFromType.getDefaultInterpreterInfo(false);

            } else {
                interpreterInfo = interpreterManagerFromType.getInterpreterInfo(projectInterpreter, null);

            }
            nature = new SystemPythonNature(interpreterManagerFromType, interpreterInfo);
            AbstractRunner runner = UniversalRunner.getRunner(nature);

            Tuple<String, String> output = runner.runCodeAndGetOutput(GET_DJANGO_VERSION, new String[] {}, null,
                    new NullProgressMonitor());

            String err = output.o2.trim();
            String out = output.o1.trim();
            if (err.length() > 0) {
                Log.log("Error attempting to determine Django version: " + err);

            } else {
                //System.out.println("Gotten version: "+out);
                if (out.startsWith("0.")) {
                    setDefaultVersion(DjangoSettingsPage.DJANGO_11_OR_EARLIER);

                } else if (out.startsWith("1.")) {
                    out = out.substring(2);
                    if (out.startsWith("0") || out.startsWith("1")) {
                        setDefaultVersion(DjangoSettingsPage.DJANGO_11_OR_EARLIER);

                    } else if (out.startsWith("2") || out.startsWith("3")) {
                        setDefaultVersion(DjangoSettingsPage.DJANGO_12_OR_13);

                    } else {
                        //Later version
                        setDefaultVersion(DjangoSettingsPage.DJANGO_14);
                    }
                }
            }

        } catch (Exception e) {
            Log.log("Unable to determine Django version.", e);
        }
    }

    @SuppressWarnings("unused")
    public void createControl(Composite parent) {
        Composite topComp = new Composite(parent, SWT.NONE);
        GridLayout innerLayout = new GridLayout();
        innerLayout.numColumns = 1;
        innerLayout.marginHeight = 0;
        innerLayout.marginWidth = 0;
        topComp.setLayout(innerLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        topComp.setLayoutData(gd);

        //General Settings
        Group general_grp = new Group(topComp, SWT.NONE);
        general_grp.setText("General");
        GridLayout general_layout = new GridLayout();
        general_layout.horizontalSpacing = 8;
        general_layout.numColumns = 2;
        general_grp.setLayout(general_layout);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        general_grp.setLayoutData(gd);

        Label versionLabel = newLabel(general_grp, "Django version");

        djVersionCombo = new Combo(general_grp, SWT.READ_ONLY);

        gd = new GridData(GridData.FILL_HORIZONTAL);
        djVersionCombo.setLayoutData(gd);

        //Database Settings
        Group group = new Group(topComp, SWT.NONE);
        group.setText("Database settings");
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 8;
        layout.numColumns = 2;
        group.setLayout(layout);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);

        // Database Engine
        Label engineLabel = newLabel(group, "Database &Engine");

        engineCombo = new Combo(group, 0);
        final IWizardNewProjectNameAndLocationPage projectPage = projectPageCallback.call();

        engineCombo.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                updateSqlitePathIfNeeded(projectPage);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        gd = new GridData(GridData.FILL_HORIZONTAL);
        engineCombo.setLayoutData(gd);

        // Database Name
        Label nameLabel = newLabel(group, "Database &Name");
        nameText = newText(group);
        // Database Host
        Label hostLabel = newLabel(group, "Database &Host");
        hostText = newText(group);
        // Database Port
        Label portLabel = newLabel(group, "Database P&ort");
        portText = newText(group);

        // Database User
        Label userLabel = newLabel(group, "&Username");
        userText = newText(group);
        // Database Pass
        Label passLabel = newLabel(group, "&Password");
        passText = newText(group);
        passText.setEchoChar('*');
        setErrorMessage(null);
        setMessage(null);
        setControl(topComp);
    }

    public static class DjangoSettings {
        public String djangoVersion;
        public String databaseEngine;
        public String databaseName;
        public String databaseHost;
        public String databasePort;
        public String databaseUser;
        public String databasePassword;

    }

    public DjangoSettings getSettings() {
        DjangoSettings s = new DjangoSettings();
        //make it suitable to be written
        s.djangoVersion = djVersionCombo.getText();
        s.databaseEngine = escapeSlashes(engineCombo.getText());
        s.databaseName = escapeSlashes(nameText.getText());
        s.databaseHost = escapeSlashes(hostText.getText());
        s.databasePort = escapeSlashes(portText.getText());
        s.databaseUser = escapeSlashes(userText.getText());
        s.databasePassword = escapeSlashes(passText.getText());
        return s;
    }

    public void updateSqlitePathIfNeeded(final IWizardNewProjectNameAndLocationPage projectPage) {
        String selection = engineCombo.getText();
        if (selection.endsWith("sqlite3")) {
            String projectName = projectPage.getProjectName();
            IPath base = projectPage.getLocationPath().append(projectName);
            int sourceFolderConfigurationStyle = projectPage.getSourceFolderConfigurationStyle();

            switch (sourceFolderConfigurationStyle) {
                case IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER:
                case IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_NO_PYTHONPATH:
                    break;
                default:
                    base = base.append("src");
            }

            nameText.setText(base.append("sqlite.db").toOSString());
        }
    }

    private String escapeSlashes(String text) {
        return StringUtils.replaceAll(text, "\\", "\\\\\\\\");
    }
}
