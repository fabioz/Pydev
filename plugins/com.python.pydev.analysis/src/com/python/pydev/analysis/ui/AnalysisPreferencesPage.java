/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.utils.LabelFieldEditor;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.AnalysisPreferenceInitializer;
import com.python.pydev.analysis.IAnalysisPreferences;

public class AnalysisPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

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
        Composite p = getFieldEditorParent();
        
        addField(new LabelFieldEditor("Analysis_pref_note", "NOTE: Any file with the comment below will not be analyzed.\n\n#@PydevCodeAnalysisIgnore\n\nOptions:\n\n", p));

        TabFolder tabFolder = new TabFolder(p, SWT.NONE);
        tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        p = createTab(tabFolder, "Options");
        String[][] whenAnalyze = new String[][]{
                {"Only on save"  , String.valueOf(IAnalysisPreferences.ANALYZE_ON_SAVE)},
                {"On any successful parse", String.valueOf(IAnalysisPreferences.ANALYZE_ON_SUCCESFUL_PARSE)}
        };
        addField(new BooleanFieldEditor(AnalysisPreferenceInitializer.DO_CODE_ANALYSIS, "Do code analysis?", BooleanFieldEditor.DEFAULT,p));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.WHEN_ANALYZE, "When do we analyze?", 2,whenAnalyze,p, true));

        
        String[][] values = new String[][]{
                {"Error"  , String.valueOf(IMarker.SEVERITY_ERROR)},
                {"Warning", String.valueOf(IMarker.SEVERITY_WARNING)},
                {"Ignore" , String.valueOf(IMarker.SEVERITY_INFO)}
        };

        
        p = createTab(tabFolder, "Unused");
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_IMPORT, "Unused import", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_WILD_IMPORT, "Unused wild import", 3,values,p, true));
        addField(new StringFieldEditor(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_IMPORT, "Don't report unused imports in modules named: (comma separated)",p ));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_PARAMETER, "Unused parameter", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNUSED_VARIABLE, "Unused variable", 3,values,p, true));
        addField(new StringFieldEditor(AnalysisPreferenceInitializer.NAMES_TO_IGNORE_UNUSED_VARIABLE, "Don't report unused variable if name starts with: (comma separated)",p ){
            @Override
            public int getNumberOfControls() {
                return 1;
            }
        });

        
        p = createTab(tabFolder, "Undefined");
        addField(new StringFieldEditor(AnalysisPreferenceInitializer.NAMES_TO_CONSIDER_GLOBALS, "Consider the following names as globals: (comma separated)",p ));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_VARIABLE, "Undefined variable", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNDEFINED_IMPORT_VARIABLE, "Undefined variable from import" , 3,values,p, true));
        
        p = createTab(tabFolder, "Imports");
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_REIMPORT, "Import redefinition", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_UNRESOLVED_IMPORT, "Import not found", 3,values,p, true));
        
        p = createTab(tabFolder, "Others");
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_DUPLICATED_SIGNATURE, "Duplicated signature", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_NO_SELF, "'self' not specified in class method", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_NO_EFFECT_STMT, "Statement has no effect", 3,values,p, true));
        addField(new RadioGroupFieldEditor(AnalysisPreferenceInitializer.SEVERITY_INDENTATION_PROBLEM, "Indentation problems and mixing of tabs/spaces", 3,values,p, true));

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

    public void init(IWorkbench workbench) {
    }

}
