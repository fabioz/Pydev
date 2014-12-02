/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.editorinput.PySourceLocatorPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.field_editors.ComboFieldEditor;

/**
 * Preferences for the locations that should be translated -- used when the debugger is not able
 * to find some path aa the client, so, the user is asked for the location and the answer is
 * kept in the preferences in the format:
 * 
 * path asked, new path -- means that a request for the "path asked" should return the "new path"
 * path asked, DONTASK -- means that if some request for that file was asked it should silently ignore it
 */
public class SourceLocatorPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Initializer sets the preference store
     */
    public SourceLocatorPrefsPage() {
        super("Source locator", GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
    }

    private static final String[][] ENTRIES_AND_VALUES = new String[][] {
            { "Ask for local file.", Integer.toString(PySourceLocatorPrefs.ASK_FOR_FILE) },
            { "Ask for local file/Get from server (read only).",
                    Integer.toString(PySourceLocatorPrefs.ASK_FOR_FILE_GET_FROM_SERVER) },
            { "Get from server (read only).", Integer.toString(PySourceLocatorPrefs.GET_FROM_SERVER) }, };

    /**
     * Creates the editors
     */
    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        addField(new ComboFieldEditor(PySourceLocatorPrefs.ON_SOURCE_NOT_FOUND,
                "Action when source is not directly found:", ENTRIES_AND_VALUES, p));

        addField(new IntegerFieldEditor(PySourceLocatorPrefs.FILE_CONTENTS_TIMEOUT,
                "Timeout to get file contents (millis):", p));

        addField(new TableEditor(PydevEditorPrefs.SOURCE_LOCATION_PATHS, "Translation paths to use:", p) {

            @Override
            protected String createTable(List<String[]> items) {
                return PySourceLocatorPrefs.wordsAsString(items);
            }

            @Override
            protected String[] getNewInputObject() {
                InputDialog d = new InputDialog(getShell(), "New entry",
                        "Add the entry in the format path_to_replace,new_path or path,DONTASK.", "",
                        new IInputValidator() {
                            public String isValid(String newText) {
                                String[] splitted = StringUtils.splitAndRemoveEmptyTrimmed(newText, ',').toArray(
                                        new String[0]);
                                if (splitted.length != 2) {
                                    return "Input must have 2 paths separated by a comma.";
                                }
                                return PySourceLocatorPrefs.isValid(splitted);
                            }
                        });

                int retCode = d.open();
                if (retCode == InputDialog.OK) {
                    return StringUtils.splitAndRemoveEmptyTrimmed(d.getValue(), ',').toArray(new String[0]);
                }
                return null;
            }

            @Override
            protected List<String[]> parseString(String stringList) {
                return PySourceLocatorPrefs.stringAsWords(stringList);
            }

            @Override
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                super.doFillIntoGrid(parent, numColumns);
                Table table = getTableControl(parent);
                GridData layoutData = (GridData) table.getLayoutData();
                layoutData.heightHint = 300;
            }
        });
    }

    /**
     * Sets default preference values
     */
    protected void initializeDefaultPreferences(Preferences prefs) {
    }

}
