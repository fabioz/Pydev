package org.python.pydev.debug.ui.variables;

import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariablesPreferences;

public class ShowCapitalizedReferencesActionDelegate extends AbstractShowReferencesActionDelegate {
    @Override
    protected boolean isShowReference() {
        return PyVariablesPreferences.isShowCapitalizedReferences();
    }

    @Override
    protected void setShowReferences(boolean checked) {
        PyVariablesPreferences.setShowCapitalizedReferences(checked);
    }

    @Override
    protected boolean isShowReferenceProperty(String property) {
        return PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_CAPITALIZED_REFERENCES.equals(property);
    }

    @Override
    protected boolean select(Viewer viewer, Object parentElement, PyVariable variable, String variableName) {
        if (variableName != null && variableName.length() >= 1 && Character.isUpperCase(variableName.charAt(0))) {
            return false;
        }
        return true;
    }
}
