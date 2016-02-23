/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 26, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder.todo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * @author Fabio Zadrozny
 */
public class PyTodoPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String DEFAULT_PY_TODO_TAGS = "TODO: FIXME:";
    public static final String PY_TODO_TAGS = "PY_TODO_TAGS";

    public PyTodoPrefPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Task tags");
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();
        addField(new StringFieldEditor(PY_TODO_TAGS, "Todo tags (separated by spaces)", p));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {

    }

    public static List<String> getTodoTags() {
        String string = PydevPrefs.getPreferences().getString(PY_TODO_TAGS);
        String[] strings = string.split(" ");
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].length() > 0) {
                list.add(strings[i]);
            }
        }
        return list;
    }
}
