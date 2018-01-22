/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.filetypes;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.FileTypesPreferences;
import org.python.pydev.shared_core.string.WrapAndCaseUtils;
import org.python.pydev.utils.LabelFieldEditorWith2Cols;

/**
 * Preferences regarding the python file types available.
 *
 * Also provides a better access to them and caches to make that access efficient.
 *
 * @author Fabio
 */
public class FileTypesPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public FileTypesPreferencesPage() {
        super(FLAT);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("File Types Preferences");
    }

    @Override
    protected void createFieldEditors() {
        final Composite p = getFieldEditorParent();

        addField(new LabelFieldEditorWith2Cols("Label_Info_File_Preferences1", WrapAndCaseUtils.wrap(
                "These setting are used to know which files should be considered valid internally, and are "
                        + "not used in the file association of those files to the pydev editor.\n\n",
                80), p) {
            @Override
            public String getLabelTextCol1() {
                return "Note:\n\n";
            }
        });

        addField(new LabelFieldEditorWith2Cols(
                "Label_Info_File_Preferences2",
                WrapAndCaseUtils
                        .wrap("After changing those settings, a manual reconfiguration of the interpreter and a manual rebuild "
                                + "of the projects may be needed to update the inner caches that may be affected by those changes.\n\n",
                                80),
                p) {
            @Override
            public String getLabelTextCol1() {
                return "Important:\n\n";
            }
        });

        addField(new StringFieldEditor(FileTypesPreferences.VALID_SOURCE_FILES, "Valid source files (comma-separated):",
                StringFieldEditor.UNLIMITED, p));
        addField(
                new StringFieldEditor(FileTypesPreferences.FIRST_CHOICE_PYTHON_SOURCE_FILE, "Default python extension:",
                        StringFieldEditor.UNLIMITED, p));
    }

    @Override
    public void init(IWorkbench workbench) {
        // pass
    }

}
