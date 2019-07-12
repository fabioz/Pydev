/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.python.pydev.analysis.AnalysisPreferenceInitializer;
import com.python.pydev.analysis.AnalysisUiPlugin;

/**
 * Preferences page indicating auto-import preferences
 *
 * @author Fabio
 */
public class AutoImportsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public AutoImportsPreferencesPage() {
        super(FLAT);
        setDescription("Auto Imports");
        setPreferenceStore(null);
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return AnalysisUiPlugin.getPreferenceStore();
    }

    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(AnalysisPreferenceInitializer.DO_AUTO_IMPORT, "Do auto import?",
                BooleanFieldEditor.DEFAULT, p));

        addField(new BooleanFieldEditor(AnalysisPreferenceInitializer.DO_IGNORE_IMPORTS_STARTING_WITH_UNDER,
                "Ignore last modules starting with '_' when doing auto-import/quick fix?", BooleanFieldEditor.DEFAULT,
                p));

        addField(new BooleanFieldEditor(AnalysisPreferenceInitializer.DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS,
                "Do auto import on organize imports (Ctrl+Shift+O)?", BooleanFieldEditor.DEFAULT, p));
    }

}
