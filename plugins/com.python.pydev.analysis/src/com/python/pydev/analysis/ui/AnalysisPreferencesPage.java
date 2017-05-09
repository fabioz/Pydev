/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;
import org.python.pydev.shared_ui.field_editors.RadioGroupFieldEditor;
import org.python.pydev.shared_ui.field_editors.ScopedFieldEditorPreferencePage;
import org.python.pydev.shared_ui.field_editors.ScopedPreferencesFieldEditor;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.AnalysisPreferenceInitializer;
import com.python.pydev.analysis.PyAnalysisScopedPreferences;

public class AnalysisPreferencesPage extends ScopedFieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_PEP8_CONSOLE = "USE_PEP8_CONSOLE";
    public static final boolean DEFAULT_USE_PEP8_CONSOLE = false;
    public static final String PEP8_COMMAND_LINE = "PEP8_IGNORE_WARNINGS";
    public static final String PEP8_USE_SYSTEM = "PEP8_USE_SYSTEM";
    public static final boolean DEFAULT_PEP8_USE_SYSTEM = false;

    //Disabled because we're running in a thread now.
    public static final boolean SHOW_IN_PEP8_FEATURE_ENABLED = false;

    public AnalysisPreferencesPage() {
        super(FLAT);
        setDescription("PyDev Analysis");
        setPreferenceStore(null);
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return AnalysisPlugin.getDefault().getPreferenceStore();
    }

    @Override
    public void createFieldEditors() {
        final Composite initialParent = getFieldEditorParent();
        Composite p = initialParent;

        addField(new LabelFieldEditor(
                "Analysis_pref_note",
                "NOTE: Any file with the comment below will not be analyzed.\n\n#@PydevCodeAnalysisIgnore\n\n",
                p));

        BooleanFieldEditor field = new BooleanFieldEditor(AnalysisPreferenceInitializer.DO_CODE_ANALYSIS,
                "Do code analysis?",
                BooleanFieldEditor.DEFAULT, p);
        addField(field);

        TabFolder tabFolder = new TabFolder(p, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));

        String[][] values = new String[][] {
                { "Error", String.valueOf(IMarker.SEVERITY_ERROR) },
                { "Warning", String.valueOf(IMarker.SEVERITY_WARNING) },
                { "Info", String.valueOf(IMarker.SEVERITY_INFO) },
                { "Ignore", String.valueOf(-1) }
        };

        p = createTab(tabFolder, "Unused");
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_IMPORT, "Unused import", 4,
                values, p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_WILD_IMPORT,
                "Unused wild import", 4, values, p, true));
        addField(new StringFieldEditor(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_IMPORT,
                "Don't report unused imports in modules named: (comma separated)", p));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_PARAMETER, "Unused parameter",
                4, values, p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_VARIABLE, "Unused variable",
                4, values, p, true));
        addField(new StringFieldEditor(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_VARIABLE,
                "Don't report unused variable if name starts with: (comma separated)", p) {
            @Override
            public int getNumberOfControls() {
                return 1;
            }
        });

        p = createTab(tabFolder, "Undefined");
        addField(new StringFieldEditor(AnalysisPreferenceInitializer.NAMES_TO_CONSIDER_GLOBALS,
                "Consider the following names as globals: (comma separated)", p));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_VARIABLE,
                "Undefined variable", 4, values, p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_IMPORT_VARIABLE,
                "Undefined variable from import", 4, values, p, true));

        p = createTab(tabFolder, "Imports");
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_REIMPORT, "Import redefinition", 4,
                values, p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNRESOLVED_IMPORT,
                "Import not found", 4, values, p, true));

        p = createTab(tabFolder, "Others");
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_DUPLICATED_SIGNATURE,
                "Duplicated signature", 4, values, p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_NO_SELF,
                "'self' not specified in class method", 4, values, p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_NO_EFFECT_STMT,
                "Statement has no effect", 4, values, p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_INDENTATION_PROBLEM,
                "Indentation problems and mixing of tabs/spaces", 4, values, p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_ASSIGNMENT_TO_BUILT_IN_SYMBOL,
                "Redefinition of builtin symbols", 4, values, p, true));
        //TODO: Add ARGUMENTS_MISMATCH again later on
        //addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_ARGUMENTS_MISMATCH, "Arguments mismatch", 4,values,p, true));

        p = createTab(tabFolder, "pycodestyle.py (pep8)");

        String[][] pep8values = new String[][] { { "Error", String.valueOf(IMarker.SEVERITY_ERROR) },
                { "Warning", String.valueOf(IMarker.SEVERITY_WARNING) },
                { "Info", String.valueOf(IMarker.SEVERITY_INFO) },
                { "Don't run", String.valueOf(-1) } };

        addField(
                new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_PEP8, "Pep8", 4, pep8values, p, true) {
                    @Override
                    protected void doFillIntoGrid(Composite parent, int numColumns) {
                        super.doFillIntoGrid(parent, 4);
                        adjustForNumColumns(4);
                    }
                });
        if (SHOW_IN_PEP8_FEATURE_ENABLED) {
            addField(new BooleanFieldEditor(USE_PEP8_CONSOLE, "Redirect pycodestyle output to console?", p) {
                @Override
                protected void doFillIntoGrid(Composite parent, int numColumns) {
                    super.doFillIntoGrid(parent, 4);
                    adjustForNumColumns(4);
                }
            });
        }
        addField(new BooleanFieldEditor(PEP8_USE_SYSTEM, "Use system interpreter (may be faster than internal Jython)",
                p) {
            @Override
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                super.doFillIntoGrid(parent, 4);
                adjustForNumColumns(4);
            }
        });

        addField(new LinkFieldEditor(PEP8_COMMAND_LINE,
                "Additional command line arguments (i.e.: --ignore=E5,W391). See <a>pycodestyle docs</a> for details.",
                p,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        Program.launch("https://pycodestyle.readthedocs.io/");
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                }) {

            @Override
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                numColumns = 4;
                Link linkControl = getLinkControl(parent);
                Object layoutData = linkControl.getLayoutData();
                if (layoutData == null) {
                    layoutData = new GridData();
                    linkControl.setLayoutData(layoutData);
                }
                ((GridData) layoutData).horizontalSpan = numColumns;
                adjustForNumColumns(4);
            }
        });

        addField(new StringFieldEditor(PEP8_COMMAND_LINE, "Arguments: ", p) {
            @Override
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                super.doFillIntoGrid(parent, 4);
                adjustForNumColumns(4);
            }
        });

        addField(new ScopedPreferencesFieldEditor(initialParent, PyAnalysisScopedPreferences.ANALYSIS_SCOPE, this));
    }

    /**
     * @param tabFolder
     * @return
     */
    private Composite createTab(TabFolder tabFolder, String tabText) {
        TabItem item1 = new TabItem(tabFolder, SWT.NULL);
        item1.setText(tabText);
        Composite p1 = new Composite(tabFolder, SWT.NONE);
        p1.setLayoutData(new GridData(GridData.FILL_BOTH));
        item1.setControl(p1);
        return p1;
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    public static String[] getPep8CommandLine(IAdaptable projectAdaptable) {
        return PythonRunnerConfig.parseStringIntoList(getPep8CommandLineAsStr(projectAdaptable));
    }

    public static String getPep8CommandLineAsStr(IAdaptable projectAdaptable) {
        return PyAnalysisScopedPreferences.getString(PEP8_COMMAND_LINE, projectAdaptable);
    }

    public static boolean useConsole(IAdaptable projectAdaptable) {
        if (SHOW_IN_PEP8_FEATURE_ENABLED) {
            return PyAnalysisScopedPreferences.getBoolean(USE_PEP8_CONSOLE, projectAdaptable);
        }
        return false;
    }

    public static boolean useSystemInterpreter(IAdaptable projectAdaptable) {
        return PyAnalysisScopedPreferences.getBoolean(PEP8_USE_SYSTEM, projectAdaptable);
    }
}
