/******************************************************************************
* Copyright (C) 2013  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.newconsole.prefs;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;

public class InteractiveConsoleUMDPrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String PREFERENCES_ID = "org.python.pydev.debug.newconsole.prefs.InteractiveConsoleUMDPrefs";

    public InteractiveConsoleUMDPrefs() {
        super(FLAT);
    }

    public void init(IWorkbench workbench) {
        setDescription("PyDev User Module Deleter (UMD) preferences.\n\n" +
                "UMD forces Python to reload modules which were " +
                "imported when executing a script in the " +
                "external console with the 'runfile' function.");
        setPreferenceStore(PydevDebugPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_ENABLED,
                "Enable UMD", p));
        addField(new LabelFieldEditor("LabelFieldEditor", "", p));

        addField(new BooleanFieldEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_VERBOSE,
                "Show reloaded modules list", p));
        addField(new LabelFieldEditor("LabelFieldEditor", "", p));

        addField(new ListEditor(PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_EXCLUDE_MODULE_LIST,
                "UMD Excluded Modules:", p) {

            @Override
            protected String createList(String[] items) {
                return StringUtils.join(",", items);
            }

            @Override
            protected String[] parseString(String stringList) {
                return stringList.split(",");
            }

            @Override
            protected String getNewInputObject() {
                InputDialog d = new InputDialog(getShell(), "New Excluded Module",
                        "Add the module you want to exclude.", "",
                        new IInputValidator() {
                            public String isValid(String newText) {
                                if (newText.indexOf(',') != -1) {
                                    return "The input cannot have a comma";
                                }
                                return null;
                            }
                        });

                if (d.open() == InputDialog.OK) {
                    return d.getValue();
                }
                return null;
            }

            @Override
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                super.doFillIntoGrid(parent, numColumns);
                List listControl = getListControl(parent);
                GridData layoutData = (GridData) listControl.getLayoutData();
                layoutData.heightHint = 300;
            }
        });

    }

    public static boolean isUMDEnabled() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_ENABLED);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_UMD_ENABLED;
        }
    }

    public static boolean isUMDVerbose() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getBoolean(
                    PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_VERBOSE);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_UMD_VERBOSE;
        }
    }

    public static String getUMDExcludeModules() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        if (plugin != null) {
            return plugin.getPreferenceStore().getString(
                    PydevConsoleConstants.INTERACTIVE_CONSOLE_UMD_EXCLUDE_MODULE_LIST);
        } else {
            return PydevConsoleConstants.DEFAULT_INTERACTIVE_CONSOLE_UMD_EXCLUDE_MODULE_LIST;
        }
    }

}
