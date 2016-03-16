/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.jython.ui;

import java.io.File;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.jython.JythonPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;

public class JyScriptingPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String SHOW_SCRIPTING_OUTPUT = "SHOW_SCRIPTING_OUTPUT";
    public static final boolean DEFAULT_SHOW_SCRIPTING_OUTPUT = false;

    public static final String LOG_SCRIPTING_ERRORS = "LOG_SCRIPTING_ERRORS";
    public static final boolean DEFAULT_LOG_SCRIPTING_ERRORS = true;

    public static final String ADDITIONAL_SCRIPTING_LOCATION = "ADDITIONAL_SCRIPTING_LOCATION";

    public JyScriptingPreferencesPage() {
        super(GRID);
        //Set the preference store for the preference page.
        setPreferenceStore(JythonPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();
        addField(new BooleanFieldEditor(SHOW_SCRIPTING_OUTPUT,
                "Show the output given from the scripting to some console?", p));
        addField(new BooleanFieldEditor(LOG_SCRIPTING_ERRORS, "Show errors from scripting in the Error Log?", p));
        DirectoryFieldEditor fileField = new DirectoryFieldEditor(ADDITIONAL_SCRIPTING_LOCATION,
                "Location of additional jython scripts:", p);
        addField(fileField);
    }

    /**
     * @return if we should show the scripting output in a shell.
     */
    public static boolean getShowScriptingOutput() {
        if (SharedCorePlugin.inTestMode()) {
            return true; // always show output
        }
        JythonPlugin plugin = JythonPlugin.getDefault();
        return plugin.getPreferenceStore().getBoolean(SHOW_SCRIPTING_OUTPUT);
    }

    /**
     * @return if we should show the scripting output in a shell.
     */
    public static boolean getLogScriptingErrors() {
        return JythonPlugin.getDefault().getPreferenceStore().getBoolean(LOG_SCRIPTING_ERRORS);
    }

    /**
     * @return a directory that has additional scripts for pydev (besides the jysrc in org.python.pydev.jython). May
     * return null if none is specified or if one that is not a directory is specified.
     */
    public static File getAdditionalScriptingLocation() {
        String loc = JythonPlugin.getDefault().getPreferenceStore().getString(ADDITIONAL_SCRIPTING_LOCATION);
        if (loc != null && loc.trim().length() > 0) {
            File file = new File(loc);
            if (file.exists() && file.isDirectory()) {
                return file;
            }
        }
        return null;
    }
}
