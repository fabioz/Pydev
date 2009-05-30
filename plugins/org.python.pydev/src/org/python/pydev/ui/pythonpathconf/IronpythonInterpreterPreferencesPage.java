/*
 * License: Eclipse Public License v1.0
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;

public class IronpythonInterpreterPreferencesPage extends AbstractInterpreterPreferencesPage{

    public String getTitle() {
        return "Iron Python Interpreters";
    }

    /**
     * @return the title that should be used above the interpreters editor.
     */
    protected String getInterpretersTitle() {
        return "Iron Python interpreters (e.g.: ipy.exe)";
    }

    /**
     * @param p this is the composite that should be the interpreter parent
     * @return an interpreter editor (used to add/edit/remove the information on an editor)
     */
    protected AbstractInterpreterEditor getInterpreterEditor(Composite p) {
        return new IronpythonInterpreterEditor (getInterpretersTitle(), p, PydevPlugin.getIronpythonInterpreterManager(true));
    }
    
    protected void createFieldEditors() {
        super.createFieldEditors();
        addField(new StringFieldEditor(IInterpreterManager.IRONPYTHON_DEFAULT_VM_ARGS, "Default vm arguments", getFieldEditorParent()));
    }
    
    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getIronpythonInterpreterManager(true);
    }

}
