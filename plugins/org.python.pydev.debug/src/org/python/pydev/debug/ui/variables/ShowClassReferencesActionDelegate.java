package org.python.pydev.debug.ui.variables;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.debug.model.PyVariableCollection;
import org.python.pydev.debug.model.PyVariableGroup;
import org.python.pydev.debug.model.PyVariablesPreferences;

public class ShowClassReferencesActionDelegate extends AbstractShowReferencesActionDelegate {
    @Override
    protected boolean isShowReference() {
        return PyVariablesPreferences.isShowClassReferences();
    }

    @Override
    protected void setShowReferences(boolean checked) {
        PyVariablesPreferences.setShowClassReferences(checked);
    }

    @Override
    protected boolean isShowReferenceProperty(String property) {
        return PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_CLASS_REFERENCES.equals(property);
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (isShowReference()) {
            return true;
        } else {
            if (element instanceof PyVariableGroup) {
                PyVariableGroup variable = (PyVariableGroup) element;
                try {
                    String name = variable.getName();
                    if (PyVariableCollection.SCOPE_CLASS_VARS.equals(name)) {
                        return false;
                    }
                } catch (DebugException e) {
                    // Ignore error, if we get one, don't filter
                }
            }

            return true;
        }
    }

}
