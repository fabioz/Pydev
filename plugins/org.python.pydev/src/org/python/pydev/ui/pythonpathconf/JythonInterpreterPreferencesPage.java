/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;

public class JythonInterpreterPreferencesPage extends AbstractInterpreterPreferencesPage {

    @Override
    public String getTitle() {
        return "Jython Interpreters";
    }

    /**
     * @return the title that should be used above the interpreters editor.
     */
    protected String getInterpretersTitle() {
        return "Jython interpreters (e.g.: jython.jar).   Double-click to rename.";
    }

    /**
     * @param p this is the composite that should be the interpreter parent
     * @return an interpreter editor (used to add/edit/remove the information on an editor)
     */
    @Override
    protected AbstractInterpreterEditor getInterpreterEditor(Composite p) {
        return new JythonInterpreterEditor(getInterpretersTitle(), p, PydevPlugin.getJythonInterpreterManager(true));
    }

    @Override
    protected void createFieldEditors() {
        super.createFieldEditors();
        addField(new DirectoryFieldEditor(IInterpreterManager.JYTHON_CACHE_DIR, "-Dpython.cachedir",
                getFieldEditorParent()));
    }

    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getJythonInterpreterManager(true);
    }

}
