/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;

public class IronpythonInterpreterPreferencesPage extends AbstractInterpreterPreferencesPage {

    @Override
    public String getTitle() {
        return "IronPython Interpreters";
    }

    /**
     * @return the title that should be used above the interpreters editor.
     */
    protected String getInterpretersTitle() {
        return "IronPython interpreters (e.g.: ipy.exe).   Double-click to rename.";
    }

    /**
     * @param p this is the composite that should be the interpreter parent
     * @return an interpreter editor (used to add/edit/remove the information on an editor)
     */
    @Override
    protected AbstractInterpreterEditor getInterpreterEditor(Composite p) {
        return new IronpythonInterpreterEditor(getInterpretersTitle(), p,
                PydevPlugin.getIronpythonInterpreterManager(true));
    }

    @Override
    protected void createFieldEditors() {
        super.createFieldEditors();
        addField(new StringFieldEditor(IInterpreterManager.IRONPYTHON_INTERNAL_SHELL_VM_ARGS,
                "Vm arguments for internal shell", getFieldEditorParent()) {
            @Override
            protected void adjustForNumColumns(int numColumns) {
                GridData gd = (GridData) getTextControl().getLayoutData();
                gd.horizontalSpan = numColumns; //We want it in a separate line!
                gd.grabExcessHorizontalSpace = true;

            }
        });
    }

    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getIronpythonInterpreterManager(true);
    }

}
