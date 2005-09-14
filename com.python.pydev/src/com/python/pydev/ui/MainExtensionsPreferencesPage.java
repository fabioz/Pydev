/*
 * License: Common Public License v1.0
 * Created on Sep 14, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.ui;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class MainExtensionsPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public MainExtensionsPreferencesPage() {
        super(GRID);
    }

    @Override
    protected void createFieldEditors() {
    }

    public void init(IWorkbench workbench) {
    }

}
