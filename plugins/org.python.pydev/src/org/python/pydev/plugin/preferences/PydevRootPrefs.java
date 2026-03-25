/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.package_manager.PoetryPackageManager;
import org.python.pydev.core.package_manager.UVPackageManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.CheckDefaultPreferencesDialog.CheckInfo;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.dialogs.DialogHelpers;
import org.python.pydev.shared_ui.field_editors.ButtonFieldEditor;

public class PydevRootPrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String CHECK_PREFERRED_PYDEV_SETTINGS = "CHECK_PREFERRED_PYDEV_SETTINGS";
    public static final boolean DEFAULT_CHECK_PREFERRED_PYDEV_SETTINGS = true;
    public static final String POETRY_BIN = "POETRY_BIN";
    public static final String UV_BIN = "UV_BIN";

    public PydevRootPrefs() {
        setDescription(StringUtils.format("PyDev version: %s",
                PydevPlugin.getVersion()));
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        final BooleanFieldEditor booleanField = new BooleanFieldEditor(CHECK_PREFERRED_PYDEV_SETTINGS,
                "Check preferred Eclipse settings for PyDev on editor open", p);
        addField(booleanField);

        addField(new ButtonFieldEditor("__UNUSED__", "Check preferred settings now.", p, new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                CheckInfo[] missing = CheckInfo.getMissing();
                if (missing.length == 0) {
                    DialogHelpers.openInfo("Checked",
                            "Preferences in Eclipse already match preferred PyDev settings.");
                    return;
                }
                Shell shell = getShell();
                CheckDefaultPreferencesDialog dialog = new CheckDefaultPreferencesDialog(shell, missing);
                dialog.open();
                booleanField.load();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        }));
        addField(new FileFieldEditor(PoetryPackageManager.PREFS_NAME, "Poetry path", p));
        addField(new FileFieldEditor(UVPackageManager.PREFS_NAME, "UV path", p));
    }

    public static void setCheckPreferredPydevSettings(boolean b) {
        PydevPlugin.getDefault().getPreferenceStore().setValue(CHECK_PREFERRED_PYDEV_SETTINGS, b);
    }

    public static boolean getCheckPreferredPydevSettings() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(CHECK_PREFERRED_PYDEV_SETTINGS);
    }

    /**
     * Gets the default poetry install location.
     * 
     * @return The path to the poetry bin
     */
    public static String getDefaultPoetryBinPreference() {
        String path;
        if (PlatformUtils.isWindowsPlatform()) {
            path = System.getenv("APPDATA") + "\\poetry";
        } else if (PlatformUtils.isMacOsPlatform()) {
            path = System.getenv("HOME") + "/Library/Application Support/pypoetry";
        } else {
            path = System.getenv("HOME") + "/.local/share/pypoetry/venv/bin/poetry";
        }
        return path;
    }

    /**
     * Gets the default UV install location.
     * 
     * @return The path to the uv bin
     */
    public static String getDefaultUVBinPreference() {
        String path;
        if (PlatformUtils.isWindowsPlatform()) {
            path = System.getenv("APPDATA") + "\\bin\\uv";
        } else {
            path = System.getenv("HOME") + "/.local/bin/uv";
        }
        return path;
    }

}
