/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ui;

import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.AnalysisPreferenceInitializer;

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

    public void init(IWorkbench workbench) {
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return getPlugin().getPreferenceStore();
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

    /**
     * 
     * @param doIgnoreImportsStartingWithUnder: result from the doIgnoreImportsStartingWithUnder() method
     * (but should be called before so that it does not get into a loop which call this method as that method
     * may be slow).
     */
    public static String removeImportsStartingWithUnderIfNeeded(String declPackageWithoutInit, FastStringBuffer buf,
            boolean doIgnoreImportsStartingWithUnder) {
        if (doIgnoreImportsStartingWithUnder) {
            List<String> splitted = StringUtils.dotSplit(declPackageWithoutInit);

            boolean foundStartingWithoutUnder = false;
            buf.clear();
            int len = splitted.size();
            for (int i = len - 1; i >= 0; i--) {
                String s = splitted.get(i);
                if (!foundStartingWithoutUnder) {
                    if (s.charAt(0) == '_') {
                        continue;
                    }
                    foundStartingWithoutUnder = true;
                }
                buf.insert(0, s);
                if (i != 0) {
                    buf.insert(0, '.');
                }
            }
            declPackageWithoutInit = buf.toString();
        }
        return declPackageWithoutInit;
    }

    private static AnalysisPlugin getPlugin() {
        return AnalysisPlugin.getDefault();
    }

    public static boolean TESTS_DO_AUTO_IMPORT = true;

    public static boolean doAutoImport() {
        if (SharedCorePlugin.inTestMode()) {
            return TESTS_DO_AUTO_IMPORT;
        }

        AnalysisPlugin plugin = getPlugin();
        return plugin.getPreferenceStore().getBoolean(AnalysisPreferenceInitializer.DO_AUTO_IMPORT);
    }

    public static boolean TESTS_DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS = true;

    public static boolean doAutoImportOnOrganizeImports() {
        if (SharedCorePlugin.inTestMode()) {
            return TESTS_DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS;
        }
        AnalysisPlugin plugin = getPlugin();
        return plugin.getPreferenceStore().getBoolean(
                AnalysisPreferenceInitializer.DO_AUTO_IMPORT_ON_ORGANIZE_IMPORTS);
    }

    public static boolean TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER = false;

    public static boolean doIgnoreImportsStartingWithUnder() {
        if (SharedCorePlugin.inTestMode()) {
            return TESTS_DO_IGNORE_IMPORT_STARTING_WITH_UNDER;
        }

        AnalysisPlugin plugin = getPlugin();
        return plugin.getPreferenceStore().getBoolean(
                AnalysisPreferenceInitializer.DO_IGNORE_IMPORTS_STARTING_WITH_UNDER);
    }

}
